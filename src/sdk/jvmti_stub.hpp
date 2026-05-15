#pragma once
#include "jni_stub.hpp"

// Minimal JVMTI vtable stub — apenas os slots necessários.
// Números de slot conforme OpenJDK jvmti.h (Java 8).

typedef int jvmtiError;
#define JVMTI_ERROR_NONE    0
#define JVMTI_VERSION_1_2   0x30010200

struct JVMTIInterface_;

struct JVMTIEnv_ {
    const JVMTIInterface_* functions;
};
typedef JVMTIEnv_ JVMTIEnv;

struct JVMTIInterface_ {
    void* _pad0_45[46];   // slots 0-45

    // 46: Deallocate — libera buffers alocados pelo JVMTI
    jvmtiError (JNICALL *Deallocate)(JVMTIEnv* env, unsigned char* mem);

    // 47: GetClassSignature — retorna "Ljava/lang/String;" style
    jvmtiError (JNICALL *GetClassSignature)(JVMTIEnv* env, jclass klass,
                                            char** sigPtr, char** genericPtr);

    void* _pad48_76[29];  // slots 48-76

    // 77: GetLoadedClasses — todas as classes carregadas, independente de classloader
    jvmtiError (JNICALL *GetLoadedClasses)(JVMTIEnv* env,
                                           jint* countPtr, jclass** classesPtr);
};
