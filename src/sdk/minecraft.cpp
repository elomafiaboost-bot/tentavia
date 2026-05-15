#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>
#include <cstring>

// ── Cache global ──────────────────────────────────────────────────────────────
namespace SDK {

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

static jclass  g_mcClass     = nullptr;
static jobject g_mcInstance  = nullptr;  // global ref
static jobject g_entityList  = nullptr;  // global ref para a List de jogadores
static bool    g_initDone    = false;
static bool    g_initFailed  = false;
static int     g_retryCount  = 0;

// Campos double de posição (global refs para java.lang.reflect.Field)
static jobject g_posXField   = nullptr;
static jobject g_posYField   = nullptr;
static jobject g_posZField   = nullptr;

// ── Helpers de reflexão ───────────────────────────────────────────────────────

// Pré-carregados uma vez para não re-buscar a cada frame
static jmethodID s_gdfM         = nullptr; // Class.getDeclaredFields()
static jmethodID s_getNameM     = nullptr; // Class.getName()
static jmethodID s_getModsM     = nullptr; // Field.getModifiers()
static jmethodID s_getTypeM     = nullptr; // Field.getType()
static jmethodID s_getM         = nullptr; // Field.get(Object)
static jmethodID s_setAccM      = nullptr; // Field.setAccessible(boolean)
static jmethodID s_toStrM       = nullptr; // Object.toString()
static jmethodID s_listSizeM    = nullptr; // List.size()
static jmethodID s_listGetM     = nullptr; // List.get(int)

static bool CacheReflectMethods(JNIEnv* env) {
    if (s_gdfM) return true;
    auto* f = env->functions;

    jclass classCls = f->FindClass(env, "java/lang/Class");
    jclass fieldCls = f->FindClass(env, "java/lang/reflect/Field");
    jclass objCls   = f->FindClass(env, "java/lang/Object");
    jclass listCls  = f->FindClass(env, "java/util/List");
    if (!classCls || !fieldCls || !objCls || !listCls) {
        f->ExceptionClear(env); return false;
    }

    s_gdfM      = f->GetMethodID(env, classCls, "getDeclaredFields",
                                   "()[Ljava/lang/reflect/Field;");
    s_getNameM  = f->GetMethodID(env, classCls, "getName", "()Ljava/lang/String;");
    s_getModsM  = f->GetMethodID(env, fieldCls, "getModifiers", "()I");
    s_getTypeM  = f->GetMethodID(env, fieldCls, "getType", "()Ljava/lang/Class;");
    s_getM      = f->GetMethodID(env, fieldCls, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    s_setAccM   = f->GetMethodID(env, fieldCls, "setAccessible", "(Z)V");
    s_toStrM    = f->GetMethodID(env, objCls,   "toString", "()Ljava/lang/String;");
    s_listSizeM = f->GetMethodID(env, listCls,  "size", "()I");
    s_listGetM  = f->GetMethodID(env, listCls,  "get", "(I)Ljava/lang/Object;");

    f->DeleteLocalRef(env, classCls);
    f->DeleteLocalRef(env, fieldCls);
    f->DeleteLocalRef(env, objCls);
    f->DeleteLocalRef(env, listCls);

    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); s_gdfM = nullptr; return false; }
    return s_gdfM && s_getNameM && s_getModsM && s_getTypeM &&
           s_getM && s_setAccM  && s_listSizeM && s_listGetM;
}

// Retorna getDeclaredFields() de um objeto Class (a própria classe)
// classObj deve ser um jclass (java.lang.Class instance)
static jobject GetDeclaredFields(JNIEnv* env, jobject classObj) {
    auto* f = env->functions;
    jobject arr = f->CallObjectMethod(env, classObj, s_gdfM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return arr;
}

// Chama Class.getName() em um jclass ou objeto Class
static std::string GetClassName(JNIEnv* env, jobject classObj) {
    auto* f = env->functions;
    jstring s = (jstring)f->CallObjectMethod(env, classObj, s_getNameM);
    if (!s || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    const char* cs = f->GetStringUTFChars(env, s, nullptr);
    std::string r = cs ? cs : "";
    if (cs) f->ReleaseStringUTFChars(env, s, cs);
    f->DeleteLocalRef(env, s);
    return r;
}

// Field.get(instance) — faz setAccessible(true) antes
static jobject FieldGet(JNIEnv* env, jobject field, jobject instance) {
    auto* f = env->functions;
    // setAccessible(true) para acessar campos privados
    f->CallVoidMethod(env, field, s_setAccM, (jboolean)1);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    jobject val = f->CallObjectMethod(env, field, s_getM, instance);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return val;
}

// Field.getModifiers()
static jint FieldGetMods(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jint r = f->CallIntMethod(env, field, s_getModsM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
    return r;
}

// Field.getType().getName()
static std::string FieldTypeName(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jobject typeClass = f->CallObjectMethod(env, field, s_getTypeM);
    if (!typeClass || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    std::string name = GetClassName(env, typeClass);
    f->DeleteLocalRef(env, typeClass);
    return name;
}

// Converte um Double/boxed para double via toString + atof
// (necessário pois não temos CallDoubleMethod no stub)
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

// Field.get(entity) → double (via boxed Double)
static double FieldGetDouble(JNIEnv* env, jobject field, jobject instance) {
    jobject boxed = FieldGet(env, field, instance);
    if (!boxed) return 0.0;
    double r = BoxedToDouble(env, boxed);
    env->functions->DeleteLocalRef(env, boxed);
    return r;
}

static bool IsCoordRange(double v) { return v > -60000.0 && v < 60000.0 && v != 0.0; }

// ── Heurística singleton ──────────────────────────────────────────────────────
// Encontra uma classe com nome curto (sem pacote) que tem campo STATIC do próprio tipo.
// Em Minecraft vanilla obfuscado, é a classe Minecraft com "theMinecraft".
static jclass FindSingletonClass(JNIEnv* env, JVMTIEnv* jvmti) {
    auto* t = jvmti->functions;
    auto* f = env->functions;

    jint    count   = 0;
    jclass* classes = nullptr;
    if (t->GetLoadedClasses(jvmti, &count, &classes) != JVMTI_ERROR_NONE || !classes)
        return nullptr;

    static const jint ACC_STATIC = 0x0008;
    jclass result = nullptr;

    for (jint k = 0; k < count && !result; k++) {
        // Filtra: nome curto sem pacote (ex: "Lbib;" ou "Low;")
        char* sig = nullptr;
        bool  ok  = false;
        if (t->GetClassSignature(jvmti, classes[k], &sig, nullptr) == JVMTI_ERROR_NONE && sig) {
            int len = (int)strlen(sig);
            ok = (len <= 7 && strchr(sig, '/') == nullptr);
            t->Deallocate(jvmti, (unsigned char*)sig);
        }
        if (!ok) { f->DeleteLocalRef(env, classes[k]); continue; }

        // Pega nome da classe
        std::string className = GetClassName(env, classes[k]);
        if (className.empty()) { f->DeleteLocalRef(env, classes[k]); continue; }

        // getDeclaredFields() do classes[k] (um jclass = java.lang.Class instance)
        jobject fieldsArr = GetDeclaredFields(env, classes[k]);
        if (!fieldsArr) { f->DeleteLocalRef(env, classes[k]); continue; }

        jsize n = f->GetArrayLength(env, fieldsArr);
        for (jsize i = 0; i < n; i++) {
            jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
            if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            jint mods = FieldGetMods(env, field);
            if (mods & ACC_STATIC) {
                std::string typeName = FieldTypeName(env, field);
                if (typeName == className) {
                    std::cout << "[+] SDK: singleton candidato: " << className
                              << " (" << n << " campos)" << std::endl;
                    result = classes[k];
                    f->DeleteLocalRef(env, field);
                    break;
                }
            }
            f->DeleteLocalRef(env, field);
        }
        f->DeleteLocalRef(env, fieldsArr);
        if (!result) f->DeleteLocalRef(env, classes[k]);
    }

    // Libera refs restantes
    for (jint k = 0; k < count; k++)
        if (classes[k] != result) f->DeleteLocalRef(env, classes[k]);
    t->Deallocate(jvmti, (unsigned char*)classes);
    return result;
}

// ── Pega a instância singleton do campo static do próprio tipo ────────────────
static jobject GetSingletonInstance(JNIEnv* env, jclass cls, const std::string& className) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;

    // getDeclaredFields em cls (que é o java.lang.Class para a classe singleton)
    jobject fieldsArr = GetDeclaredFields(env, cls);
    if (!fieldsArr) return nullptr;

    jsize n = f->GetArrayLength(env, fieldsArr);
    jobject result = nullptr;

    for (jsize i = 0; i < n && !result; i++) {
        jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        jint mods = FieldGetMods(env, field);
        if ((mods & ACC_STATIC) && FieldTypeName(env, field) == className) {
            // Pega valor do campo estático
            result = FieldGet(env, field, nullptr); // null = static field
        }
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, fieldsArr);
    return result;
}

// ── Encontra List de entidades navegando campos do Minecraft ──────────────────
// Procura em dois níveis: campos do Minecraft → campos do World → List
static jobject FindPlayerEntityList(JNIEnv* env, jobject mcInstance, jclass mcCls) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;

    // getDeclaredFields do Minecraft
    jobject mcFields = GetDeclaredFields(env, mcCls);
    if (!mcFields) return nullptr;

    jsize mcFieldCount = f->GetArrayLength(env, mcFields);

    for (jsize i = 0; i < mcFieldCount; i++) {
        jobject field = f->GetObjectArrayElement(env, mcFields, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        // Pula static e primitivos
        jint mods = FieldGetMods(env, field);
        if (mods & ACC_STATIC) { f->DeleteLocalRef(env, field); continue; }

        std::string typeName = FieldTypeName(env, field);
        bool isObj = (!typeName.empty() && typeName[0] != '[' &&
                      typeName != "int"  && typeName != "float"   &&
                      typeName != "double" && typeName != "boolean" &&
                      typeName != "long"  && typeName != "byte"   &&
                      typeName != "short" && typeName != "char"   &&
                      typeName != "java.lang.String");
        if (!isObj) { f->DeleteLocalRef(env, field); continue; }

        jobject fieldVal = FieldGet(env, field, mcInstance);
        f->DeleteLocalRef(env, field);
        if (!fieldVal || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        // Nível 2: itera campos do objeto apontado por fieldVal
        jclass valCls   = f->GetObjectClass(env, fieldVal);
        jobject subFields = GetDeclaredFields(env, valCls);
        f->DeleteLocalRef(env, valCls);

        if (!subFields) { f->DeleteLocalRef(env, fieldVal); continue; }

        jsize sn = f->GetArrayLength(env, subFields);
        jobject found = nullptr;

        for (jsize si = 0; si < sn && !found; si++) {
            jobject sf = f->GetObjectArrayElement(env, subFields, si);
            if (!sf || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            std::string sfType = FieldTypeName(env, sf);
            bool isList = (sfType.find("List") != std::string::npos);
            if (!isList) { f->DeleteLocalRef(env, sf); continue; }

            jobject listVal = FieldGet(env, sf, fieldVal);
            f->DeleteLocalRef(env, sf);
            if (!listVal || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            // Verifica tamanho
            jint sz = f->CallIntMethod(env, listVal, s_listSizeM);
            if (f->ExceptionCheck(env)) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }
            if (sz == 0) { f->DeleteLocalRef(env, listVal); continue; }

            // Verifica se o primeiro elemento tem 3 campos double com coords razoáveis
            jobject elem = f->CallObjectMethod(env, listVal, s_listGetM, (jint)0);
            if (!elem || f->ExceptionCheck(env)) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }

            jclass elemCls   = f->GetObjectClass(env, elem);
            jobject elemFlds = GetDeclaredFields(env, elemCls);
            f->DeleteLocalRef(env, elemCls);

            int coordCount = 0;
            if (elemFlds) {
                jsize en = f->GetArrayLength(env, elemFlds);
                for (jsize ei = 0; ei < en && coordCount < 3; ei++) {
                    jobject ef = f->GetObjectArrayElement(env, elemFlds, ei);
                    if (!ef || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                    if (FieldTypeName(env, ef) == "double") {
                        double dv = FieldGetDouble(env, ef, elem);
                        if (IsCoordRange(dv)) coordCount++;
                    }
                    f->DeleteLocalRef(env, ef);
                }
                f->DeleteLocalRef(env, elemFlds);
            }
            f->DeleteLocalRef(env, elem);

            if (coordCount >= 3) {
                std::cout << "[+] SDK: lista de jogadores encontrada (" << sz << " entidades)." << std::endl;
                found = f->NewGlobalRef(env, listVal);
            }
            f->DeleteLocalRef(env, listVal);
        }
        f->DeleteLocalRef(env, subFields);
        f->DeleteLocalRef(env, fieldVal);
        if (found) { f->DeleteLocalRef(env, mcFields); return found; }
    }
    f->DeleteLocalRef(env, mcFields);
    return nullptr;
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

    // 1. Encontra a classe singleton
    jclass localMcClass = FindSingletonClass(env, jvmti);
    if (!localMcClass) {
        std::cout << "[-] SDK: classe singleton nao encontrada." << std::endl;
        g_initFailed = true; return false;
    }
    g_mcClass = (jclass)env->functions->NewGlobalRef(env, localMcClass);
    env->functions->DeleteLocalRef(env, localMcClass);
    std::string mcName = GetClassName(env, g_mcClass);
    std::cout << "[+] SDK: classe Minecraft: " << mcName << std::endl;

    // 2. Pega a instância singleton
    jobject inst = GetSingletonInstance(env, g_mcClass, mcName);
    if (!inst) {
        std::cout << "[-] SDK: instancia singleton nula (jogo ainda inicializando?)." << std::endl;
        g_initFailed = true; return false;
    }
    g_mcInstance = env->functions->NewGlobalRef(env, inst);
    env->functions->DeleteLocalRef(env, inst);
    std::cout << "[+] SDK: instancia Minecraft obtida." << std::endl;

    // 3. Encontra a lista de jogadores
    g_entityList = FindPlayerEntityList(env, g_mcInstance, g_mcClass);
    if (!g_entityList) {
        std::cout << "[-] SDK: lista de jogadores nao encontrada." << std::endl;
        g_initFailed = true; return false;
    }

    std::cout << "[+] SDK: inicializacao completa!" << std::endl;
    return true;
}

// ── Cache dos campos double de posição ────────────────────────────────────────
static bool CacheEntityFields(JNIEnv* env, jobject entity) {
    if (g_posXField) return true;
    auto* f = env->functions;
    jclass eCls = f->GetObjectClass(env, entity);
    jobject fieldsArr = GetDeclaredFields(env, eCls);
    f->DeleteLocalRef(env, eCls);
    if (!fieldsArr) return false;

    jsize n = f->GetArrayLength(env, fieldsArr);
    jobject cands[3] = {};
    int found = 0;

    for (jsize i = 0; i < n && found < 3; i++) {
        jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
        if (FieldTypeName(env, field) == "double") {
            double dv = FieldGetDouble(env, field, entity);
            if (IsCoordRange(dv)) { cands[found++] = field; continue; }
        }
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, fieldsArr);

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

// ── API pública ───────────────────────────────────────────────────────────────

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
