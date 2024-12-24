#include "../../inc/utils/MiscUtil.h"
#include "../../inc/utils/LogUtils.h"

namespace MiscUtil {

    int SystemUtils::sdkLevel = 0;
    bool SystemUtils::isInitialized = false;

    void SystemUtils::init(JNIEnv* env) {
        if (isInitialized) {
            return;
        }

        jclass versionClass = env->FindClass("android/os/Build$VERSION");
        if (versionClass != nullptr) {
            jfieldID sdkIntFieldID = env->GetStaticFieldID(versionClass, "SDK_INT", "I");
            if (sdkIntFieldID != nullptr) {
                sdkLevel = env->GetStaticIntField(versionClass, sdkIntFieldID);
                isInitialized = true;
            }
        }

        if (!isInitialized) {
            LOGE("Failed to get SDK level");
            sdkLevel = 0;
        }
    }

    int SystemUtils::getSDKLevel() {
        return sdkLevel;
    }
}