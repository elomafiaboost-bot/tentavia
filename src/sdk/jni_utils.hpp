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

    // FindClass usa o bootstrap classloader e nao enxerga classes do Minecraft.
    // Esta funcao usa Thread.currentThread().getContextClassLoader().loadClass()
    // que tem acesso ao classloader customizado do Minecraft.
    // Retorna uma local ref — chame NewGlobalRef se precisar guardar entre frames.
    // slashName: formato JNI com barras, ex: "net/minecraft/client/Minecraft"
    inline jclass FindMinecraftClass(JNIEnv* env, const char* slashName) {
        auto* f = env->functions;

        jclass threadCls = f->FindClass(env, "java/lang/Thread");
        if (!threadCls) { f->ExceptionClear(env); return nullptr; }

        jmethodID currentThreadM = f->GetStaticMethodID(env, threadCls,
            "currentThread", "()Ljava/lang/Thread;");
        if (!currentThreadM) { f->ExceptionClear(env); return nullptr; }

        jobject thread = f->CallStaticObjectMethod(env, threadCls, currentThreadM);
        if (!thread || f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }

        jmethodID getLoaderM = f->GetMethodID(env, threadCls,
            "getContextClassLoader", "()Ljava/lang/ClassLoader;");
        if (!getLoaderM) { f->ExceptionClear(env); f->DeleteLocalRef(env, thread); return nullptr; }

        jobject loader = f->CallObjectMethod(env, thread, getLoaderM);
        f->DeleteLocalRef(env, thread);
        if (!loader || f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }

        // JNI usa barras; loadClass usa pontos
        char dotted[256];
        int i = 0;
        for (; slashName[i] && i < 255; i++)
            dotted[i] = (slashName[i] == '/') ? '.' : slashName[i];
        dotted[i] = '\0';

        jclass loaderCls = f->GetObjectClass(env, loader);
        jmethodID loadClassM = f->GetMethodID(env, loaderCls,
            "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        if (!loadClassM) {
            f->ExceptionClear(env); f->DeleteLocalRef(env, loader); return nullptr;
        }

        jstring nameStr = f->NewStringUTF(env, dotted);
        auto result = (jclass)f->CallObjectMethod(env, loader, loadClassM, nameStr);
        f->DeleteLocalRef(env, nameStr);
        f->DeleteLocalRef(env, loader);

        if (f->ExceptionCheck(env)) { f->ExceptionClear(env); return nullptr; }
        return result;
    }
}
