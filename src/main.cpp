#include <windows.h>
#include <string>
#include <thread>
#include <iostream>

// Real JNI/JVMTI headers from JDK (included via /I flag in build script)
#include <jni.h>
#include <jvmti.h>

// Compatibility alias used throughout this file
typedef jvmtiEnv JVMTIEnv;

// ── JVM utility helpers ───────────────────────────────────────────────────────
static JavaVM* GetJavaVM() {
    HMODULE hJvm = GetModuleHandleA("jvm.dll");
    if (!hJvm) return nullptr;
    typedef jint(JNICALL* tGetVMs)(JavaVM**, jsize, jsize*);
    auto fn = (tGetVMs)GetProcAddress(hJvm, "JNI_GetCreatedJavaVMs");
    if (!fn) return nullptr;
    JavaVM* vm = nullptr; jsize n = 0;
    if (fn(&vm, 1, &n) != JNI_OK || n == 0) return nullptr;
    return vm;
}
static JNIEnv* GetJNIEnv() {
    JavaVM* vm = GetJavaVM();
    if (!vm) return nullptr;
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED)
        vm->AttachCurrentThread((void**)&env, nullptr);
    return env;
}
static JVMTIEnv* GetJVMTIEnv() {
    JavaVM* vm = GetJavaVM();
    if (!vm) return nullptr;
    JVMTIEnv* jvmti = nullptr;
    vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_2);
    return jvmti;
}

// ─────────────────────────────────────────────────────────────────────────────
// Loads tentavia.jar into the Forge FMLLaunchClassLoader and invokes
// cc.unknown.TentaviaAgent.init() to start the Haru-based client.
// ─────────────────────────────────────────────────────────────────────────────

static std::string GetJarPath() {
    char dll[MAX_PATH] = {};
    GetModuleFileNameA(GetModuleHandleA("tentavia.dll"), dll, MAX_PATH);
    // same dir as DLL, same name but .jar
    std::string p = dll;
    auto dot = p.rfind('.');
    if (dot != std::string::npos) p = p.substr(0, dot);
    return p + ".jar";
}

// Returns the classloader of net/minecraft/client/Minecraft via JVMTI iteration.
static jobject FindMinecraftClassLoader(JNIEnv* env, JVMTIEnv* jvmti) {
    jint classCount = 0;
    jclass* classes = nullptr;
    if (jvmti->functions->GetLoadedClasses(jvmti, &classCount, &classes) != 0) return nullptr;

    jobject result = nullptr;
    for (jint i = 0; i < classCount; i++) {
        char* sig = nullptr;
        if (jvmti->functions->GetClassSignature(jvmti, classes[i], &sig, nullptr) == 0 && sig) {
            if (strcmp(sig, "Lnet/minecraft/client/Minecraft;") == 0) {
                jvmti->functions->GetClassLoader(jvmti, classes[i], &result);
                jvmti->functions->Deallocate(jvmti, (unsigned char*)sig);
                break;
            }
            jvmti->functions->Deallocate(jvmti, (unsigned char*)sig);
        }
    }
    jvmti->functions->Deallocate(jvmti, (unsigned char*)classes);
    return result;
}

// Adds jarPath to classLoader (a URLClassLoader subclass) via reflection.
static bool AddJarToClassLoader(JNIEnv* env, jobject classLoader, const std::string& jarPath) {
    auto* f = env->functions;

    // Build File object
    jclass fileCls = f->FindClass(env, "java/io/File");
    if (!fileCls) { f->ExceptionClear(env); return false; }
    jmethodID fileCtor = f->GetMethodID(env, fileCls, "<init>", "(Ljava/lang/String;)V");
    jstring jPath = f->NewStringUTF(env, jarPath.c_str());
    jobject fileObj = f->NewObject(env, fileCls, fileCtor, jPath);
    f->DeleteLocalRef(env, jPath);

    // File → URI → URL
    jmethodID toURI = f->GetMethodID(env, fileCls, "toURI", "()Ljava/net/URI;");
    jobject uri = f->CallObjectMethod(env, fileObj, toURI);
    jclass uriCls = f->FindClass(env, "java/net/URI");
    jmethodID toURL = f->GetMethodID(env, uriCls, "toURL", "()Ljava/net/URL;");
    jobject url = f->CallObjectMethod(env, uri, toURL);
    f->DeleteLocalRef(env, fileObj); f->DeleteLocalRef(env, uri);
    f->DeleteLocalRef(env, fileCls); f->DeleteLocalRef(env, uriCls);
    if (!url || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    // Get addURL via getDeclaredMethod, searching up the class hierarchy
    jclass urlclCls = f->FindClass(env, "java/net/URLClassLoader");
    jclass classCls = f->FindClass(env, "java/lang/Class");
    jclass urlCls   = f->FindClass(env, "java/net/URL");

    // Build Class[] {URL.class} for getDeclaredMethod args
    jobject urlClassObj = f->CallStaticObjectMethod(env, urlCls,
        f->GetStaticMethodID(env, urlCls, "class", "Ljava/lang/Class;"));
    // Use Method approach: urlclCls.getDeclaredMethod("addURL", URL.class)
    jmethodID getDeclMethod = f->GetMethodID(env, classCls, "getDeclaredMethod",
        "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

    // Build parameter types array
    jclass urlClassForMethod = f->FindClass(env, "java/net/URL");
    jobjectArray paramTypes = f->NewObjectArray(env, 1,
        f->FindClass(env, "java/lang/Class"), urlClassForMethod);

    jstring methodName = f->NewStringUTF(env, "addURL");
    jobject addURLMethod = f->CallObjectMethod(env, urlclCls, getDeclMethod, methodName, paramTypes);
    f->DeleteLocalRef(env, methodName);
    f->DeleteLocalRef(env, paramTypes);

    if (!addURLMethod || f->ExceptionCheck(env)) {
        f->ExceptionClear(env);
        f->DeleteLocalRef(env, urlclCls); f->DeleteLocalRef(env, classCls);
        return false;
    }

    // setAccessible(true)
    jclass methodCls = f->FindClass(env, "java/lang/reflect/Method");
    jmethodID setAcc = f->GetMethodID(env, methodCls, "setAccessible", "(Z)V");
    f->CallVoidMethod(env, addURLMethod, setAcc, JNI_TRUE);

    // invoke(classLoader, url)
    jmethodID invoke = f->GetMethodID(env, methodCls, "invoke",
        "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    jclass objCls = f->FindClass(env, "java/lang/Object");
    jobjectArray args = f->NewObjectArray(env, 1, objCls, url);
    f->CallObjectMethod(env, addURLMethod, invoke, classLoader, args);

    bool ok = !f->ExceptionCheck(env);
    if (!ok) f->ExceptionClear(env);

    f->DeleteLocalRef(env, args);
    f->DeleteLocalRef(env, addURLMethod);
    f->DeleteLocalRef(env, url);
    f->DeleteLocalRef(env, urlclCls); f->DeleteLocalRef(env, classCls);
    f->DeleteLocalRef(env, urlCls); f->DeleteLocalRef(env, methodCls);
    f->DeleteLocalRef(env, objCls);
    return ok;
}

static DWORD WINAPI MainThread(LPVOID lpModule) {
    // Wait for the JVM to be ready
    JavaVM* jvm = nullptr;
    JNIEnv* env = nullptr;
    JVMTIEnv* jvmti = nullptr;

    for (int i = 0; i < 120 && !jvm; i++) {
        jvm = GetJavaVM();
        if (!jvm) Sleep(500);
    }
    if (!jvm) { FreeLibraryAndExitThread((HMODULE)lpModule, 1); return 1; }

    env   = GetJNIEnv();
    jvmti = GetJVMTIEnv();
    if (!env || !jvmti) { FreeLibraryAndExitThread((HMODULE)lpModule, 1); return 1; }

    // Wait for Minecraft class to be loaded
    jobject classLoader = nullptr;
    for (int i = 0; i < 300 && !classLoader; i++) {
        classLoader = FindMinecraftClassLoader(env, jvmti);
        if (!classLoader) Sleep(500);
    }
    if (!classLoader) { FreeLibraryAndExitThread((HMODULE)lpModule, 1); return 1; }

    // Add tentavia.jar to the Forge classloader
    std::string jarPath = GetJarPath();
    if (!AddJarToClassLoader(env, classLoader, jarPath)) {
        FreeLibraryAndExitThread((HMODULE)lpModule, 1);
        return 1;
    }

    // Load and invoke TentaviaAgent.init()
    auto* f = env->functions;
    jclass classCls = f->FindClass(env, "java/lang/Class");
    jmethodID forName = f->GetStaticMethodID(env, classCls, "forName",
        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");

    jstring agentClassName = f->NewStringUTF(env, "cc.unknown.TentaviaAgent");
    jobject agentClass = f->CallStaticObjectMethod(env, classCls, forName,
        agentClassName, JNI_TRUE, classLoader);
    f->DeleteLocalRef(env, agentClassName);

    if (!agentClass || f->ExceptionCheck(env)) {
        f->ExceptionClear(env);
        FreeLibraryAndExitThread((HMODULE)lpModule, 1);
        return 1;
    }

    jmethodID initMethod = f->GetStaticMethodID(env, (jclass)agentClass, "init", "()V");
    if (initMethod) {
        f->CallStaticVoidMethod(env, (jclass)agentClass, initMethod);
        if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    }

    f->DeleteLocalRef(env, agentClass);
    f->DeleteLocalRef(env, classLoader);

    // Keep the DLL loaded; the Java side manages its own lifecycle
    return 0;
}

BOOL WINAPI DllMain(HMODULE hModule, DWORD dwReason, LPVOID) {
    if (dwReason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(hModule);
        HANDLE t = CreateThread(nullptr, 0, MainThread, hModule, 0, nullptr);
        if (t) CloseHandle(t);
    }
    return TRUE;
}
