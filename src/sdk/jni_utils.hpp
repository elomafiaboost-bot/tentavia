#pragma once
#include "jni_stub.hpp"

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
}
