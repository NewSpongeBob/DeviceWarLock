#include "../inc/warlockCore.h"


extern "C" {

// 实现 popen 方法
JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_popen(JNIEnv *env, jobject /* obj */, jstring command) {
    const char *cmd = env->GetStringUTFChars(command, nullptr);
    char buffer[128];
    FILE *pipe = popen(cmd, "r");
    if (!pipe) {
        env->ReleaseStringUTFChars(command, cmd);
        return env->NewStringUTF("Error opening pipe");
    }

    // 读取命令输出
    std::string result;
    while (fgets(buffer, sizeof(buffer), pipe) != NULL) {
        result += buffer;
    }

    pclose(pipe);
    env->ReleaseStringUTFChars(command, cmd);
    return env->NewStringUTF(result.c_str());
}

// 实现 open 方法
JNIEXPORT jint JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_open(JNIEnv *env, jobject /* obj */, jstring path, jint flags) {
    const char *filePath = env->GetStringUTFChars(path, nullptr);
    int fd = open(filePath, flags);
    env->ReleaseStringUTFChars(path, filePath);
    return fd;
}
JNIEXPORT void JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_startCollect(JNIEnv *env, jclass /* clazz */, jobject callback) {
    NativeCollector::getInstance()->startCollect(env, callback);
}

JNIEXPORT jstring JNICALL
Java_com_xiaoc_warlock_Util_NativeEngine_getCollectedInfo(JNIEnv *env, jclass /* clazz */) {
    std::string jsonStr = NativeCollector::getInstance()->getCollectedInfo();
    return env->NewStringUTF(jsonStr.c_str());
}


}