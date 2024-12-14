// warlockCore.h

#ifndef WARLOCK_WARLOCKCORE_H
#define WARLOCK_WARLOCKCORE_H

#include <jni.h>
#include <string>
#include <cstring>
#include <stdio.h>
#include <fcntl.h>
#include "../inc/collector/NativeCollector.h"
#ifdef __cplusplus
extern "C" {
#endif

// ... 其他已有的声明 ...

JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_startCollect(JNIEnv *env, jclass clazz, jobject callback);

JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_getCollectedInfo(JNIEnv *env, jclass clazz);

#ifdef __cplusplus
}
#endif

#endif // WARLOCK_WARLOCKCORE_H