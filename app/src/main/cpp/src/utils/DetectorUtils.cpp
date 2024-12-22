#include "../../inc/utils//DetectorUtils.h"
#include "../../inc/utils/LogUtils.h"

const std::string DetectorUtils::CHECK_MOUNT = "mount_check";
const std::string DetectorUtils::CHECK_MAPS = "maps_check";

const std::string DetectorUtils::LEVEL_HIGH = "high";
const std::string DetectorUtils::LEVEL_MEDIUM = "medium";
const std::string DetectorUtils::LEVEL_LOW = "low";

void DetectorUtils::reportWarning(JNIEnv* env, jobject callback,
                                  const std::string& type, const std::string& level, const std::string& detail) {

    if (!callback) {
        LOGE("Callback is null");
        return;
    }

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID reportMethod = env->GetMethodID(callbackClass, "onDetectWarning",
                                              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    if (reportMethod) {
        jstring jType = env->NewStringUTF(type.c_str());
        jstring jLevel = env->NewStringUTF(level.c_str());
        jstring jDetail = env->NewStringUTF(detail.c_str());

        env->CallVoidMethod(callback, reportMethod, jType, jLevel, jDetail);

        env->DeleteLocalRef(jType);
        env->DeleteLocalRef(jLevel);
        env->DeleteLocalRef(jDetail);
    }
}