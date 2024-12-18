//
// Created by 17267 on 2024-12-17.
//
#include <jni.h>
#include "detector/SandboxDetector.h"

extern "C" {
JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_service_WarLockServer_nativeCheckSandbox(JNIEnv *env, jobject thiz) {
    SandboxDetector::checkSandbox(env, thiz);
    SandboxDetector::checkProcessByPs(env, thiz);

}
}