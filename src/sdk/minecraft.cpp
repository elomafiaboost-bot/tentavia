#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>
#include <cstring>

// ─── Estratégia de descoberta de classes/campos ───────────────────────────────
//
// O Minecraft que está rodando tem classes obfuscadas (ex: "bib" para Minecraft).
// Em vez de procurar pelo nome desobfuscado, usamos heurísticas:
//
// 1. ENCONTRAR A CLASSE MINECRAFT:
//    Procura uma classe com nome curto (1-5 chars, sem pacote) que tenha
//    um campo STATIC do próprio tipo (padrão singleton: "theMinecraft").
//
// 2. ENCONTRAR A LISTA DE ENTIDADES:
//    A partir da instância Minecraft, busca campos não-nulos cujo tipo
//    é java.util.List (ou subclasse). Checa se a lista contém objetos com
//    3+ campos double em faixa razoável de coordenadas. Esse é o playerEntities.
//
// 3. POSIÇÃO DA CÂMERA:
//    O local player é o primeiro objeto da lista cuja classe tem campo static
//    do próprio tipo (padrão singleton para Entity? não). Na prática, procuramos
//    o objeto com posição DIFERENTE dos outros no playerEntities — ou usamos
//    qualquer entrada da lista como referência de câmera (em singleplayer é o
//    único jogador; em multiplayer excluímos pelo nome).
//
// Todos os caches abaixo são populados na primeira chamada bem-sucedida.
// ─────────────────────────────────────────────────────────────────────────────

namespace SDK {

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

// ── Cache ─────────────────────────────────────────────────────────────────────
static jclass  g_mcClass      = nullptr;
static jobject g_mcInstance   = nullptr;  // global ref
static jobject g_entityList   = nullptr;  // global ref para a List de entidades
static jobject g_localPlayer  = nullptr;  // global ref para o local player
static bool    g_initDone     = false;
static bool    g_initFailed   = false;
static int     g_retryCount   = 0;

// ── Helpers de reflexão JNI ───────────────────────────────────────────────────

// Retorna a classe do objeto (GetObjectClass = local ref)
static inline jclass ObjClass(JNIEnv* env, jobject obj) {
    return env->functions->GetObjectClass(env, obj);
}

// Chama getDeclaredFields() em um objeto Class, retorna Field[] (local ref)
static jobject CallGetDeclaredFields(JNIEnv* env, jclass cls) {
    auto* f = env->functions;
    jclass classCls    = f->FindClass(env, "java/lang/Class");
    if (!classCls) { f->ExceptionClear(env); return nullptr; }
    jmethodID gdfM     = f->GetMethodID(env, classCls, "getDeclaredFields",
                                         "()[Ljava/lang/reflect/Field;");
    if (!gdfM) { f->ExceptionClear(env); return nullptr; }
    jobject fields = f->CallObjectMethod(env, cls, gdfM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return fields;
}

// Chama Field.getModifiers() → int
static jint FieldGetModifiers(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jclass fieldCls = ObjClass(env, field);
    jmethodID m     = f->GetMethodID(env, fieldCls, "getModifiers", "()I");
    f->DeleteLocalRef(env, fieldCls);
    if (!m) { f->ExceptionClear(env); return 0; }
    jint r = f->CallIntMethod(env, field, m);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
    return r;
}

// Chama Field.getType().getName() → std::string
static std::string FieldTypeName(JNIEnv* env, jobject field) {
    auto* f = env->functions;
    jclass fieldCls   = ObjClass(env, field);
    jmethodID getTypeM = f->GetMethodID(env, fieldCls, "getType", "()Ljava/lang/Class;");
    f->DeleteLocalRef(env, fieldCls);
    if (!getTypeM) { f->ExceptionClear(env); return ""; }
    jobject typeObj = f->CallObjectMethod(env, field, getTypeM);
    if (!typeObj || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }

    jclass typeCls    = ObjClass(env, typeObj);
    jmethodID nameM   = f->GetMethodID(env, typeCls, "getName", "()Ljava/lang/String;");
    f->DeleteLocalRef(env, typeCls);
    if (!nameM) { f->ExceptionClear(env); f->DeleteLocalRef(env, typeObj); return ""; }
    jstring ns = (jstring)f->CallObjectMethod(env, typeObj, nameM);
    f->DeleteLocalRef(env, typeObj);
    if (!ns || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }

    const char* cs = f->GetStringUTFChars(env, ns, nullptr);
    std::string result = cs ? cs : "";
    if (cs) f->ReleaseStringUTFChars(env, ns, cs);
    f->DeleteLocalRef(env, ns);
    return result;
}

// Chama Class.getName() no objeto dado
static std::string ClassName(JNIEnv* env, jobject obj) {
    auto* f = env->functions;
    jclass cls      = ObjClass(env, obj);
    jmethodID nameM = f->GetMethodID(env, cls, "getName", "()Ljava/lang/String;");
    f->DeleteLocalRef(env, cls);
    if (!nameM) { f->ExceptionClear(env); return ""; }
    jstring ns = (jstring)f->CallObjectMethod(env, obj, nameM);
    if (!ns || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    const char* cs = f->GetStringUTFChars(env, ns, nullptr);
    std::string r = cs ? cs : "";
    if (cs) f->ReleaseStringUTFChars(env, ns, cs);
    f->DeleteLocalRef(env, ns);
    return r;
}

// Chama Field.get(instance) → jobject com setAccessible(true) antes
static jobject FieldGet(JNIEnv* env, jobject field, jobject instance) {
    auto* f = env->functions;
    jclass fieldCls = ObjClass(env, field);

    // setAccessible(true) para campos privados
    jmethodID setAccM = f->GetMethodID(env, fieldCls, "setAccessible", "(Z)V");
    if (setAccM) {
        f->CallObjectMethod(env, field, setAccM, (jboolean)1);
        if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    }

    jmethodID getM = f->GetMethodID(env, fieldCls, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    f->DeleteLocalRef(env, fieldCls);
    if (!getM) { f->ExceptionClear(env); return nullptr; }
    jobject val = f->CallObjectMethod(env, field, getM, instance);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return val;
}

// Chama Field.getDouble(instance) → jdouble
static jdouble FieldGetDouble(JNIEnv* env, jobject field, jobject instance) {
    auto* f = env->functions;
    jclass fieldCls = ObjClass(env, field);

    jmethodID setAccM = f->GetMethodID(env, fieldCls, "setAccessible", "(Z)V");
    if (setAccM) {
        f->CallObjectMethod(env, field, setAccM, (jboolean)1);
        if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    }

    jmethodID getDblM = f->GetMethodID(env, fieldCls, "getDouble",
                                        "(Ljava/lang/Object;)D");
    f->DeleteLocalRef(env, fieldCls);
    if (!getDblM) { f->ExceptionClear(env); return 0.0; }

    // Precisamos chamar getDouble sem variadics. Usamos CallObjectMethod workaround?
    // Não — usamos a versão double. Mas não temos CallDoubleMethod no stub.
    // Workaround: chamar get() e converter via doubleValue() se for Double.
    // Mais fácil: usar a reflexão de Field.getDouble via CallObjectMethod retornando
    // um Double boxed. Mas getDouble retorna primitivo double...
    // Solução: chamar Field.get() que retorna Object (autoboxed Double), depois .doubleValue()
    jmethodID getM = f->GetMethodID(env, ObjClass(env, field), "get",
                                     "(Ljava/lang/Object;)Ljava/lang/Object;");
    (void)getDblM; // não usamos
    if (!getM) { f->ExceptionClear(env); return 0.0; }
    jobject boxed = f->CallObjectMethod(env, field, getM, instance);
    if (!boxed || f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0.0; }

    jclass dblCls    = f->FindClass(env, "java/lang/Double");
    jmethodID dblValM = f->GetMethodID(env, dblCls, "doubleValue", "()D");
    // Não temos CallDoubleMethod no stub — usar CallObjectMethod não funciona para primitivo.
    // Workaround final: usar toString() e parsear o valor.
    jmethodID toStrM = f->GetMethodID(env, dblCls, "toString", "()Ljava/lang/String;");
    f->DeleteLocalRef(env, dblCls);
    (void)dblValM;
    if (!toStrM) { f->ExceptionClear(env); f->DeleteLocalRef(env, boxed); return 0.0; }
    jstring sval = (jstring)f->CallObjectMethod(env, boxed, toStrM);
    f->DeleteLocalRef(env, boxed);
    if (!sval || f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0.0; }
    const char* cs = f->GetStringUTFChars(env, sval, nullptr);
    jdouble result = cs ? atof(cs) : 0.0;
    if (cs) f->ReleaseStringUTFChars(env, sval, cs);
    f->DeleteLocalRef(env, sval);
    return result;
}

// ── Heurística: encontra classe com campo static do próprio tipo (singleton) ─
// Retorna local ref ou nullptr.
static jclass FindSingletonClass(JNIEnv* env, JVMTIEnv* jvmti) {
    auto* t = jvmti->functions;
    auto* f = env->functions;

    jint    count   = 0;
    jclass* classes = nullptr;
    if (t->GetLoadedClasses(jvmti, &count, &classes) != JVMTI_ERROR_NONE || !classes)
        return nullptr;

    // Pré-carrega métodos de reflexão (uma vez)
    jclass classCls = f->FindClass(env, "java/lang/Class");
    if (!classCls) { f->ExceptionClear(env); return nullptr; }
    jmethodID getNameM = f->GetMethodID(env, classCls, "getName", "()Ljava/lang/String;");
    jmethodID gdfM     = f->GetMethodID(env, classCls, "getDeclaredFields",
                                         "()[Ljava/lang/reflect/Field;");
    if (!getNameM || !gdfM) { f->ExceptionClear(env); return nullptr; }

    static const jint ACC_STATIC = 0x0008;
    jclass result = nullptr;
    int candidateCount = 0;

    for (jint k = 0; k < count && !result; k++) {
        // Filtra para classes com nome curto (sem pacote = obfuscado)
        char* sig = nullptr;
        bool isShort = false;
        if (t->GetClassSignature(jvmti, classes[k], &sig, nullptr) == JVMTI_ERROR_NONE && sig) {
            int len = (int)strlen(sig);
            // Formato "Lxxx;" sem '/' → curta sem pacote
            isShort = (len <= 7 && strchr(sig, '/') == nullptr);
            t->Deallocate(jvmti, (unsigned char*)sig);
        }
        if (!isShort) { f->DeleteLocalRef(env, classes[k]); continue; }

        // Pega nome da classe via getName()
        jstring nameStr = (jstring)f->CallObjectMethod(env, classes[k], getNameM);
        if (!nameStr || f->ExceptionCheck(env)) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, classes[k]); continue;
        }
        const char* cn = f->GetStringUTFChars(env, nameStr, nullptr);
        std::string className = cn ? cn : "";
        if (cn) f->ReleaseStringUTFChars(env, nameStr, cn);
        f->DeleteLocalRef(env, nameStr);

        // getDeclaredFields
        jobject fieldsArr = f->CallObjectMethod(env, classes[k], gdfM);
        if (!fieldsArr || f->ExceptionCheck(env)) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, classes[k]); continue;
        }

        jsize fieldCount = f->GetArrayLength(env, fieldsArr);

        for (jsize fi = 0; fi < fieldCount; fi++) {
            jobject field = f->GetObjectArrayElement(env, fieldsArr, fi);
            if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            jint mods = FieldGetModifiers(env, field);
            if (!(mods & ACC_STATIC)) { f->DeleteLocalRef(env, field); continue; }

            std::string typeName = FieldTypeName(env, field);
            f->DeleteLocalRef(env, field);

            if (typeName == className) {
                candidateCount++;
                std::cout << "[+] SDK: encontrado singleton candidato: " << className
                          << " (" << fieldCount << " campos)" << std::endl;
                result = classes[k];
                break;
            }
        }
        f->DeleteLocalRef(env, fieldsArr);
        if (!result) f->DeleteLocalRef(env, classes[k]);
    }

    // Libera refs restantes
    for (jint k = 0; k < count; k++)
        if (classes[k] != result) f->DeleteLocalRef(env, classes[k]);
    t->Deallocate(jvmti, (unsigned char*)classes);

    if (candidateCount > 1)
        std::cout << "[!] SDK: " << candidateCount << " singletons encontrados — usando o primeiro." << std::endl;

    return result;
}

// ── Navega campos do Minecraft para encontrar a List de jogadores próximos ───
// Procura campos de tipo List dentro de campos Object do Minecraft.
// Valida que a List contém objetos com double fields em faixa de coordenadas.
static bool IsCoordRange(double v) { return v > -60000.0 && v < 60000.0 && v != 0.0; }

static jobject FindPlayerEntityList(JNIEnv* env, jobject mcInstance) {
    auto* f = env->functions;

    // getDeclaredFields do Minecraft
    jclass mcCls     = ObjClass(env, mcInstance);
    jobject fieldsArr = f->CallObjectMethod(env, mcCls,
        f->GetMethodID(env, mcCls, "getDeclaredFields", "()[Ljava/lang/reflect/Field;"),
        nullptr);
    f->DeleteLocalRef(env, mcCls);
    if (!fieldsArr || f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }

    jsize n = f->GetArrayLength(env, fieldsArr);

    // Para cada campo Object de Minecraft:
    for (jsize i = 0; i < n; i++) {
        jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        std::string typeName = FieldTypeName(env, field);
        // Pula primitivos e strings
        bool isObj = !typeName.empty() && typeName[0] != '[' &&
                     typeName != "int" && typeName != "float" &&
                     typeName != "double" && typeName != "boolean" &&
                     typeName != "long" && typeName != "byte" &&
                     typeName != "short" && typeName != "char" &&
                     typeName != "java.lang.String";
        if (!isObj) { f->DeleteLocalRef(env, field); continue; }

        jobject fieldVal = FieldGet(env, field, mcInstance);
        f->DeleteLocalRef(env, field);
        if (!fieldVal || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        // Verifica campos do objeto (procura List)
        jclass valCls    = ObjClass(env, fieldVal);
        jobject subFields = nullptr;
        {
            jmethodID gdfM = f->GetMethodID(env, valCls, "getDeclaredFields",
                                             "()[Ljava/lang/reflect/Field;");
            if (gdfM) subFields = f->CallObjectMethod(env, valCls, gdfM);
            if (f->ExceptionCheck(env)) f->ExceptionClear(env);
        }
        f->DeleteLocalRef(env, valCls);

        if (!subFields) { f->DeleteLocalRef(env, fieldVal); continue; }

        jsize sn = f->GetArrayLength(env, subFields);
        for (jsize si = 0; si < sn; si++) {
            jobject sf = f->GetObjectArrayElement(env, subFields, si);
            if (!sf || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            std::string sfType = FieldTypeName(env, sf);
            // List contém java.util.List no tipo
            bool isList = (sfType.find("List") != std::string::npos ||
                           sfType.find("ArrayList") != std::string::npos ||
                           sfType.find("AbstractList") != std::string::npos);
            if (!isList) { f->DeleteLocalRef(env, sf); continue; }

            jobject listVal = FieldGet(env, sf, fieldVal);
            f->DeleteLocalRef(env, sf);
            if (!listVal || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

            // Verifica tamanho da lista
            jclass listCls = ObjClass(env, listVal);
            jmethodID sizeM = f->GetMethodID(env, listCls, "size", "()I");
            f->DeleteLocalRef(env, listCls);
            if (!sizeM) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }
            jint sz = f->CallIntMethod(env, listVal, sizeM);
            if (f->ExceptionCheck(env)) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }
            if (sz == 0) { f->DeleteLocalRef(env, listVal); continue; }

            // Pega primeiro elemento e verifica se tem campos double em faixa de coordenadas
            jclass listCls2 = ObjClass(env, listVal);
            jmethodID getM  = f->GetMethodID(env, listCls2, "get", "(I)Ljava/lang/Object;");
            f->DeleteLocalRef(env, listCls2);
            if (!getM) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }
            jobject elem = f->CallObjectMethod(env, listVal, getM, (jint)0);
            if (!elem || f->ExceptionCheck(env)) { f->ExceptionClear(env); f->DeleteLocalRef(env, listVal); continue; }

            // Conta campos double com valores razoáveis de coordenada
            jclass elemCls = ObjClass(env, elem);
            jobject elemFields = nullptr;
            {
                jmethodID gdfM = f->GetMethodID(env, elemCls, "getDeclaredFields",
                                                 "()[Ljava/lang/reflect/Field;");
                if (gdfM) elemFields = f->CallObjectMethod(env, elemCls, gdfM);
                if (f->ExceptionCheck(env)) f->ExceptionClear(env);
            }
            f->DeleteLocalRef(env, elemCls);
            int coordCount = 0;
            if (elemFields) {
                jsize en = f->GetArrayLength(env, elemFields);
                for (jsize ei = 0; ei < en && coordCount < 3; ei++) {
                    jobject ef = f->GetObjectArrayElement(env, elemFields, ei);
                    if (!ef || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                    if (FieldTypeName(env, ef) == "double") {
                        jdouble dv = FieldGetDouble(env, ef, elem);
                        if (IsCoordRange(dv)) coordCount++;
                    }
                    f->DeleteLocalRef(env, ef);
                }
                f->DeleteLocalRef(env, elemFields);
            }
            f->DeleteLocalRef(env, elem);

            if (coordCount >= 3) {
                // Encontrou! Retorna global ref para a lista
                f->DeleteLocalRef(env, subFields);
                f->DeleteLocalRef(env, fieldVal);
                f->DeleteLocalRef(env, fieldsArr);
                jobject globalList = f->NewGlobalRef(env, listVal);
                f->DeleteLocalRef(env, listVal);
                std::cout << "[+] SDK: playerEntities encontrado via heurística (" << sz << " jogadores)." << std::endl;
                return globalList;
            }
            f->DeleteLocalRef(env, listVal);
        }
        f->DeleteLocalRef(env, subFields);
        f->DeleteLocalRef(env, fieldVal);
    }
    f->DeleteLocalRef(env, fieldsArr);
    return nullptr;
}

// ── Init ──────────────────────────────────────────────────────────────────────
static bool InitSDK(JNIEnv* env) {
    if (g_initDone) return !g_initFailed;
    g_initDone = true;

    JVMTIEnv* jvmti = JNIUtils::GetJVMTIEnv();
    if (!jvmti) {
        std::cout << "[-] SDK: JVMTI nao disponivel." << std::endl;
        g_initFailed = true;
        return false;
    }

    // 1. Encontra a classe singleton do Minecraft
    jclass localMcClass = FindSingletonClass(env, jvmti);
    if (!localMcClass) {
        std::cout << "[-] SDK: nao encontrou classe singleton Minecraft." << std::endl;
        g_initFailed = true;
        return false;
    }
    g_mcClass = (jclass)env->functions->NewGlobalRef(env, localMcClass);
    env->functions->DeleteLocalRef(env, localMcClass);
    std::string mcName = ClassName(env, g_mcClass);
    std::cout << "[+] SDK: classe Minecraft encontrada: " << mcName << std::endl;

    // 2. Pega instância singleton via getDeclaredFields (campo static do próprio tipo)
    {
        auto* f = env->functions;
        jclass mcCls     = ObjClass(env, g_mcClass);
        jobject fieldsArr = f->CallObjectMethod(env, mcCls,
            f->GetMethodID(env, mcCls, "getDeclaredFields", "()[Ljava/lang/reflect/Field;"),
            nullptr);
        f->DeleteLocalRef(env, mcCls);
        if (!fieldsArr || f->ExceptionCheck(env)) {
            f->ExceptionClear(env);
            std::cout << "[-] SDK: getDeclaredFields falhou." << std::endl;
            g_initFailed = true;
            return false;
        }
        jsize n = f->GetArrayLength(env, fieldsArr);
        static const jint ACC_STATIC = 0x0008;
        for (jsize i = 0; i < n && !g_mcInstance; i++) {
            jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
            if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            jint mods = FieldGetModifiers(env, field);
            if ((mods & ACC_STATIC) && FieldTypeName(env, field) == mcName) {
                jobject inst = FieldGet(env, field, nullptr);
                if (inst) {
                    g_mcInstance = f->NewGlobalRef(env, inst);
                    f->DeleteLocalRef(env, inst);
                }
            }
            f->DeleteLocalRef(env, field);
        }
        f->DeleteLocalRef(env, fieldsArr);
    }
    if (!g_mcInstance) {
        std::cout << "[-] SDK: instancia Minecraft nao encontrada (null)." << std::endl;
        g_initFailed = true;
        return false;
    }
    std::cout << "[+] SDK: instancia Minecraft obtida." << std::endl;

    // 3. Encontra lista de jogadores
    g_entityList = FindPlayerEntityList(env, g_mcInstance);
    if (!g_entityList) {
        std::cout << "[-] SDK: lista de jogadores nao encontrada." << std::endl;
        g_initFailed = true;
        return false;
    }

    std::cout << "[+] SDK: inicializacao completa!" << std::endl;
    return true;
}

// ── API publica ───────────────────────────────────────────────────────────────

// Campos de posição cacheados por classe de entidade
static jclass  g_entClass = nullptr;
static jobject g_posXField = nullptr; // global ref para Field de posX
static jobject g_posYField = nullptr;
static jobject g_posZField = nullptr;

static bool CacheEntityFields(JNIEnv* env, jobject entity) {
    if (g_posXField) return true;
    auto* f = env->functions;
    jclass eCls = ObjClass(env, entity);
    jmethodID gdfM = f->GetMethodID(env, eCls, "getDeclaredFields",
                                     "()[Ljava/lang/reflect/Field;");
    if (!gdfM) { f->ExceptionClear(env); f->DeleteLocalRef(env, eCls); return false; }
    jobject fieldsArr = f->CallObjectMethod(env, eCls, gdfM);
    f->DeleteLocalRef(env, eCls);
    if (!fieldsArr || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jsize n = f->GetArrayLength(env, fieldsArr);
    // Coleta campos double com coordenadas razoáveis em ordem: os três primeiros são posX,Y,Z
    int found = 0;
    jobject candidates[3] = {};
    for (jsize i = 0; i < n && found < 3; i++) {
        jobject field = f->GetObjectArrayElement(env, fieldsArr, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
        if (FieldTypeName(env, field) == "double") {
            jdouble v = FieldGetDouble(env, field, entity);
            if (IsCoordRange(v)) {
                candidates[found++] = field;
                continue;
            }
        }
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, fieldsArr);
    if (found < 3) {
        for (int i = 0; i < found; i++) f->DeleteLocalRef(env, candidates[i]);
        return false;
    }
    g_posXField = f->NewGlobalRef(env, candidates[0]);
    g_posYField = f->NewGlobalRef(env, candidates[1]);
    g_posZField = f->NewGlobalRef(env, candidates[2]);
    for (int i = 0; i < 3; i++) f->DeleteLocalRef(env, candidates[i]);
    std::cout << "[+] SDK: campos posX/Y/Z cacheados." << std::endl;
    return true;
}

bool Minecraft::GetNearbyPlayers(std::vector<EntityInfo>& out) {
    JNIEnv* env = Env();
    if (!env) return false;

    // Retry se init falhou
    if (g_initFailed) { g_initDone = false; g_initFailed = false; }
    if (!InitSDK(env)) {
        g_retryCount++;
        if (g_retryCount <= 3 || (g_retryCount % 120) == 0)
            std::cout << "[-] SDK: init falhou (tentativa " << g_retryCount << ")" << std::endl;
        return false;
    }

    auto* f = env->functions;
    jclass listCls  = ObjClass(env, g_entityList);
    jmethodID sizeM = f->GetMethodID(env, listCls, "size", "()I");
    jmethodID getM  = f->GetMethodID(env, listCls, "get",  "(I)Ljava/lang/Object;");
    f->DeleteLocalRef(env, listCls);
    if (!sizeM || !getM) { f->ExceptionClear(env); return false; }

    jint count = f->CallIntMethod(env, g_entityList, sizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    for (jint i = 0; i < count; i++) {
        jobject entity = f->CallObjectMethod(env, g_entityList, getM, i);
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
    if (!g_initDone) {
        JNIEnv* env = Env();
        if (!env) return false;
        if (g_initFailed) { g_initDone = false; g_initFailed = false; }
        if (!InitSDK(env)) return false;
    }
    if (!g_entityList) return false;

    // Para a câmera, pega as coordenadas do primeiro elemento da lista
    // (em singleplayer é o único jogador; em multiplayer é o mais próximo do servidor)
    // TODO: identificar o local player pelo nome de usuário
    std::vector<EntityInfo> tmp;
    if (!GetNearbyPlayers(tmp) || tmp.empty()) return false;

    // Usa o primeiro como referência de câmera (impreciso em multiplayer)
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
