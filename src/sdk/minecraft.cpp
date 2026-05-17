#include "minecraft.hpp"
#include "jni_utils.hpp"
#include <iostream>
#include <cstring>
#include <vector>
#include <cstdio>

namespace SDK {

static JNIEnv* Env() { return JNIUtils::GetJNIEnv(); }

// ── Globals ──────────────────────────────────────────────────────────────────
static jclass  g_mcClass         = nullptr;
static jobject g_mcInstance      = nullptr;
static jobject g_entityList      = nullptr;
static bool    g_entityListIsArr = false; // true = [Lxxx; array, false = java.util.List
static bool    g_initDone        = false;
static bool    g_initFailed      = false;
static int     g_retryCount      = 0;

static jobject g_posXField  = nullptr;
static jobject g_posYField  = nullptr;
static jobject g_posZField  = nullptr;
static jobject g_yawField   = nullptr; // rotationYaw (float) in Entity base class
static jobject g_pitchField = nullptr; // rotationPitch (float) in Entity base class

// ── Method cache ─────────────────────────────────────────────────────────────
static jmethodID s_gdfM         = nullptr;
static jmethodID s_getNameM     = nullptr;
static jmethodID s_getSuperM    = nullptr;
static jmethodID s_getDeclClsM  = nullptr; // Field.getDeclaringClass()
static jmethodID s_getModsM     = nullptr;
static jmethodID s_getTypeM     = nullptr;
static jmethodID s_getM         = nullptr;
static jmethodID s_setAccM      = nullptr;
static jmethodID s_toStrM       = nullptr;
static jmethodID s_listSizeM    = nullptr;
static jmethodID s_listGetM     = nullptr;
static jmethodID s_setDoubleM   = nullptr; // Field.setDouble(Object, double)

static bool CacheReflectMethods(JNIEnv* env) {
    if (s_gdfM) return true;
    auto* f = env->functions;
    jclass classCls = f->FindClass(env, "java/lang/Class");
    jclass fieldCls = f->FindClass(env, "java/lang/reflect/Field");
    jclass objCls   = f->FindClass(env, "java/lang/Object");
    jclass listCls  = f->FindClass(env, "java/util/List");
    if (!classCls || !fieldCls || !objCls || !listCls) { f->ExceptionClear(env); return false; }
    s_gdfM        = f->GetMethodID(env, classCls, "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    s_getNameM    = f->GetMethodID(env, classCls, "getName",           "()Ljava/lang/String;");
    s_getSuperM   = f->GetMethodID(env, classCls, "getSuperclass",     "()Ljava/lang/Class;");
    s_getDeclClsM = f->GetMethodID(env, fieldCls, "getDeclaringClass", "()Ljava/lang/Class;");
    s_getModsM    = f->GetMethodID(env, fieldCls, "getModifiers",      "()I");
    s_getTypeM    = f->GetMethodID(env, fieldCls, "getType",           "()Ljava/lang/Class;");
    s_getM        = f->GetMethodID(env, fieldCls, "get",               "(Ljava/lang/Object;)Ljava/lang/Object;");
    s_setAccM     = f->GetMethodID(env, fieldCls, "setAccessible",     "(Z)V");
    s_toStrM      = f->GetMethodID(env, objCls,   "toString",          "()Ljava/lang/String;");
    s_listSizeM   = f->GetMethodID(env, listCls,  "size",              "()I");
    s_listGetM    = f->GetMethodID(env, listCls,  "get",               "(I)Ljava/lang/Object;");
    s_setDoubleM  = f->GetMethodID(env, fieldCls, "setDouble", "(Ljava/lang/Object;D)V");
    f->DeleteLocalRef(env, classCls); f->DeleteLocalRef(env, fieldCls);
    f->DeleteLocalRef(env, objCls);   f->DeleteLocalRef(env, listCls);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); s_gdfM = nullptr; return false; }
    return s_gdfM && s_getNameM && s_getSuperM && s_getDeclClsM && s_getModsM && s_getTypeM &&
           s_getM && s_setAccM  && s_listSizeM && s_listGetM && s_setDoubleM;
}

// ── Reflection helpers ────────────────────────────────────────────────────────
static std::string GetClassName(JNIEnv* env, jobject cls) {
    auto* f = env->functions;
    jstring s = (jstring)f->CallObjectMethod(env, cls, s_getNameM);
    if (!s || f->ExceptionCheck(env)) { f->ExceptionClear(env); return ""; }
    const char* cs = f->GetStringUTFChars(env, s, nullptr);
    std::string r  = cs ? cs : "";
    if (cs) f->ReleaseStringUTFChars(env, s, cs);
    f->DeleteLocalRef(env, s);
    return r;
}

static jobject GetDeclaredFields(JNIEnv* env, jobject cls) {
    auto* f = env->functions;
    jobject a = f->CallObjectMethod(env, cls, s_gdfM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return a;
}

static jclass GetSuperclass(JNIEnv* env, jclass cls) {
    auto* f = env->functions;
    jclass s = (jclass)f->CallObjectMethod(env, cls, s_getSuperM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return s;
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
    std::string n = GetClassName(env, tc);
    f->DeleteLocalRef(env, tc);
    return n;
}

static jobject FieldGet(JNIEnv* env, jobject field, jobject instance) {
    auto* f = env->functions;
    f->CallVoidMethod(env, field, s_setAccM, (jboolean)1);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    jobject v = f->CallObjectMethod(env, field, s_getM, instance);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
    return v;
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

static double FieldGetDouble(JNIEnv* env, jobject field, jobject inst) {
    jobject b = FieldGet(env, field, inst);
    if (!b) return 0.0;
    double r = BoxedToDouble(env, b);
    env->functions->DeleteLocalRef(env, b);
    return r;
}

// Escreve um campo double via reflection (setAccessible + setDouble)
static void FieldSetDouble(JNIEnv* env, jobject field, jobject inst, double val) {
    auto* f = env->functions;
    f->CallVoidMethod(env, field, s_setAccM, (jboolean)1);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
    f->CallVoidMethod(env, field, s_setDoubleM, inst, val);
    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
}

static bool IsCoordRange(double v) { return v > -60000.0 && v < 60000.0; }

// Campos de motion cacheados (jobject = java.lang.reflect.Field)
static jobject g_motXField = nullptr;
static jobject g_motZField = nullptr;

// Object type (non-array, non-primitive, non-String)
static bool IsObjType(const std::string& t) {
    if (t.empty() || t[0] == '[') return false;
    if (t == "int"  || t == "float"   || t == "double"  || t == "boolean" ||
        t == "long" || t == "byte"    || t == "short"   || t == "char") return false;
    if (t == "java.lang.String") return false;
    return true;
}

// Object array type: [Lxxx;
static bool IsObjArrayType(const std::string& t) {
    return t.size() > 3 && t[0] == '[' && t[1] == 'L';
}

// Duck-type List.size()
static jint TryListSize(JNIEnv* env, jobject obj) {
    auto* f = env->functions;
    jint sz = f->CallIntMethod(env, obj, s_listSizeM);
    if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return -1; }
    return sz;
}

// ── Count coord doubles walking superclass chain ──────────────────────────────
static int CountCoordDoubles(JNIEnv* env, jobject entity) {
    auto* f = env->functions;
    int cnt = 0;
    jclass cls = f->GetObjectClass(env, entity);
    for (int d = 0; cls && cnt < 3 && d < 15; d++) {
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n && cnt < 3; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                if (FieldTypeName(env, field) == "double") {
                    double dv = FieldGetDouble(env, field, entity);
                    if (IsCoordRange(dv)) cnt++;
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
    return cnt;
}

// ── Field value container (object or object-array) ────────────────────────────
struct FV { std::string type; jobject val; bool isArr; };

static std::vector<FV> CollectFields(JNIEnv* env, jclass startCls, jobject inst) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;
    std::vector<FV> res;
    jclass cls = (jclass)f->NewGlobalRef(env, startCls);
    for (int d = 0; cls && d < 15; d++) {
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                jint mods = FieldGetMods(env, field);
                if (!(mods & ACC_STATIC)) {
                    std::string t = FieldTypeName(env, field);
                    bool isArr = IsObjArrayType(t);
                    bool isObj = IsObjType(t);
                    if (isObj || isArr) {
                        jobject v = FieldGet(env, field, inst);
                        if (v && !f->ExceptionCheck(env)) res.push_back({t, v, isArr});
                        else { f->ExceptionClear(env); if (v) f->DeleteLocalRef(env, v); }
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
    return res;
}

static void FreeFV(JNIEnv* env, std::vector<FV>& v) {
    for (auto& p : v) env->functions->DeleteLocalRef(env, p.val);
    v.clear();
}

// ── Try a value as entity collection (List or Object[]) ───────────────────────
// Returns element count if valid, 0 if not.
static jint TryEntityCollection(JNIEnv* env, const FV& fv) {
    auto* f = env->functions;
    if (fv.isArr) {
        jsize sz = f->GetArrayLength(env, fv.val);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
        if (sz <= 0) return 0;
        jobject elem = f->GetObjectArrayElement(env, fv.val, 0);
        if (!elem || f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
        int coords = CountCoordDoubles(env, elem);
        f->DeleteLocalRef(env, elem);
        return coords >= 1 ? (jint)sz : 0;
    } else {
        jint sz = TryListSize(env, fv.val);
        if (sz <= 0) return 0;
        jobject elem = f->CallObjectMethod(env, fv.val, s_listGetM, (jint)0);
        if (!elem || f->ExceptionCheck(env)) { f->ExceptionClear(env); return 0; }
        int coords = CountCoordDoubles(env, elem);
        f->DeleteLocalRef(env, elem);
        return coords >= 1 ? sz : 0;
    }
}

// ── Find entity collection: 3 levels deep (List OR Object[]) ─────────────────
// Returns global ref to the collection (List or array), or nullptr.
static jobject FindEntityCollection(JNIEnv* env, jobject mcInst, jclass mcCls,
                                    bool& outIsArr) {
    auto* f = env->functions;

    jobject bestVal  = nullptr;
    jint    bestSz   = 0;
    bool    bestIsArr = false;

    auto tryFV = [&](const FV& fv) {
        jint sz = TryEntityCollection(env, fv);
        if (sz > bestSz) {
            if (bestVal) f->DeleteLocalRef(env, bestVal);
            bestVal   = f->NewGlobalRef(env, fv.val);
            bestSz    = sz;
            bestIsArr = fv.isArr;
        }
    };

    auto L0 = CollectFields(env, mcCls, mcInst);
    for (auto& p0 : L0) {
        tryFV(p0);
        jclass c0 = f->GetObjectClass(env, p0.val);
        auto   L1 = CollectFields(env, c0, p0.val);
        f->DeleteLocalRef(env, c0);
        for (auto& p1 : L1) {
            tryFV(p1);
            if (!p1.isArr) {
                jclass c1 = f->GetObjectClass(env, p1.val);
                auto   L2 = CollectFields(env, c1, p1.val);
                f->DeleteLocalRef(env, c1);
                for (auto& p2 : L2) tryFV(p2);
                FreeFV(env, L2);
            }
        }
        FreeFV(env, L1);
    }
    FreeFV(env, L0);

    if (bestVal) {
        outIsArr = bestIsArr;
        std::cout << "[+] SDK: colecao de entidades encontrada ("
                  << bestSz << " elem, " << (bestIsArr ? "array" : "List") << ")" << std::endl;
    }
    return bestVal;
}

// ── Find ALL singleton candidates ─────────────────────────────────────────────
// Returns vector of global refs (caller must DeleteGlobalRef unused ones)
static std::vector<jclass> FindAllSingletonCandidates(JNIEnv* env, JVMTIEnv* jvmti) {
    auto* t = jvmti->functions;
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;

    jint    count   = 0;
    jclass* classes = nullptr;
    if (t->GetLoadedClasses(jvmti, &count, &classes) != JVMTI_ERROR_NONE || !classes)
        return {};

    std::vector<jclass> results;
    for (jint k = 0; k < count; k++) {
        char* sig = nullptr;
        bool  ok  = false;
        if (t->GetClassSignature(jvmti, classes[k], &sig, nullptr) == JVMTI_ERROR_NONE && sig) {
            int len = (int)strlen(sig);
            ok = (len <= 7 && !strchr(sig, '/'));
            t->Deallocate(jvmti, (unsigned char*)sig);
        }
        if (!ok) { f->DeleteLocalRef(env, classes[k]); continue; }

        std::string clsName = GetClassName(env, classes[k]);
        if (clsName.empty()) { f->DeleteLocalRef(env, classes[k]); continue; }

        jobject flds = GetDeclaredFields(env, classes[k]);
        bool found = false;
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                jint mods = FieldGetMods(env, field);
                if ((mods & ACC_STATIC) && FieldTypeName(env, field) == clsName)
                    found = true;
                f->DeleteLocalRef(env, field);
                if (found) break;
            }
            f->DeleteLocalRef(env, flds);
        }

        if (found) {
            std::cout << "[+] SDK: candidato: " << clsName << std::endl;
            results.push_back((jclass)f->NewGlobalRef(env, classes[k]));
        }
        f->DeleteLocalRef(env, classes[k]);
    }
    t->Deallocate(jvmti, (unsigned char*)classes);
    return results;
}

static jobject GetSingletonInstance(JNIEnv* env, jclass cls, const std::string& clsName) {
    auto* f = env->functions;
    static const jint ACC_STATIC = 0x0008;
    jobject flds = GetDeclaredFields(env, cls);
    if (!flds) return nullptr;
    jsize   n    = f->GetArrayLength(env, flds);
    jobject res  = nullptr;
    for (jsize i = 0; i < n && !res; i++) {
        jobject field = f->GetObjectArrayElement(env, flds, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
        jint mods = FieldGetMods(env, field);
        if ((mods & ACC_STATIC) && FieldTypeName(env, field) == clsName)
            res = FieldGet(env, field, nullptr);
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, flds);
    return res;
}

// ── Diagnostic dump (once) ────────────────────────────────────────────────────
static void DiagnoseStructure(JNIEnv* env, jobject mcInst, jclass mcCls,
                               const std::string& mcName) {
    static bool done = false;
    if (done) return;
    done = true;

    auto* f = env->functions;
    FILE* fp = fopen("tentavia_structure.txt", "a"); // append to keep prior content
    if (!fp) return;
    fprintf(fp, "\n=== %s ===\n", mcName.c_str());

    jclass cls = (jclass)f->NewGlobalRef(env, mcCls);
    for (int d = 0; cls && d < 8; d++) {
        std::string cn = GetClassName(env, cls);
        jobject flds = GetDeclaredFields(env, cls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            if (n > 0) fprintf(fp, "[chain %d] %s (%d campos)\n", d, cn.c_str(), (int)n);
            for (jsize i = 0; i < n; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                jint mods = FieldGetMods(env, field);
                bool isSt = (mods & 0x0008) != 0;
                std::string t = FieldTypeName(env, field);
                fprintf(fp, "  %s%s\n", isSt ? "static " : "", t.c_str());

                if (!isSt && (IsObjType(t) || IsObjArrayType(t))) {
                    jobject v = FieldGet(env, field, mcInst);
                    if (v) {
                        if (IsObjArrayType(t)) {
                            jsize asz = f->GetArrayLength(env, v);
                            if (f->ExceptionCheck(env)) f->ExceptionClear(env);
                            else fprintf(fp, "    → ARRAY[%d]\n", (int)asz);
                            if (asz > 0) {
                                jobject e0 = f->GetObjectArrayElement(env, v, 0);
                                if (e0 && !f->ExceptionCheck(env)) {
                                    jclass ec = f->GetObjectClass(env, e0);
                                    fprintf(fp, "    → elem[0] runtime: %s, coords=%d\n",
                                            GetClassName(env, ec).c_str(),
                                            CountCoordDoubles(env, e0));
                                    f->DeleteLocalRef(env, ec);
                                    f->DeleteLocalRef(env, e0);
                                } else f->ExceptionClear(env);
                            }
                        } else {
                            jclass vc = f->GetObjectClass(env, v);
                            fprintf(fp, "    → %s\n", GetClassName(env, vc).c_str());
                            jint lsz = TryListSize(env, v);
                            if (lsz >= 0) fprintf(fp, "    → LIST size=%d\n", (int)lsz);

                            // Level 1 sub-fields
                            jclass sc = (jclass)f->NewGlobalRef(env, vc);
                            for (int sd = 0; sc && sd < 6; sd++) {
                                jobject sflds = GetDeclaredFields(env, sc);
                                if (sflds) {
                                    jsize sn = f->GetArrayLength(env, sflds);
                                    for (jsize si = 0; si < sn; si++) {
                                        jobject sf = f->GetObjectArrayElement(env, sflds, si);
                                        if (!sf || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                                        jint sm = FieldGetMods(env, sf);
                                        bool ss = (sm & 0x0008) != 0;
                                        std::string st = FieldTypeName(env, sf);
                                        fprintf(fp, "    %s%s\n", ss ? "static " : "", st.c_str());
                                        if (!ss && (IsObjType(st) || IsObjArrayType(st))) {
                                            jobject sv = FieldGet(env, sf, v);
                                            if (sv) {
                                                if (IsObjArrayType(st)) {
                                                    jsize asz = f->GetArrayLength(env, sv);
                                                    if (f->ExceptionCheck(env)) f->ExceptionClear(env);
                                                    else {
                                                        fprintf(fp, "      → ARRAY[%d]\n", (int)asz);
                                                        if (asz > 0) {
                                                            jobject e0 = f->GetObjectArrayElement(env, sv, 0);
                                                            if (e0 && !f->ExceptionCheck(env)) {
                                                                fprintf(fp, "      → coords=%d\n",
                                                                        CountCoordDoubles(env, e0));
                                                                f->DeleteLocalRef(env, e0);
                                                            } else f->ExceptionClear(env);
                                                        }
                                                    }
                                                } else {
                                                    jint slsz = TryListSize(env, sv);
                                                    if (slsz >= 0) fprintf(fp, "      → LIST size=%d\n", (int)slsz);
                                                }
                                                f->DeleteLocalRef(env, sv);
                                            }
                                        }
                                        f->DeleteLocalRef(env, sf);
                                    }
                                    f->DeleteLocalRef(env, sflds);
                                }
                                jclass nsup = GetSuperclass(env, sc);
                                f->DeleteLocalRef(env, sc);
                                sc = nsup;
                            }
                            if (sc) f->DeleteLocalRef(env, sc);
                            f->DeleteLocalRef(env, vc);
                        }
                        f->DeleteLocalRef(env, v);
                    } else {
                        fprintf(fp, "    → null\n");
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
    fclose(fp);
    std::cout << "[DIAG] tentavia_structure.txt atualizado." << std::endl;
}

// ── Init ──────────────────────────────────────────────────────────────────────
static bool InitSDK(JNIEnv* env) {
    if (g_initDone) return !g_initFailed;
    g_initDone = true;

    if (!CacheReflectMethods(env)) {
        std::cout << "[-] SDK: falha ao cachear reflexao." << std::endl;
        g_initFailed = true; return false;
    }

    JVMTIEnv* jvmti = JNIUtils::GetJVMTIEnv();
    if (!jvmti) {
        std::cout << "[-] SDK: JVMTI nao disponivel." << std::endl;
        g_initFailed = true; return false;
    }

    auto candidates = FindAllSingletonCandidates(env, jvmti);
    if (candidates.empty()) {
        std::cout << "[-] SDK: nenhum candidato singleton encontrado." << std::endl;
        g_initFailed = true; return false;
    }

    for (jclass cand : candidates) {
        std::string candName = GetClassName(env, cand);
        jobject inst = GetSingletonInstance(env, cand, candName);
        if (!inst) {
            std::cout << "[-] SDK: " << candName << " instancia nula, pulando." << std::endl;
            env->functions->DeleteGlobalRef(env, cand);
            continue;
        }

        DiagnoseStructure(env, inst, cand, candName);

        bool isArr = false;
        jobject col = FindEntityCollection(env, inst, cand, isArr);
        if (col) {
            g_mcClass         = cand;
            g_mcInstance      = env->functions->NewGlobalRef(env, inst);
            g_entityList      = col;
            g_entityListIsArr = isArr;
            env->functions->DeleteLocalRef(env, inst);
            std::cout << "[+] SDK: Minecraft=" << candName << " pronto." << std::endl;
            // Delete remaining candidates
            bool found = false;
            for (jclass c : candidates) {
                if (found || c == cand) { found = true; continue; }
                env->functions->DeleteGlobalRef(env, c);
            }
            return true;
        }
        std::cout << "[-] SDK: " << candName << " sem lista de entidades." << std::endl;
        env->functions->DeleteLocalRef(env, inst);
        env->functions->DeleteGlobalRef(env, cand);
    }

    std::cout << "[-] SDK: nenhum candidato tem lista de entidades." << std::endl;
    g_initFailed = true;
    return false;
}

// ── Cache entity position fields (superclass chain) ───────────────────────────
static bool CacheEntityFields(JNIEnv* env, jobject entity) {
    if (g_posXField) return true;
    auto* f = env->functions;
    jobject cands[3] = {};
    int found = 0;
    jclass cls = f->GetObjectClass(env, entity);
    for (int d = 0; cls && found < 3 && d < 15; d++) {
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
    if (found < 3) { for (int i = 0; i < found; i++) f->DeleteLocalRef(env, cands[i]); return false; }
    g_posXField = f->NewGlobalRef(env, cands[0]);
    g_posYField = f->NewGlobalRef(env, cands[1]);
    g_posZField = f->NewGlobalRef(env, cands[2]);
    for (int i = 0; i < 3; i++) f->DeleteLocalRef(env, cands[i]);
    std::cout << "[+] SDK: campos posX/Y/Z cacheados." << std::endl;
    return true;
}

// ── Cache motionX/Z — doubles na classe Entity que NÃO são posX/Y/Z ──────────
// Usa IsSameObject para excluir os campos de posição já cacheados.
static bool CacheMotionFields(JNIEnv* env, jobject entity) {
    if (g_motXField && g_motZField) return true;
    if (!g_posXField) return false;
    auto* f = env->functions;

    // Obtém a classe que declarou posX (Entity base)
    jclass entityCls = (jclass)f->CallObjectMethod(env, g_posXField, s_getDeclClsM);
    if (!entityCls || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jobject flds = GetDeclaredFields(env, entityCls);
    f->DeleteLocalRef(env, entityCls);
    if (!flds) return false;

    jsize    n       = f->GetArrayLength(env, flds);
    jobject  cands[3] = {};
    int      found   = 0;

    for (jsize i = 0; i < n && found < 3; i++) {
        jobject field = f->GetObjectArrayElement(env, flds, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }

        if (FieldTypeName(env, field) == "double") {
            // Exclui posX, posY, posZ comparando objetos
            bool isPos = f->IsSameObject(env, field, g_posXField) ||
                         f->IsSameObject(env, field, g_posYField) ||
                         f->IsSameObject(env, field, g_posZField);
            if (!isPos) {
                cands[found++] = field;
                continue;
            }
        }
        f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, flds);

    if (found < 3) {
        for (int i = 0; i < found; i++) f->DeleteLocalRef(env, cands[i]);
        return false;
    }
    // cands[0]=motionX, cands[1]=motionY, cands[2]=motionZ
    g_motXField = f->NewGlobalRef(env, cands[0]);
    g_motZField = f->NewGlobalRef(env, cands[2]);
    for (int i = 0; i < 3; i++) f->DeleteLocalRef(env, cands[i]);
    std::cout << "[+] SDK: campos motionX/Z cacheados." << std::endl;
    return true;
}

// ── Cache rotationYaw/rotationPitch from the same class that declares posX ────
// posX is in Entity base class; rotationYaw/rotationPitch are the first 2 floats
// declared in that same class.
static bool CacheRotationFields(JNIEnv* env) {
    if (g_yawField || !g_posXField) return g_yawField != nullptr;
    auto* f = env->functions;

    // Get the class that declared posX (the Entity base class)
    jclass entityCls = (jclass)f->CallObjectMethod(env, g_posXField, s_getDeclClsM);
    if (!entityCls || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    jobject flds = GetDeclaredFields(env, entityCls);
    f->DeleteLocalRef(env, entityCls);
    if (!flds) return false;

    static const jint ACC_STATIC = 0x0008;
    jsize   n    = f->GetArrayLength(env, flds);
    jobject c[2] = {};
    int     found = 0;

    for (jsize i = 0; i < n && found < 2; i++) {
        jobject field = f->GetObjectArrayElement(env, flds, i);
        if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
        jint mods = FieldGetMods(env, field);
        if (!(mods & ACC_STATIC) && FieldTypeName(env, field) == "float")
            c[found++] = field;
        else
            f->DeleteLocalRef(env, field);
    }
    f->DeleteLocalRef(env, flds);

    if (found < 2) {
        for (int i = 0; i < found; i++) f->DeleteLocalRef(env, c[i]);
        return false;
    }
    g_yawField   = f->NewGlobalRef(env, c[0]);
    g_pitchField = f->NewGlobalRef(env, c[1]);
    for (int i = 0; i < 2; i++) f->DeleteLocalRef(env, c[i]);
    std::cout << "[+] SDK: campos yaw/pitch cacheados." << std::endl;
    return true;
}

// ── Public API ────────────────────────────────────────────────────────────────
bool Minecraft::GetNearbyPlayers(std::vector<EntityInfo>& out) {
    JNIEnv* env = Env();
    if (!env) return false;
    if (g_initFailed) { g_initDone = false; g_initFailed = false; }
    if (!InitSDK(env)) {
        g_retryCount++;
        if (g_retryCount == 1)
            std::cout << "[-] SDK: init falhou. Aguardando..." << std::endl;
        return false;
    }

    auto* f = env->functions;

    if (g_entityListIsArr) {
        jsize count = f->GetArrayLength(env, g_entityList);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
        for (jsize i = 0; i < count; i++) {
            jobject entity = f->GetObjectArrayElement(env, g_entityList, i);
            if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            if (!CacheEntityFields(env, entity)) { f->DeleteLocalRef(env, entity); continue; }
            CacheRotationFields(env);
            EntityInfo info;
            info.posX  = FieldGetDouble(env, g_posXField, entity);
            info.posY  = FieldGetDouble(env, g_posYField, entity);
            info.posZ  = FieldGetDouble(env, g_posZField, entity);
            if (g_yawField)   info.yaw   = (float)FieldGetDouble(env, g_yawField,   entity);
            if (g_pitchField) info.pitch = (float)FieldGetDouble(env, g_pitchField, entity);
            out.push_back(info);
            f->DeleteLocalRef(env, entity);
        }
    } else {
        jint count = f->CallIntMethod(env, g_entityList, s_listSizeM);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
        for (jint i = 0; i < count; i++) {
            jobject entity = f->CallObjectMethod(env, g_entityList, s_listGetM, i);
            if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
            if (!CacheEntityFields(env, entity)) { f->DeleteLocalRef(env, entity); continue; }
            CacheRotationFields(env);
            EntityInfo info;
            info.posX  = FieldGetDouble(env, g_posXField, entity);
            info.posY  = FieldGetDouble(env, g_posYField, entity);
            info.posZ  = FieldGetDouble(env, g_posZField, entity);
            if (g_yawField)   info.yaw   = (float)FieldGetDouble(env, g_yawField,   entity);
            if (g_pitchField) info.pitch = (float)FieldGetDouble(env, g_pitchField, entity);
            out.push_back(info);
            f->DeleteLocalRef(env, entity);
        }
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
    out.yaw   = tmp[0].yaw;
    out.pitch = tmp[0].pitch;
    out.fov   = 70.0f;
    out.valid = true;
    return true;
}

void Minecraft::PrintLocalPlayerName() {}

bool Minecraft::SetSneakKeyState(bool pressed) {
    JNIEnv* env = Env();
    if (!env) return false;
    if (!CacheReflectMethods(env)) return false;

    // Cache o ponteiro nativo do keyDownBuffer do LWJGL na primeira chamada.
    // org.lwjgl.input.Keyboard tem um campo static ByteBuffer (keyDownBuffer)
    // indexado pelo keycode LWJGL (KEY_LSHIFT = 42 = DIK_LSHIFT).
    static void* s_keyBuf = nullptr;
    static bool  s_tried  = false;

    if (!s_tried) {
        s_tried = true;
        auto* f = env->functions;

        jclass kbCls = f->FindClass(env, "org/lwjgl/input/Keyboard");
        if (!kbCls || f->ExceptionCheck(env)) {
            f->ExceptionClear(env);
            std::cout << "[-] SDK: org/lwjgl/input/Keyboard nao encontrado." << std::endl;
            return false;
        }

        static const jint ACC_STATIC = 0x0008;
        jobject flds = GetDeclaredFields(env, kbCls);
        if (flds) {
            jsize n = f->GetArrayLength(env, flds);
            for (jsize i = 0; i < n && !s_keyBuf; i++) {
                jobject field = f->GetObjectArrayElement(env, flds, i);
                if (!field || f->ExceptionCheck(env)) { f->ExceptionClear(env); continue; }
                jint mods = FieldGetMods(env, field);
                if (mods & ACC_STATIC) {
                    std::string t = FieldTypeName(env, field);
                    if (t == "java.nio.ByteBuffer") {
                        f->CallVoidMethod(env, field, s_setAccM, (jboolean)1);
                        if (f->ExceptionCheck(env)) f->ExceptionClear(env);
                        jobject buf = f->CallObjectMethod(env, field, s_getM, (jobject)nullptr);
                        if (buf && !f->ExceptionCheck(env)) {
                            void* addr = f->GetDirectBufferAddress(env, buf);
                            if (addr) {
                                s_keyBuf = addr;
                                std::cout << "[+] SDK: LWJGL keyDownBuffer @ " << addr << std::endl;
                            }
                            f->DeleteLocalRef(env, buf);
                        } else f->ExceptionClear(env);
                    }
                }
                f->DeleteLocalRef(env, field);
            }
            f->DeleteLocalRef(env, flds);
        }
        f->DeleteLocalRef(env, kbCls);

        if (!s_keyBuf)
            std::cout << "[-] SDK: keyDownBuffer nao encontrado no Keyboard." << std::endl;
    }

    if (!s_keyBuf) return false;

    // KEY_LSHIFT no LWJGL 2 = 42 (DIK_LSHIFT = 0x2A)
    static const int KEY_LSHIFT = 42;
    ((uint8_t*)s_keyBuf)[KEY_LSHIFT] = pressed ? 1 : 0;
    return true;
}


bool Minecraft::ApplyAntiKB() {
    JNIEnv* env = Env();
    if (!env || !g_initDone || !g_entityList) return false;
    auto* f = env->functions;

    // Obtém o jogador local — índice 0 na lista de entidades
    jobject entity = nullptr;
    if (g_entityListIsArr) {
        jsize count = f->GetArrayLength(env, g_entityList);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
        if (count == 0) return false;
        entity = f->GetObjectArrayElement(env, g_entityList, 0);
    } else {
        jint count = f->CallIntMethod(env, g_entityList, s_listSizeM);
        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }
        if (count == 0) return false;
        entity = f->CallObjectMethod(env, g_entityList, s_listGetM, (jint)0);
    }
    if (!entity || f->ExceptionCheck(env)) { f->ExceptionClear(env); return false; }

    // Cache dos campos de posição necessário antes do de motion
    CacheEntityFields(env, entity);

    // Cache e escrita dos campos de motion
    bool ok = false;
    if (CacheMotionFields(env, entity)) {
        FieldSetDouble(env, g_motXField, entity, 0.0);
        FieldSetDouble(env, g_motZField, entity, 0.0);
        ok = true;
    }

    f->DeleteLocalRef(env, entity);
    return ok;
}

} // namespace SDK
