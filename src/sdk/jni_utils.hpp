#pragma once
#include "jni_stub.hpp"
#include "jvmti_stub.hpp"
#include <cstring>
#include <iostream>

namespace JNIUtils {
    typedef jint (JNICALL *tJNI_GetCreatedJavaVMs)(JavaVM**, jsize, jsize*);

    inline JavaVM* GetJavaVM() {
        HMODULE hJvm = GetModuleHandleA("jvm.dll");
        if (!hJvm) return nullptr;
        auto fn = (tJNI_GetCreatedJavaVMs)GetProcAddress(hJvm, "JNI_GetCreatedJavaVMs");
        if (!fn) return nullptr;
        JavaVM* vm = nullptr;
        jsize n = 0;
        if (fn(&vm, 1, &n) != JNI_OK || n == 0) return nullptr;
        return vm;
    }

    inline JNIEnv* GetJNIEnv() {
        JavaVM* vm = GetJavaVM();
        if (!vm) return nullptr;
        JNIEnv* env = nullptr;
        if (vm->functions->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED)
            vm->functions->AttachCurrentThread(vm, (void**)&env, nullptr);
        return env;
    }

    inline JVMTIEnv* GetJVMTIEnv() {
        JavaVM* vm = GetJavaVM();
        if (!vm) return nullptr;
        JVMTIEnv* jvmti = nullptr;
        vm->functions->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION_1_2);
        return jvmti;
    }

    // ── JVMTI path ────────────────────────────────────────────────────────────
    // Enumera TODAS as classes carregadas pelo JVM, independente de classloader.
    // É a forma mais confiável — funciona mesmo quando o contexto classloader
    // da thread do hook não tem acesso às classes do Minecraft/Forge.
    // slashName: "net/minecraft/client/Minecraft"
    // Retorna local JNI ref ou nullptr.
    inline jclass FindClassViaJVMTI(JNIEnv* jni, JVMTIEnv* jvmti, const char* slashName) {
        auto* t = jvmti->functions;
        auto* f = jni->functions;

        // Monta a assinatura esperada: "Lnet/minecraft/client/Minecraft;"
        char sig[260] = { 'L' };
        int i = 1;
        for (int j = 0; slashName[j] && i < 257; j++, i++)
            sig[i] = slashName[j];
        sig[i++] = ';';
        sig[i]   = '\0';

        jint    count   = 0;
        jclass* classes = nullptr;
        if (t->GetLoadedClasses(jvmti, &count, &classes) != JVMTI_ERROR_NONE || !classes)
            return nullptr;

        jclass result = nullptr;
        for (jint k = 0; k < count; k++) {
            if (!result) {
                char* classSig   = nullptr;
                char* genericSig = nullptr;
                if (t->GetClassSignature(jvmti, classes[k], &classSig, &genericSig) == JVMTI_ERROR_NONE) {
                    if (classSig && strcmp(classSig, sig) == 0)
                        result = classes[k];   // mantém esta local ref
                    if (classSig)   t->Deallocate(jvmti, (unsigned char*)classSig);
                    if (genericSig) t->Deallocate(jvmti, (unsigned char*)genericSig);
                }
            }
            if (!result || classes[k] != result)
                f->DeleteLocalRef(jni, classes[k]);
        }
        t->Deallocate(jvmti, (unsigned char*)classes);
        return result;
    }

    // ── Classloader fallback ──────────────────────────────────────────────────
    // Itera todas as threads do JVM e tenta loadClass em cada contextClassLoader.
    static inline jclass TryLoadClass(JNIEnv* env, jobject loader, jstring nameStr) {
        auto* f = env->functions;
        jclass loaderCls = f->GetObjectClass(env, loader);
        jmethodID loadM  = f->GetMethodID(env, loaderCls, "loadClass",
                                           "(Ljava/lang/String;)Ljava/lang/Class;");
        f->DeleteLocalRef(env, loaderCls);
        if (!loadM) { f->ExceptionClear(env); return nullptr; }
        auto cls = (jclass)f->CallObjectMethod(env, loader, loadM, nameStr);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
        return cls;
    }

    static inline jclass FindClassViaThreadLoaders(JNIEnv* env, const char* slashName) {
        auto* f = env->functions;

        char dotted[256];
        int i = 0;
        for (; slashName[i] && i < 255; i++)
            dotted[i] = (slashName[i] == '/') ? '.' : slashName[i];
        dotted[i] = '\0';

        jstring nameStr = f->NewStringUTF(env, dotted);
        if (!nameStr) return nullptr;

        jclass threadCls = f->FindClass(env, "java/lang/Thread");
        if (!threadCls) { f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr; }

        jmethodID getAllM = f->GetStaticMethodID(env, threadCls,
            "getAllStackTraces", "()Ljava/util/Map;");
        if (!getAllM) { f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr; }

        jobject traceMap = f->CallStaticObjectMethod(env, threadCls, getAllM);
        if (!traceMap || f->ExceptionCheck(env)) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr;
        }

        jclass mapCls     = f->GetObjectClass(env, traceMap);
        jmethodID keySetM = f->GetMethodID(env, mapCls, "keySet", "()Ljava/util/Set;");
        f->DeleteLocalRef(env, mapCls);
        jobject keySet    = f->CallObjectMethod(env, traceMap, keySetM);
        f->DeleteLocalRef(env, traceMap);
        if (!keySet || f->ExceptionCheck(env)) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr;
        }

        jclass setCls       = f->GetObjectClass(env, keySet);
        jmethodID iteratorM = f->GetMethodID(env, setCls, "iterator", "()Ljava/util/Iterator;");
        f->DeleteLocalRef(env, setCls);
        jobject iterator    = f->CallObjectMethod(env, keySet, iteratorM);
        f->DeleteLocalRef(env, keySet);
        if (!iterator || f->ExceptionCheck(env)) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr;
        }

        jclass iterCls      = f->GetObjectClass(env, iterator);
        jmethodID hasNextM  = f->GetMethodID(env, iterCls, "hasNext", "()Z");
        jmethodID nextM     = f->GetMethodID(env, iterCls, "next",    "()Ljava/lang/Object;");
        f->DeleteLocalRef(env, iterCls);
        jmethodID getLoaderM = f->GetMethodID(env, threadCls,
            "getContextClassLoader", "()Ljava/lang/ClassLoader;");

        jclass result = nullptr;
        while (f->CallBooleanMethod(env, iterator, hasNextM)) {
            if (f->ExceptionCheck(env)) { f->ExceptionClear(env); break; }
            jobject thread = f->CallObjectMethod(env, iterator, nextM);
            if (!thread || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            jobject loader = f->CallObjectMethod(env, thread, getLoaderM);
            f->DeleteLocalRef(env, thread);
            if (!loader || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            result = TryLoadClass(env, loader, nameStr);
            f->DeleteLocalRef(env, loader);
            if (result) break;
        }
        f->DeleteLocalRef(env, iterator);
        f->DeleteLocalRef(env, nameStr);
        return result;
    }

    // ── Ponto de entrada público ──────────────────────────────────────────────
    // 1º tenta JVMTI (mais confiável); se não disponível, usa thread classloaders.
    // Retorna local ref — chame NewGlobalRef se precisar guardar entre frames.
    inline jclass FindMinecraftClass(JNIEnv* env, const char* slashName) {
        // JVMTI: encontra qualquer classe já carregada sem precisar de classloader
        JVMTIEnv* jvmti = GetJVMTIEnv();
        if (jvmti) {
            jclass cls = FindClassViaJVMTI(env, jvmti, slashName);
            if (cls) return cls;
            std::cout << "[-] JVMTI GetLoadedClasses falhou para " << slashName
                      << ", tentando classloaders..." << std::endl;
        } else {
            static bool warnedJVMTI = false;
            if (!warnedJVMTI) {
                std::cout << "[-] JVMTI nao disponivel (GetEnv retornou null), "
                             "usando fallback de classloaders." << std::endl;
                warnedJVMTI = true;
            }
        }

        // Fallback: itera classloaders de todas as threads
        return FindClassViaThreadLoaders(env, slashName);
    }
}
