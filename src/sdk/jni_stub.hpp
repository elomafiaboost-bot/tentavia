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

typedef struct JNIEnv_ JNIEnv;
typedef struct JavaVM_ JavaVM;

// ─── JNINativeInterface_ ───────────────────────────────────────────────────
// Vtable da JNIEnv mapeada slot a slot conforme a spec JNI (OpenJDK jni.h).
// Índices de cada função estão nos comentários.
struct JNINativeInterface_ {
    void* reserved[4];              // 0-3

    void* _pad4_5[2];               // 4-5  GetVersion, DefineClass

    // 6
    jclass   (JNICALL *FindClass)(JNIEnv*, const char*);

    void* _pad7_14[8];              // 7-14

    // 15
    jobject  (JNICALL *ExceptionOccurred)(JNIEnv*);

    void* _pad16;                   // 16 ExceptionDescribe

    // 17
    void     (JNICALL *ExceptionClear)(JNIEnv*);

    void* _pad18_20[3];             // 18-20 FatalError, PushLocalFrame, PopLocalFrame

    // 21
    jobject  (JNICALL *NewGlobalRef)(JNIEnv*, jobject);

    void* _pad22;                   // 22 DeleteGlobalRef

    // 23
    void     (JNICALL *DeleteLocalRef)(JNIEnv*, jobject);

    // 24
    jboolean (JNICALL *IsSameObject)(JNIEnv*, jobject, jobject);

    void* _pad25_30[6];             // 25-30 NewLocalRef .. NewObjectA

    // 31
    jclass   (JNICALL *GetObjectClass)(JNIEnv*, jobject);

    void* _pad32;                   // 32 IsInstanceOf

    // 33
    jmethodID (JNICALL *GetMethodID)(JNIEnv*, jclass, const char*, const char*);

    // 34 – variadic; cobre todos os tipos de argumento via ...
    jobject  (JNICALL *CallObjectMethod)(JNIEnv*, jobject, jmethodID, ...);

    void* _pad35_36[2];             // 35-36 CallObjectMethodV, CallObjectMethodA

    // 37
    jboolean (JNICALL *CallBooleanMethod)(JNIEnv*, jobject, jmethodID, ...);

    void* _pad38_48[11];            // 38-48 CallBoolean[V/A] .. CallShort[V/A]

    // 49
    jint     (JNICALL *CallIntMethod)(JNIEnv*, jobject, jmethodID, ...);

    void* _pad50_93[44];            // 50-93 CallInt[V/A] .. CallNonvirtualVoidA

    // 94
    jfieldID (JNICALL *GetFieldID)(JNIEnv*, jclass, const char*, const char*);

    // 95
    jobject  (JNICALL *GetObjectField)(JNIEnv*, jobject, jfieldID);

    void* _pad96_101[6];            // 96-101 GetBooleanField .. GetLongField

    // 102
    jfloat   (JNICALL *GetFloatField)(JNIEnv*, jobject, jfieldID);

    // 103
    jdouble  (JNICALL *GetDoubleField)(JNIEnv*, jobject, jfieldID);

    void* _pad104_112[9];           // 104-112 SetObjectField .. SetDoubleField

    // 113
    jmethodID (JNICALL *GetStaticMethodID)(JNIEnv*, jclass, const char*, const char*);

    // 114 – variadic
    jobject  (JNICALL *CallStaticObjectMethod)(JNIEnv*, jclass, jmethodID, ...);

    void* _pad115_166[52];          // 115-166

    // 167
    jstring     (JNICALL *NewStringUTF)(JNIEnv*, const char*);

    void* _pad168;                  // 168 GetStringUTFLength

    // 169
    const char* (JNICALL *GetStringUTFChars)(JNIEnv*, jstring, jboolean*);

    // 170
    void        (JNICALL *ReleaseStringUTFChars)(JNIEnv*, jstring, const char*);

    void* _pad171_227[57];          // 171-227

    // 228
    jboolean (JNICALL *ExceptionCheck)(JNIEnv*);
};

// ─── JNIEnv_ ──────────────────────────────────────────────────────────────
struct JNIEnv_ {
    const JNINativeInterface_* functions;
};

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
