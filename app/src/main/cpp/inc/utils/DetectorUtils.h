#ifndef WARLOCK_DETECTORUTILS_H
#define WARLOCK_DETECTORUTILS_H

#include <string>
#include <jni.h>

class DetectorUtils {
public:
    static void reportWarning(JNIEnv* env, jobject callback,
                              const std::string& type, const std::string& level, const std::string& detail);

    // 检测类型常量
    static const std::string CHECK_MOUNT;
    static const std::string CHECK_MAPS;

    // 风险等级常量
    static const std::string LEVEL_HIGH;
    static const std::string LEVEL_MEDIUM;
    static const std::string LEVEL_LOW;
};

#endif // WARLOCK_DETECTORUTILS_H