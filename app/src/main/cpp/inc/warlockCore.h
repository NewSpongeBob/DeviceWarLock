// warlockCore.h

#ifndef WARLOCK_WARLOCKCORE_H
#define WARLOCK_WARLOCKCORE_H

#include <jni.h>
#include <string>
#include <cstring>
#include <stdio.h>
#include <fcntl.h>
#include "../inc/collector/NativeCollector.h"
#include "../inc/detector/NativeDetector.h"
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_popen(JNIEnv *env, jobject /* obj */, jstring command);
JNIEXPORT jint JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_open(JNIEnv *env, jobject /* obj */, jstring path, jint flags);
JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_startCollect(JNIEnv *env, jclass clazz, jobject callback);

JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_getCollectedInfo(JNIEnv *env, jclass clazz);
JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_startDetect(JNIEnv *env, jclass /* clazz */, jobject callback);
JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_stopDetect(JNIEnv *env, jclass /* clazz */);
#ifdef __cplusplus
}
#endif

#endif // WARLOCK_WARLOCKCORE_H