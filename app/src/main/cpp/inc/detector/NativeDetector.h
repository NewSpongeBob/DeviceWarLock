#ifndef WARLOCK_NATIVEDETECTOR_H
#define WARLOCK_NATIVEDETECTOR_H

#include <jni.h>
#include <string>
#include <thread>
#include <atomic>
#include <unistd.h>

class NativeDetector {
private:
    static NativeDetector* instance;
    std::thread* detectThread;
    std::atomic<bool> isRunning;
    jobject globalCallback;
    JavaVM* javaVM;

    void detectLoop();
    JNIEnv* getEnv();
    void reportWarning(JNIEnv* env, const std::string& type, const std::string& level, const std::string& detail);

public:
    NativeDetector();
    ~NativeDetector();

    static NativeDetector* getInstance();
    void startDetect(JNIEnv* env, jobject callback);
    void stopDetect();
};

#endif // WARLOCK_NATIVEDETECTOR_H