#pragma once
#include "jni_stub.hpp"
#include <cstring>

namespace JNIUtils {
    typedef jint (JNICALL *tJNI_GetCreatedJavaVMs)(JavaVM**, jsize, jsize*);

    inline JNIEnv* GetJNIEnv() {
        HMODULE hJvm = GetModuleHandleA("jvm.dll");
        if (!hJvm) return nullptr;

        auto pJNI_GetCreatedJavaVMs = (tJNI_GetCreatedJavaVMs)GetProcAddress(hJvm, "JNI_GetCreatedJavaVMs");
        if (!pJNI_GetCreatedJavaVMs) return nullptr;

        JavaVM* vm = nullptr;
        jsize nVms = 0;
        if (pJNI_GetCreatedJavaVMs(&vm, 1, &nVms) != JNI_OK || nVms == 0) return nullptr;

        JNIEnv* env = nullptr;
        if (vm->functions->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
            vm->functions->AttachCurrentThread(vm, (void**)&env, nullptr);
        }

        return env;
    }

    // Converte slash-notation (JNI) para dot-notation (loadClass)
    static inline void SlashToDot(const char* slashName, char* dotted, int maxLen) {
        int i = 0;
        for (; slashName[i] && i < maxLen - 1; i++)
            dotted[i] = (slashName[i] == '/') ? '.' : slashName[i];
        dotted[i] = '\0';
    }

    // Tenta loadClass(name) em um classloader especifico.
    // Retorna local ref ou nullptr (sem deixar excecao pendente).
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

    // Itera todos os classloaders de todas as threads do JVM ate achar a classe.
    // Necessario porque a thread do hook (SwapBuffers) pode nao ter o classloader
    // do Minecraft como context classloader.
    // Retorna local ref — use NewGlobalRef se precisar guardar entre frames.
    inline jclass FindMinecraftClass(JNIEnv* env, const char* slashName) {
        auto* f = env->functions;

        char dotted[256];
        SlashToDot(slashName, dotted, 256);

        jstring nameStr = f->NewStringUTF(env, dotted);
        if (!nameStr) return nullptr;

        jclass threadCls = f->FindClass(env, "java/lang/Thread");
        if (!threadCls) { f->ExceptionClear(env); f->DeleteLocalRef(env, nameStr); return nullptr; }

        // Thread.getAllStackTraces() -> Map<Thread, StackTraceElement[]>
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
}
