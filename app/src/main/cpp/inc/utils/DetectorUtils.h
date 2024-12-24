#ifndef WARLOCK_DETECTORUTILS_H
#define WARLOCK_DETECTORUTILS_H

#include <string>
#include <jni.h>

class DetectorUtils {
public:
    static constexpr const char* LEVEL_HIGH = "high";
    static constexpr const char* LEVEL_MEDIUM = "medium";
    static constexpr const char* LEVEL_LOW = "low";

    static void reportWarning(JNIEnv* env, jobject callback,
                              const std::string& type,
                              const std::string& level,
                              const std::string& detail);
};

#endif