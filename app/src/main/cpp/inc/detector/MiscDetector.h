#ifndef WARLOCK_MISCDETECTOR_H
#define WARLOCK_MISCDETECTOR_H

#include <jni.h>
#include <string>
#include "IDetector.h"
#include "../../inc/utils/DetectorUtils.h"
#include "../../inc/utils/LogUtils.h"
#include "../../inc/utils/SyscallUtils.h"
#include "../utils/StringUtils.h"
#include "../utils/allheader.h"
#include <linux/prctl.h>
#include <sys/prctl.h>
class MiscDetector : public IDetector {
private:
    static const std::string CHECK_MISC;

    JavaVM* javaVM;
    jobject globalCallback;
    volatile bool isRunning;
    pthread_t threadId;

    // 检测状态标志
    bool isPathCheckReported;
    bool isMapsCheckReported;

    // 线程相关
    static void* threadFunction(void* arg);
    void cleanup();
    void resetDetectionState();

    // 具体检测方法
    void detectSomething(JNIEnv* env);  // 示例方法，可以根据需要添加更多
    void detectPathExistence(JNIEnv* env);  // 添加路径检测方法
    void detectTmpInMaps(JNIEnv* env);

public:
    MiscDetector();
    ~MiscDetector() override;

    void detect(JNIEnv* env, jobject callback) override;
    void stop();
};

#endif