#pragma once
#include <windows.h>
#include <stdarg.h>

#define JNICALL  __stdcall
#define JNIEXPORT __declspec(dllexport)

// Tipos primitivos JNI
typedef int            jint;
typedef long long      jlong;
typedef signed char    jbyte;
typedef unsigned char  jboolean;
typedef unsigned short jchar;
typedef short          jshort;
typedef float          jfloat;
typedef double         jdouble;
typedef jint           jsize;

// Tipos de referência JNI
typedef void* jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef void* jfieldID;
typedef void* jmethodID;

#define JNI_VERSION_1_8 0x00010008
#define JNI_OK          0
#define JNI_EDETACHED   (-2)

struct JavaVM_;
struct JNIEnv_;

// ─── JNINativeInterface_ ───────────────────────────────────────────────────
// Vtable da JNIEnv. Cada slot corresponde a um índice fixo definido pela
// especificação JNI (OpenJDK jni.h). Slots não utilizados são void*.
// Ref: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html
struct JNINativeInterface_ {
    // Índices 0–3: reservados pela JVM
    void* reserved[4];

    // 4: GetVersion   5: DefineClass
    void* _pad4_5[2];

    // 6: FindClass
    jclass  (JNICALL *FindClass)(JNIEnv*, const char*);

    // 7–14: FromReflectedMethod … ThrowNew
    void* _pad7_14[8];

    // 15: ExceptionOccurred
    jobject (JNICALL *ExceptionOccurred)(JNIEnv*);

    // 16: ExceptionDescribe
    void* _pad16;

    // 17: ExceptionClear
    void    (JNICALL *ExceptionClear)(JNIEnv*);

    // 18–30: FatalError … IsInstanceOf (13 slots)
    void* _pad18_30[13];

    // 31: GetObjectClass
    jclass  (JNICALL *GetObjectClass)(JNIEnv*, jobject);

    // 32: IsInstanceOf
    void* _pad32;

    // 33: GetMethodID
    jmethodID (JNICALL *GetMethodID)(JNIEnv*, jclass, const char*, const char*);

    // 34: CallObjectMethod (variadic — sem argumentos extras para métodos sem parâmetros)
    jobject (JNICALL *CallObjectMethod)(JNIEnv*, jobject, jmethodID, ...);

    // 35–93: CallObjectMethodV … SetDoubleField (59 slots)
    void* _pad35_93[59];

    // 94: GetFieldID
    jfieldID (JNICALL *GetFieldID)(JNIEnv*, jclass, const char*, const char*);

    // 95: GetObjectField
    jobject  (JNICALL *GetObjectField)(JNIEnv*, jobject, jfieldID);

    // 96–112: GetBooleanField … SetDoubleField (17 slots)
    void* _pad96_112[17];

    // 113: GetStaticMethodID
    jmethodID (JNICALL *GetStaticMethodID)(JNIEnv*, jclass, const char*, const char*);

    // 114: CallStaticObjectMethod (variadic)
    jobject   (JNICALL *CallStaticObjectMethod)(JNIEnv*, jclass, jmethodID, ...);

    // 115–168: CallStaticObjectMethodV … GetStringUTFLength (54 slots)
    void* _pad115_168[54];

    // 169: GetStringUTFChars
    const char* (JNICALL *GetStringUTFChars)(JNIEnv*, jstring, jboolean*);

    // 170: ReleaseStringUTFChars
    void        (JNICALL *ReleaseStringUTFChars)(JNIEnv*, jstring, const char*);

    // 171–227: GetArrayLength … DeleteWeakGlobalRef (57 slots)
    void* _pad171_227[57];

    // 228: ExceptionCheck
    jboolean (JNICALL *ExceptionCheck)(JNIEnv*);
};

// ─── JNIEnv_ ──────────────────────────────────────────────────────────────
struct JNIEnv_ {
    const JNINativeInterface_* functions;
};

typedef struct JNIEnv_ JNIEnv;

// ─── JavaVM vtable ────────────────────────────────────────────────────────
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
