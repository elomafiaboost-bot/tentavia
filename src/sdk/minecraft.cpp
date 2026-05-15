#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>
#include <cstring>
#include <vector>

namespace SDK {

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

// ── Globals ──────────────────────────────────────────────────────────────────
static jclass  g_mcClass    = nullptr;
static jobject g_mcInstance = nullptr;
static jobject g_entityList = nullptr;
static bool    g_initDone   = false;
static bool    g_initFailed = false;
static int     g_retryCount = 0;

static jobject g_posXField  = nullptr;
static jobject g_posYField  = nullptr;
static jobject g_posZField  = nullptr;

// ── Method ID cache ──────────────────────────────────────────────────────────
static jmethodID s_gdfM      = nullptr; // Class.getDeclaredFields()
static jmethodID s_getNameM  = nullptr; // Class.getName()
static jmethodID s_getSuperM = nullptr; // Class.getSuperclass()
static jmethodID s_getModsM  = nullptr; // Field.getModifiers()
static jmethodID s_getTypeM  = nullptr; // Field.getType()
static jmethodID s_getM      = nullptr; // Field.get(Object)
static jmethodID s_setAccM   = nullptr; // Field.setAccessible(boolean)
static jmethodID s_toStrM    = nullptr; // Object.toString()
static jmethodID s_listSizeM = nullptr; // List.size()
static jmethodID s_listGetM  = nullptr; // List.get(int)

static bool CacheReflectMethods(JNIEnv* env) {
    if (s_gdfM) return true;
    auto* f = env->functions;
    jclass classCls = f->FindClass(env, "java/lang/Class");
    jclass fieldCls = f->FindClass(env, "java/lang/reflect/Field");
    jclass objCls   = f->FindClass(env, "java/lang/Object");
    jclass listCls  = f->FindClass(env, "java/util/List");
    if (!classCls || !fieldCls || !objCls || !listCls) { f->ExceptionClear(env); return false; }

    s_gdfM      = f->GetMethodID(env, classCls, "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    s_getNameM  = f->GetMethodID(env, classCls, "getName",           "()Ljava/lang/String;");
    s_getSuperM = f->GetMethodID(env, classCls, "getSuperclass",     "()Ljava/lang/Class;");
    s_getModsM  = f->GetMethodID(env, fieldCls, "getModifiers",      "()I");
    s_getTypeM  = f->GetMethodID(env, fieldCls, "getType",           "()Ljava/lang/Class;");
    s_getM      = f->GetMethodID(env, fieldCls, "get",               "(Ljava/lang/Object;)Ljava/lang/Object;");
    s_setAccM   = f->GetMethodID(env, fieldCls, "setAccessible",     "(Z)V");
    s_toStrM    = f->GetMethodID(env, objCls,   "toString",          "()Ljava/lang/String;");
    s_listSizeM = f->GetMethodID(env, listCls,  "size",              "()I");
    s_listGetM  = f->GetMethodID(env, listCls,  "get",               "(I)Ljava/lang/Object;");

    f->DeleteLocalRef(env, classCls);
    f->DeleteLocalRef(env, fieldCls);
    f->DeleteLocalRef(env, objCls);
    f->DeleteLocalRef(env, listCls);

    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); s_gdfM = nullptr; return false; }
    return s_gdfM && s_getNameM && s_getSuperM && s_getModsM && s_getTypeM &&
           s_getM && s_setAccM  && s_listSizeM && s_listGetM;
}

// ── Reflection helpers ────────────────────────────────────────────────────────

static std::string GetClassName(JNIEnv* env, jobject classObj) {
    auto* f = env->functions;
    jstring s = (jstring)f->CallObjectMethod(env, classObj, s_getNameM);
    if (!s || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    const char* cs = f->GetStringUTFChars(env, s, nullptr);
    std::string r  = cs ? cs : "";
    if (cs) f->ReleaseStringUTFChars(env, s, cs);
    f->DeleteLocalRef(env, s);
    return r;
}

static jobject GetDeclaredFields(JNIEnv* env, jobject classObj) {
    auto* f = env->functions;
    jobject arr = f->CallObjectMethod(env, classObj, s_gdfM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return arr;
}

// Returns local ref to superclass, or nullptr if none
static jclass GetSuperclass(JNIEnv* env, jclass cls) {
    auto* f = env->functions;
    jclass sup = (jclass)f->CallObjectMethod(env, cls, s_getSuperM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return sup;
}

static jint FieldGetMods(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jint r = f->CallIntMethod(env, field, s_getModsM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
    return r;
}

static std::string FieldTypeName(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jobject tc = f->CallObjectMethod(env, field, s_getTypeM);
    if (!tc || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    std::string name = GetClassName(env, tc);
    f->DeleteLocalRef(env, tc);
    return name;
}

// setAccessible(true) then field.get(instance)
static jobject FieldGet(JNIEnv* env, jobject field, jobject instance) {
    auto* f = env->functions;
    f->CallVoidMethod(env, field, s_setAccM, (jboolean)1);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    jobject val = f->CallObjectMethod(env, field, s_getM, instance);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return val;
}

static double BoxedToDouble(JNIEnv* env, jobject boxed) {
    auto* f = env->functions;
    if (!boxed) return 0.0;
    jstring s = (jstring)f->CallObjectMethod(env, boxed, s_toStrM);
    if (!s || f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0.0; }
    const char* cs = f->GetStringUTFChars(env, s, nullptr);
    double r = cs ? atof(cs) : 0.0;
    if (cs) f->ReleaseStringUTFChars(env, s, cs);
    f->DeleteLocalRef(env, s);
    return r;
}

static double FieldGetDouble(JNIEnv* env, jobject field, jobject instance) {
    jobject boxed = FieldGet(env, field, instance);
    if (!boxed) return 0.0;
    double r = BoxedToDouble(env, boxed);
    env->functions->DeleteLocalRef(env, boxed);
    return r;
}

static bool IsCoordRange(double v) { return v > -60000.0 && v < 60000.0 && v != 0.0; }

static bool IsObjType(const std::string& t) {
    if (t.empty() || t[0] == '[') return false;
    if (t == "int"    || t == "float"   || t == "double" || t == "boolean" ||
        t == "long"   || t == "byte"    || t == "short"  || t == "char") return false;
    if (t == "java.lang.String") return false;
    return true;
}

// ── Count coord doubles in an entity, walking superclass chain ────────────────
static int CountCoordDoubles(JNIEnv* env, jobject entity) {
    auto* f = env->functions;
    int count = 0;
    jclass cls = f->GetObjectClass(env, entity);
    for (int depth = 0; cls && count < 3 && depth < 12; depth++) {
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n && count < 3; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                if (FieldTypeName(env, field) == "double") {
                    double dv = FieldGetDouble(env, field, entity);
                    if (IsCoordRange(dv)) count++;
                }
                f->DeleteLocalRef(env, field);
            }
            f->DeleteLocalRef(env, flds);
        }
        jclass sup = GetSuperclass(env, cls);
        f->DeleteLocalRef(env, cls);
        cls = sup;
    }
    if (cls) f->DeleteLocalRef(env, cls);
    return count;
}

// ── Collect non-static Object field values from cls hierarchy ─────────────────
// Returns vector of (typeName, localRef) — caller must DeleteLocalRef each value
static std::vector<std::pair<std::string, jobject>>
CollectObjFields(JNIEnv* env, jclass startCls, jobject instance) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;
    std::vector<std::pair<std::string, jobject>> result;

    jclass cls = (jclass)f->NewGlobalRef(env, startCls);
    for (int depth = 0; cls && depth < 12; depth++) {
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                jint mods = FieldGetMods(env, field);
                if (!(mods & ACC_STATIC)) {
                    std::string typeName = FieldTypeName(env, field);
                    if (IsObjType(typeName)) {
                        jobject val = FieldGet(env, field, instance);
                        if (val && !f->ExceptionCheck(env))
                            result.push_back({typeName, val});
                        else {
                            f->ExceptionClear(env);
                            if (val) f->DeleteLocalRef(env, val);
                        }
                    }
                }
                f->DeleteLocalRef(env, field);
            }
            f->DeleteLocalRef(env, flds);
        }
        jclass sup = GetSuperclass(env, cls);
        f->DeleteLocalRef(env, cls);
        cls = sup;
    }
    if (cls) f->DeleteLocalRef(env, cls);
    return result;
}

static void FreeObjFields(JNIEnv* env,
                           std::vector<std::pair<std::string, jobject>>& v) {
    for (auto& p : v) env->functions->DeleteLocalRef(env, p.second);
    v.clear();
}

// Check if obj is a non-empty List whose first element has coord doubles
static bool IsEntityList(JNIEnv* env, jobject obj) {
    auto* f = env->functions;
    jint sz = f->CallIntMethod(env, obj, s_listSizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
    if (sz <= 0) return false;
    jobject elem = f->CallObjectMethod(env, obj, s_listGetM, (jint)0);
    if (!elem || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
    int coords = CountCoordDoubles(env, elem);
    f->DeleteLocalRef(env, elem);
    return coords >= 2;
}

// ── 3-level deep entity list search with superclass walking ───────────────────
static jobject FindPlayerEntityList(JNIEnv* env, jobject mcInstance, jclass mcCls) {
    auto* f = env->functions;

    auto level0 = CollectObjFields(env, mcCls, mcInstance);

    for (auto& p0 : level0) {
        const std::string& t0 = p0.first;
        jobject v0 = p0.second;

        // Check v0 directly as a List
        if (t0.find("List") != std::string::npos && IsEntityList(env, v0)) {
            jint sz = f->CallIntMethod(env, v0, s_listSizeM);
            if (f->ExceptionCheck(env)) f->ExceptionClear(env);
            jobject ref = f->NewGlobalRef(env, v0);
            FreeObjFields(env, level0);
            std::cout << "[+] SDK: lista nivel 0 (" << sz << " entidades)" << std::endl;
            return ref;
        }

        jclass cls0 = f->GetObjectClass(env, v0);
        auto level1 = CollectObjFields(env, cls0, v0);
        f->DeleteLocalRef(env, cls0);

        for (auto& p1 : level1) {
            const std::string& t1 = p1.first;
            jobject v1 = p1.second;

            if (t1.find("List") != std::string::npos && IsEntityList(env, v1)) {
                jint sz = f->CallIntMethod(env, v1, s_listSizeM);
                if (f->ExceptionCheck(env)) f->ExceptionClear(env);
                jobject ref = f->NewGlobalRef(env, v1);
                FreeObjFields(env, level1);
                FreeObjFields(env, level0);
                std::cout << "[+] SDK: lista nivel 1 (" << sz << " entidades)" << std::endl;
                return ref;
            }

            jclass cls1 = f->GetObjectClass(env, v1);
            auto level2 = CollectObjFields(env, cls1, v1);
            f->DeleteLocalRef(env, cls1);

            for (auto& p2 : level2) {
                const std::string& t2 = p2.first;
                jobject v2 = p2.second;

                if (t2.find("List") != std::string::npos && IsEntityList(env, v2)) {
                    jint sz = f->CallIntMethod(env, v2, s_listSizeM);
                    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
                    jobject ref = f->NewGlobalRef(env, v2);
                    FreeObjFields(env, level2);
                    FreeObjFields(env, level1);
                    FreeObjFields(env, level0);
                    std::cout << "[+] SDK: lista nivel 2 (" << sz << " entidades)" << std::endl;
                    return ref;
                }
            }
            FreeObjFields(env, level2);
        }
        FreeObjFields(env, level1);
    }
    FreeObjFields(env, level0);

    std::cout << "[-] SDK: lista de jogadores nao encontrada (3 niveis)." << std::endl;
    return nullptr;
}

// ── Singleton discovery ───────────────────────────────────────────────────────
static jclass FindSingletonClass(JNIEnv* env, JVMTIEnv* jvmti) {
    auto* t = jvmti->functions;
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;

    jint    count   = 0;
    jclass* classes = nullptr;
    if (t->GetLoadedClasses(jvmti, &count, &classes) != JVMTI_ERROR_NONE || !classes)
        return nullptr;

    jclass result = nullptr;
    for (jint k = 0; k < count && !result; k++) {
        char* sig = nullptr;
        bool  ok  = false;
        if (t->GetClassSignature(jvmti, classes[k], &sig, nullptr) == JVMTI_ERROR_NONE && sig) {
            int len = (int)strlen(sig);
            ok = (len <= 7 && !strchr(sig, '/'));
            t->Deallocate(jvmti, (unsigned char*)sig);
        }
        if (!ok) { f->DeleteLocalRef(env, classes[k]); continue; }

        std::string className = GetClassName(env, classes[k]);
        if (className.empty()) { f->DeleteLocalRef(env, classes[k]); continue; }

        jobject fieldsArr = GetDeclaredFields(env, classes[k]);
        if (!fieldsArr) { f->DeleteLocalRef(env, classes[k]); continue; }

        jsize n = f->GetArrayLength(env, fieldsArr);
        for (jsize i = 0; i < n; i++) {
            jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
            if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            jint mods = FieldGetMods(env, field);
            if ((mods & ACC_STATIC) && FieldTypeName(env, field) == className) {
                std::cout << "[+] SDK: singleton candidato: " << className
                          << " (" << n << " campos)" << std::endl;
                result = classes[k];
                f->DeleteLocalRef(env, field);
                break;
            }
            f->DeleteLocalRef(env, field);
        }
        f->DeleteLocalRef(env, fieldsArr);
        if (!result) f->DeleteLocalRef(env, classes[k]);
    }

    for (jint k = 0; k < count; k++)
        if (classes[k] != result) f->DeleteLocalRef(env, classes[k]);
    t->Deallocate(jvmti, (unsigned char*)classes);
    return result;
}

static jobject GetSingletonInstance(JNIEnv* env, jclass cls, const std::string& className) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;

    jobject fieldsArr = GetDeclaredFields(env, cls);
    if (!fieldsArr) return nullptr;

    jsize   n      = f->GetArrayLength(env, fieldsArr);
    jobject result = nullptr;

    for (jsize i = 0; i < n && !result; i++) {
        jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
        jint mods = FieldGetMods(env, field);
        if ((mods & ACC_STATIC) && FieldTypeName(env, field) == className)
            result = FieldGet(env, field, nullptr);
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, fieldsArr);
    return result;
}

// ── Init ──────────────────────────────────────────────────────────────────────
static bool InitSDK(JNIEnv* env) {
    if (g_initDone) return !g_initFailed;
    g_initDone = true;

    if (!CacheReflectMethods(env)) {
        std::cout << "[-] SDK: falha ao cachear metodos de reflexao." << std::endl;
        g_initFailed = true; return false;
    }

    JVMTIEnv* jvmti = JNIUtils::GetJVMTIEnv();
    if (!jvmti) {
        std::cout << "[-] SDK: JVMTI nao disponivel." << std::endl;
        g_initFailed = true; return false;
    }

    jclass localMcClass = FindSingletonClass(env, jvmti);
    if (!localMcClass) {
        std::cout << "[-] SDK: classe singleton nao encontrada." << std::endl;
        g_initFailed = true; return false;
    }
    g_mcClass = (jclass)env->functions->NewGlobalRef(env, localMcClass);
    env->functions->DeleteLocalRef(env, localMcClass);
    std::string mcName = GetClassName(env, g_mcClass);
    std::cout << "[+] SDK: classe Minecraft: " << mcName << std::endl;

    jobject inst = GetSingletonInstance(env, g_mcClass, mcName);
    if (!inst) {
        std::cout << "[-] SDK: instancia singleton nula." << std::endl;
        g_initFailed = true; return false;
    }
    g_mcInstance = env->functions->NewGlobalRef(env, inst);
    env->functions->DeleteLocalRef(env, inst);
    std::cout << "[+] SDK: instancia Minecraft obtida." << std::endl;

    g_entityList = FindPlayerEntityList(env, g_mcInstance, g_mcClass);
    if (!g_entityList) {
        g_initFailed = true; return false;
    }

    std::cout << "[+] SDK: inicializacao completa!" << std::endl;
    return true;
}

// ── Cache entity position fields, walking superclass chain ────────────────────
static bool CacheEntityFields(JNIEnv* env, jobject entity) {
    if (g_posXField) return true;
    auto* f = env->functions;

    jobject cands[3] = {};
    int found = 0;

    jclass cls = f->GetObjectClass(env, entity);
    for (int depth = 0; cls && found < 3 && depth < 12; depth++) {
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n && found < 3; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                if (FieldTypeName(env, field) == "double") {
                    double dv = FieldGetDouble(env, field, entity);
                    if (IsCoordRange(dv)) { cands[found++] = field; continue; }
                }
                f->DeleteLocalRef(env, field);
            }
            f->DeleteLocalRef(env, flds);
        }
        jclass sup = GetSuperclass(env, cls);
        f->DeleteLocalRef(env, cls);
        cls = sup;
    }
    if (cls) f->DeleteLocalRef(env, cls);

    if (found < 3) {
        for (int i = 0; i < found; i++) f->DeleteLocalRef(env, cands[i]);
        return false;
    }
    g_posXField = f->NewGlobalRef(env, cands[0]);
    g_posYField = f->NewGlobalRef(env, cands[1]);
    g_posZField = f->NewGlobalRef(env, cands[2]);
    for (int i = 0; i < 3; i++) f->DeleteLocalRef(env, cands[i]);
    std::cout << "[+] SDK: campos posX/Y/Z cacheados." << std::endl;
    return true;
}

// ── Public API ────────────────────────────────────────────────────────────────

bool Minecraft::GetNearbyPlayers(std::vector<EntityInfo>& out) {
    JNIEnv* env = Env();
    if (!env) return false;
    if (g_initFailed) { g_initDone = false; g_initFailed = false; }
    if (!InitSDK(env)) {
        g_retryCount++;
        if (g_retryCount <= 3 || (g_retryCount % 120) == 0)
            std::cout << "[-] SDK: init falhou (tentativa " << g_retryCount << ")" << std::endl;
        return false;
    }

    auto* f = env->functions;
    jint count = f->CallIntMethod(env, g_entityList, s_listSizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    for (jint i = 0; i < count; i++) {
        jobject entity = f->CallObjectMethod(env, g_entityList, s_listGetM, i);
        if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        if (!CacheEntityFields(env, entity)) { f->DeleteLocalRef(env, entity); continue; }

        EntityInfo info;
        info.posX = FieldGetDouble(env, g_posXField, entity);
        info.posY = FieldGetDouble(env, g_posYField, entity);
        info.posZ = FieldGetDouble(env, g_posZField, entity);
        out.push_back(info);
        f->DeleteLocalRef(env, entity);
    }
    return !out.empty();
}

bool Minecraft::GetCameraInfo(CameraInfo& out) {
    JNIEnv* env = Env();
    if (!env) return false;
    if (g_initFailed) { g_initDone = false; g_initFailed = false; }
    if (!InitSDK(env)) return false;
    if (!g_entityList) return false;

    std::vector<EntityInfo> tmp;
    if (!GetNearbyPlayers(tmp) || tmp.empty()) return false;

    out.eyeX  = tmp[0].posX;
    out.eyeY  = tmp[0].posY + 1.62;
    out.eyeZ  = tmp[0].posZ;
    out.yaw   = 0.0f;
    out.pitch = 0.0f;
    out.fov   = 70.0f;
    out.valid = true;
    return true;
}

void Minecraft::PrintLocalPlayerName() {}

} // namespace SDK
