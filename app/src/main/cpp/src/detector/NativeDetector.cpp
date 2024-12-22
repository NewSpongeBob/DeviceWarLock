#include "../../inc/detector/NativeDetector.h"
#include <android/log.h>

#define TAG "NativeDetector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

NativeDetector* NativeDetector::instance = nullptr;

NativeDetector::NativeDetector() : detectThread(nullptr), isRunning(false), globalCallback(nullptr), javaVM(nullptr) {}

NativeDetector::~NativeDetector() {
    stopDetect();
}

NativeDetector* NativeDetector::getInstance() {
    if (instance == nullptr) {
        instance = new NativeDetector();
    }
    return instance;
}

void NativeDetector::startDetect(JNIEnv* env, jobject callback) {
    if (isRunning) {
        return;
    }

    // 保存JavaVM用于后续获取JNIEnv
    env->GetJavaVM(&javaVM);

    // 创建全局回调引用
    globalCallback = env->NewGlobalRef(callback);

    isRunning = true;
    detectThread = new std::thread(&NativeDetector::detectLoop, this);
}

void NativeDetector::stopDetect() {
    if (!isRunning) {
        return;
    }

    isRunning = false;
    if (detectThread && detectThread->joinable()) {
        detectThread->join();
        delete detectThread;
        detectThread = nullptr;
    }

    // 清理全局引用
    if (globalCallback) {
        JNIEnv* env = getEnv();
        if (env) {
            env->DeleteGlobalRef(globalCallback);
            globalCallback = nullptr;
        }
    }
}

void NativeDetector::detectLoop() {
    while (isRunning) {
        JNIEnv* env = getEnv();
        if (!env) {
            continue;
        }

        // TODO: 在这里实现具体的检测逻辑
        // 例如：检测maps文件、检测挂载点等

        // 示例：发现异常时报告
        // reportWarning(env, "nativeCheck", "high", "发现异常: xxx");

        // 休眠一段时间再继续检测
        sleep(5);
    }
}

JNIEnv* NativeDetector::getEnv() {
    if (!javaVM) {
        return nullptr;
    }

    JNIEnv* env = nullptr;
    int status = javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (status == JNI_EDETACHED) {
        status = javaVM->AttachCurrentThread(&env, nullptr);
        if (status < 0) {
            return nullptr;
        }
    }

    return env;
}

void NativeDetector::reportWarning(JNIEnv* env, const std::string& type, const std::string& level, const std::string& detail) {
    // 查找回调类和方法
    jclass callbackClass = env->GetObjectClass(globalCallback);
    jmethodID reportMethod = env->GetMethodID(callbackClass, "onDetectWarning",
                                              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    if (reportMethod) {
        jstring jType = env->NewStringUTF(type.c_str());
        jstring jLevel = env->NewStringUTF(level.c_str());
        jstring jDetail = env->NewStringUTF(detail.c_str());

        env->CallVoidMethod(globalCallback, reportMethod, jType, jLevel, jDetail);

        env->DeleteLocalRef(jType);
        env->DeleteLocalRef(jLevel);
        env->DeleteLocalRef(jDetail);
    }
}