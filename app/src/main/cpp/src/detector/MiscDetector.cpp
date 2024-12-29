#include "../../inc/detector/MiscDetector.h"
#include <errno.h>
#include <string.h>

const std::string MiscDetector::CHECK_MISC = "misc_check";

MiscDetector::MiscDetector()
        : isRunning(false), javaVM(nullptr), globalCallback(nullptr),
          isPathCheckReported(false), isMapsCheckReported(false) {
}

MiscDetector::~MiscDetector() {
    stop();
}

void MiscDetector::detect(JNIEnv* env, jobject callback) {
    try {
        if (isRunning) {
            LOGD("Detection already running");
            return;
        }

        if (!env || !callback) {
            LOGE("Invalid parameters: env or callback is null");
            return;
        }

        // 获取JavaVM
        if (env->GetJavaVM(&javaVM) != JNI_OK) {
            LOGE("Failed to get JavaVM");
            return;
        }

        // 创建全局引用
        globalCallback = env->NewGlobalRef(callback);
        if (!globalCallback) {
            LOGE("Failed to create global reference");
            return;
        }

        // 创建检测线程
        isRunning = true;
        int result = pthread_create(&threadId, nullptr, threadFunction, this);
        if (result != 0) {
            LOGE("Failed to create thread: %s", strerror(result));
            cleanup();
            return;
        }
    } catch (const std::exception& e) {
        LOGE("Exception in detect: %s", e.what());
        cleanup();
    } catch (...) {
        LOGE("Unknown exception in detect");
        cleanup();
    }
}

void* MiscDetector::threadFunction(void* arg) {
    MiscDetector* detector = static_cast<MiscDetector*>(arg);

    prctl(PR_SET_NAME, "warlockmsc");

    JNIEnv* env = nullptr;
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6;
    args.name = "MiscDetector";
    args.group = nullptr;

    if (detector->javaVM->AttachCurrentThread(&env, &args) != JNI_OK || !env) {
        LOGE("Failed to attach thread to JVM");
        return nullptr;
    }

    // 主检测循环
    while (detector->isRunning) {
        try {
            // 只在未报告时执行检测
            if (!detector->isPathCheckReported) {
                detector->detectPathExistence(env);
            }
            if (!detector->isMapsCheckReported) {
                detector->detectTmpInMaps(env);
            }

            // 如果两个检测都已报告，可以考虑停止线程
            if (detector->isPathCheckReported && detector->isMapsCheckReported) {
                LOGD("All checks completed, stopping thread");
                break;
            }
        } catch (const std::exception& e) {
            LOGE("Exception in detection loop: %s", e.what());
        } catch (...) {
            LOGE("Unknown exception in detection loop");
        }
        usleep(3000 * 1000);  // 3秒延迟
    }

    detector->javaVM->DetachCurrentThread();
    return nullptr;
}

void MiscDetector::detectPathExistence(JNIEnv* env) {
    try {
        // 获取应用的files目录路径
        jclass contextClass = env->FindClass("android/content/Context");
        jmethodID getFilesDir = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
        jobject fileObj = env->CallObjectMethod(globalCallback, getFilesDir);

        jclass fileClass = env->FindClass("java/io/File");
        jmethodID getAbsolutePath = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
        jstring pathStr = (jstring)env->CallObjectMethod(fileObj, getAbsolutePath);

        const char* path = env->GetStringUTFChars(pathStr, nullptr);

        // 使用SyscallUtils进行检测
#if defined(__aarch64__)
        const long SYSCALL_FACCESSAT = 48;  // ARM64
#elif defined(__arm__)
        const long SYSCALL_FACCESSAT = 334; // ARM
        #elif defined(__x86_64__)
        const long SYSCALL_FACCESSAT = 269; // x86_64
        #else
        #error "Unsupported architecture"
#endif

        int result = utils::SyscallUtils::syscall(SYSCALL_FACCESSAT, AT_FDCWD, (long)path, F_OK, 0);

        env->ReleaseStringUTFChars(pathStr, path);

        if (result < 0) {
            // 可能运行在虚拟机中
            std::string detail = "Application might be running in a virtual machine";
            DetectorUtils::reportWarning(env, globalCallback,
                                         "checkVirtual/sandbox_native",
                                         DetectorUtils::LEVEL_HIGH,
                                         detail);
        }

    } catch (const std::exception& e) {
        LOGE("Exception in detectPathExistence: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in detectPathExistence");
    }
}
void MiscDetector::detectTmpInMaps(JNIEnv* env) {
    try {
#if defined(__aarch64__)
        const long SYSCALL_OPENAT = 56;
        const long SYSCALL_READ = 63;
        const long SYSCALL_CLOSE = 57;
#elif defined(__arm__)
        const long SYSCALL_OPENAT = 322;
        const long SYSCALL_READ = 3;
        const long SYSCALL_CLOSE = 6;
        #elif defined(__x86_64__)
        const long SYSCALL_OPENAT = 257;
        const long SYSCALL_READ = 0;
        const long SYSCALL_CLOSE = 3;
#endif

        int fd = utils::SyscallUtils::syscall(SYSCALL_OPENAT, AT_FDCWD, (long)"/proc/self/maps", O_RDONLY);
        if (fd < 0) {
            LOGE("Failed to open maps: %s", strerror(errno));
            return;
        }

        char buffer[4096];
        std::string content;
        std::map<std::string, std::vector<std::string>> hookFeatures;

        // Hook框架特征
        const std::vector<std::pair<std::string, std::string>> signatures = {
                {"Frida", "frida"},
                {"Frida", "gadget"},
                {"Frida", "gum-js-loop"},
                {"Xposed", "XposedBridge"},
                {"Xposed", "xposed"},
                {"Substrate", "substrate"},
                {"Substrate", "cynject"},
                {"Other", "/data/local/tmp"},
                {"Other", "libriru"},      // Riru框架
                {"Other", "libsandhook"},  // SandHook框架
                {"Other", "epic"},         // Epic框架
                {"Other", "edxposed"},     // EdXposed框架
                {"Other", "taichi"},       // 太极框架
                {"LSPosed", "lspd"},       // LSPosed框架
                {"Dexposed", "dexposed"},  // DexPosed框架
                {"YAHFA", "libyahfa"},     // YAHFA框架
                {"Pine", "libpine"},       // Pine框架
                {"Dobby", "libdobby"},     // Dobby框架
                {"Shadow","libhookProxy"},
                {"Shadow","libshadow"}
        };

        // 读取文件内容
        while (true) {
            long bytes = utils::SyscallUtils::syscall(SYSCALL_READ, fd, (long)buffer, sizeof(buffer));
            if (bytes <= 0) break;

            content.append(buffer, bytes);

            size_t pos = 0;
            std::string remaining = content;
            while ((pos = remaining.find('\n')) != std::string::npos) {
                std::string line = remaining.substr(0, pos);
                remaining = remaining.substr(pos + 1);

                for (const auto& sig : signatures) {
                    // 使用多种方法检测特征
                    if (utils::StringUtils::containsSafe(line, sig.second)) {
                        hookFeatures[sig.first].push_back(line);
                    }
                }
            }
            content = remaining;
        }

        utils::SyscallUtils::syscall(SYSCALL_CLOSE, fd);

        // 报告发现的hook特征
        if (!hookFeatures.empty()) {
            std::string detail;

            for (const auto& feature : hookFeatures) {
                detail += "=== " + feature.first + " Framework ===\n";
                for (const auto& line : feature.second) {
                    detail += line + "\n";
                }
                detail += "\n";
            }

            DetectorUtils::reportWarning(env, globalCallback,
                                         "checkMaps_native",
                                         DetectorUtils::LEVEL_HIGH,
                                         detail);
            isMapsCheckReported = true;
        }

    } catch (const std::exception& e) {
        LOGE("Exception in detectTmpInMaps: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in detectTmpInMaps");
    }
}

void MiscDetector::cleanup() {
    isRunning = false;

    if (javaVM && globalCallback) {
        JNIEnv* env = nullptr;
        if (javaVM->AttachCurrentThread(&env, nullptr) == JNI_OK && env) {
            env->DeleteGlobalRef(globalCallback);
            javaVM->DetachCurrentThread();
        }
    }

    globalCallback = nullptr;
    javaVM = nullptr;
    resetDetectionState();
}

void MiscDetector::resetDetectionState() {
    isPathCheckReported = false;
    isMapsCheckReported = false;
}

void MiscDetector::stop() {
    if (!isRunning) {
        return;
    }

    try {
        isRunning = false;
        if (pthread_join(threadId, nullptr) != 0) {
            LOGE("Failed to join thread: %s", strerror(errno));
        }
        cleanup();
    } catch (const std::exception& e) {
        LOGE("Exception in stop: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in stop");
    }
}