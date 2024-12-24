#ifndef WARLOCK_SYSTEMUTILS_H
#define WARLOCK_SYSTEMUTILS_H

#include <string>
#include <jni.h>

namespace MiscUtil {
    class SystemUtils {
    public:
        static int getSDKLevel();
        static void init(JNIEnv* env);

    private:
        static int sdkLevel;
        static bool isInitialized;
    };
}

#endif // WARLOCK_SYSTEMUTILS_H