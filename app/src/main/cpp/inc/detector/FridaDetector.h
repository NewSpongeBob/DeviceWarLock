#ifndef WARLOCK_FRIDADETECTOR_H
#define WARLOCK_FRIDADETECTOR_H

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/syscall.h>
#include "IDetector.h"
#include "../../inc/utils/DetectorUtils.h"
#include "../../inc/utils/LogUtils.h"
#include <sys/prctl.h>
#include <linux/sched.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <linux/in.h>
#include <sys/endian.h>
#include <set>
#include <sys/wait.h>
#include "../../inc/utils/SyscallUtils.h"
#include <vector>
#include "../utils/allheader.h"
#include <bits/glibc-syscalls.h>
class FridaDetector: public IDetector {
private:
    static const std::string CHECK_FRIDA;

    volatile bool isRunning;
    pthread_t threadId;
    JavaVM* javaVM;
    jobject globalCallback;

    // 添加检测状态
    std::set<int> detectedPorts;
    bool isAbnormalStateReported;
    void cleanup();
    static void* threadFunction(void* arg);
    void detectFridaPorts(JNIEnv* env);
    void resetDetectionState();
    void detectFridaFile(JNIEnv* env);
    void detectFridaInMaps(JNIEnv* env);
    bool isMapsAbnormalReported;
public:
    FridaDetector();
    ~FridaDetector() override;

    void detect(JNIEnv* env, jobject callback) override;
    void stop();
};

#endif