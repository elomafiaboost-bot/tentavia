#pragma once
#include "jni_stub.hpp"
#include "jvmti_stub.hpp"
#include <cstring>
#include <iostream>
#include <cstdio>

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

    // ── Diagnóstico: despeja todas as classes em arquivo (só executa uma vez) ──
    static inline void DumpLoadedClasses(JVMTIEnv* jvmti, JNIEnv* jni, jint count, jclass* classes) {
        static bool done = false;
        if (done) return;
        done = true;

        auto* t = jvmti->functions;
        auto* f = jni->functions;

        FILE* fp = fopen("tentavia_classes.txt", "w");
        if (!fp) {
            std::cout << "[-] Nao foi possivel criar tentavia_classes.txt" << std::endl;
            return;
        }
        fprintf(fp, "Total de classes: %d\n\n", (int)count);

        int mcCount = 0;
        for (jint k = 0; k < count; k++) {
            char* sig = nullptr;
            if (t->GetClassSignature(jvmti, classes[k], &sig, nullptr) == JVMTI_ERROR_NONE && sig) {
                fprintf(fp, "%s\n", sig);
                // Conta classes com "minecraft" no nome (nao obfuscadas)
                if (strstr(sig, "minecraft") || strstr(sig, "Minecraft"))
                    mcCount++;
                t->Deallocate(jvmti, (unsigned char*)sig);
            }
        }
        fclose(fp);

        std::cout << "[*] JVMTI: " << count << " classes despejadas em tentavia_classes.txt"
                  << " (" << mcCount << " com 'minecraft' no nome)" << std::endl;
        std::cout << "[*] Abra tentavia_classes.txt para ver os nomes reais das classes." << std::endl;
        if (mcCount == 0)
            std::cout << "[!] Nenhuma classe com 'minecraft' encontrada — "
                         "provavelmente Minecraft vanilla com classes obfuscadas." << std::endl;
    }

    // ── JVMTI: busca classe por nome exato ───────────────────────────────────
    inline jclass FindClassViaJVMTI(JNIEnv* jni, JVMTIEnv* jvmti, const char* slashName) {
        auto* t = jvmti->functions;
        auto* f = jni->functions;

        char sig[260] = { 'L' };
        int i = 1;
        for (int j = 0; slashName[j] && i < 257; j++, i++)
            sig[i] = slashName[j];
        sig[i++] = ';';
        sig[i]   = '\0';

        jint    count   = 0;
        jclass* classes = nullptr;
        jvmtiError err  = t->GetLoadedClasses(jvmti, &count, &classes);

        if (err != JVMTI_ERROR_NONE) {
            static bool logged = false;
            if (!logged) {
                std::cout << "[-] JVMTI GetLoadedClasses erro codigo " << (int)err
                          << " (112=wrong_phase, 100=null_ptr, 115=unattached_thread)" << std::endl;
                logged = true;
            }
            return nullptr;
        }

        if (!classes || count == 0) {
            std::cout << "[-] JVMTI retornou 0 classes — JVM pode nao estar no live phase." << std::endl;
            return nullptr;
        }

        // Despeja uma vez para diagnóstico
        DumpLoadedClasses(jvmti, jni, count, classes);

        jclass result = nullptr;
        for (jint k = 0; k < count; k++) {
            if (!result) {
                char* classSig   = nullptr;
                char* genericSig = nullptr;
                if (t->GetClassSignature(jvmti, classes[k], &classSig, &genericSig) == JVMTI_ERROR_NONE) {
                    if (classSig && strcmp(classSig, sig) == 0)
                        result = classes[k];
                    if (classSig)   t->Deallocate(jvmti, (unsigned char*)classSig);
                    if (genericSig) t->Deallocate(jvmti, (unsigned char*)genericSig);
                }
            }
            if (!result || classes[k] != result)
                f->DeleteLocalRef(jni, classes[k]);
        }
        t->Deallocate(jvmti, (unsigned char*)classes);

        if (!result) {
            static bool notFoundLogged = false;
            if (!notFoundLogged) {
                notFoundLogged = true;
                std::cout << "[-] Classe " << sig << " nao encontrada entre " << (int)count
                          << " classes. Provavelmente obfuscada. Veja tentavia_classes.txt." << std::endl;
            }
        }
        return result;
    }

    // ── Classloader fallback ──────────────────────────────────────────────────
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
    inline jclass FindMinecraftClass(JNIEnv* env, const char* slashName) {
        static bool jvmtiExhausted = false;

        if (!jvmtiExhausted) {
            JVMTIEnv* jvmti = GetJVMTIEnv();
            if (jvmti) {
                jclass cls = FindClassViaJVMTI(env, jvmti, slashName);
                if (cls) return cls;
                // Se chegou aqui, JVMTI rodou mas nao achou — classe obfuscada.
                // Inutíl continuar tentando JVMTI com o mesmo nome.
                jvmtiExhausted = true;
            } else {
                static bool warnedJVMTI = false;
                if (!warnedJVMTI) {
                    std::cout << "[-] JVMTI nao disponivel." << std::endl;
                    warnedJVMTI = true;
                }
            }
        }

        return FindClassViaThreadLoaders(env, slashName);
    }
}
