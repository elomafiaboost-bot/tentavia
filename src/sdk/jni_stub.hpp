#pragma once
#include <windows.h>

// JNI Stub - Verso Consertada
#define JNICALL __stdcall
#define JNIEXPORT __declspec(dllexport)

typedef int jint;
typedef long long jlong;
typedef signed char jbyte;
typedef unsigned char jboolean;
typedef unsigned short jchar;
typedef short jshort;
typedef float jfloat;
typedef double jdouble;
typedef jint jsize;

typedef void* jobject;
typedef jobject jclass;
typedef void* jfieldID;
typedef void* jmethodID;

#define JNI_VERSION_1_8 0x00010008
#define JNI_OK          0
#define JNI_EDETACHED   (-2)

struct JavaVM_;
struct JNIEnv_;

// Vtable do JavaVM
struct JNIInvokeInterface_ {
    void* reserved0;
    void* reserved1;
    void* reserved2;
    jint (JNICALL *DestroyJavaVM)(struct JavaVM_* vm);
    jint (JNICALL *AttachCurrentThread)(struct JavaVM_* vm, void** penv, void* args);
    jint (JNICALL *DetachCurrentThread)(struct JavaVM_* vm);
    jint (JNICALL *GetEnv)(struct JavaVM_* vm, void** penv, jint version);
};

struct JavaVM_ {
    const struct JNIInvokeInterface_* functions;
};

typedef struct JavaVM_ JavaVM;
typedef struct JNIEnv_ JNIEnv;
