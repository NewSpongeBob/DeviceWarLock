//
// Created by 17267 on 2024/12/10.
//

#ifndef WARLOCK_WARLOCKCORE_H
#define WARLOCK_WARLOCKCORE_H


#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_popen(JNIEnv *env, jobject obj, jstring command);

JNIEXPORT jint JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_open(JNIEnv *env, jobject obj, jstring path, jint flags);

#ifdef __cplusplus
}
#endif

#endif // WARLOCK_CORE_H