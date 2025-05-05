#include "../../inc/detector/FridaDetector.h"


const std::string FridaDetector::CHECK_FRIDA = "frida_check";

FridaDetector::FridaDetector()
        : isRunning(false), javaVM(nullptr), globalCallback(nullptr),
          isAbnormalStateReported(false) , isMapsAbnormalReported(false){
}

FridaDetector::~FridaDetector() {
    stop();
}

void FridaDetector::detect(JNIEnv* env, jobject callback) {
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

        try {
            detectFridaFile(env);
        }  catch (...) {

        }

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

void* FridaDetector::threadFunction(void* arg) {
    FridaDetector* detector = static_cast<FridaDetector*>(arg);

    // 设置线程名
    prctl(PR_SET_NAME, "warlockcf");

    // 获取JNI环境
    JNIEnv* env = nullptr;
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6;
    args.name = "FridaDetector";
    args.group = nullptr;

    if (detector->javaVM->AttachCurrentThread(&env, &args) != JNI_OK || !env) {
        LOGE("Failed to attach thread to JVM");
        return nullptr;
    }

    // 主检测循环
    while (detector->isRunning) {
        try {
            detector->detectFridaPorts(env);
            detector->detectFridaInMaps(env);

        } catch (const std::exception& e) {
            LOGE("Exception in detection loop: %s", e.what());
        } catch (...) {
            LOGE("Unknown exception in detection loop");
        }
        usleep(3000 * 1000);
    }

    detector->javaVM->DetachCurrentThread();
    return nullptr;
}
void FridaDetector::cleanup() {
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
void FridaDetector::detectFridaPorts(JNIEnv* env) {
    if (!env || !globalCallback) {
        LOGE("Invalid JNI environment or callback");
        return;
    }

    const int FRIDA_DEFAULT_PORT = 27042;
    const int knownPorts[] = {27042, 6666, 9999, 6699};
    const int knownPortsCount = sizeof(knownPorts) / sizeof(knownPorts[0]);

    try {
        struct sockaddr_in sa;
        memset(&sa, 0, sizeof(sa));
        sa.sin_family = AF_INET;
        sa.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

        bool foundNewPort = false;
        std::string newDetectedPorts;

        for (int i = 0; i < knownPortsCount; i++) {
            if (detectedPorts.find(knownPorts[i]) != detectedPorts.end()) {
                continue;
            }

            int sock = socket(AF_INET, SOCK_STREAM, 0);
            if (sock == -1) {
                LOGD("Failed to create socket: %s", strerror(errno));
                continue;
            }

            // 设置socket超时
            struct timeval timeout;
            timeout.tv_sec = 1;
            timeout.tv_usec = 0;
            setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
            setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));

            sa.sin_port = htons(knownPorts[i]);
            if (connect(sock, (struct sockaddr*)&sa, sizeof(sa)) != -1) {
                LOGD("Found new suspicious port: %d", knownPorts[i]);
                detectedPorts.insert(knownPorts[i]);
                foundNewPort = true;
                if (!newDetectedPorts.empty()) {
                    newDetectedPorts += ", ";
                }
                newDetectedPorts += std::to_string(knownPorts[i]);
            }
            close(sock);
        }

        if (foundNewPort && env->ExceptionCheck()) {
            env->ExceptionClear();
        }

        // 只在发现新端口时报告
        if (foundNewPort) {
            std::string detail;
            std::string checkType;

            if (detectedPorts.find(FRIDA_DEFAULT_PORT) != detectedPorts.end()) {
                detail = "port: " + newDetectedPorts;
                checkType = "checkFridaPort_native";
            } else {
                detail = "ports: " + newDetectedPorts;
                checkType = "checkAbnormalPorts_native";
            }

            if (!isAbnormalStateReported) {
                isAbnormalStateReported = true;
            }

            DetectorUtils::reportWarning(env, globalCallback,
                                         checkType,
                                         DetectorUtils::LEVEL_HIGH,
                                         detail);
        }
    } catch (const std::exception& e) {
        LOGE("Exception in detectFridaPorts: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in detectFridaPorts");
    }
}
void FridaDetector::detectFridaFile(JNIEnv* env) {
    try {
        const char* targetPath = "/data/local/tmp";
        int fd;
                // Android 系统调用号
        #if defined(__aarch64__)
                const long SYSCALL_OPEN = 56;        // openat for arm64
                const long SYSCALL_GETDENTS64 = 61;  // getdents64 for arm64
                const long SYSCALL_CLOSE = 57;       // close for arm64
        #elif defined(__arm__)
                const long SYSCALL_OPEN = 5;         // open for arm
                const long SYSCALL_GETDENTS64 = 217; // getdents64 for arm
                const long SYSCALL_CLOSE = 6;        // close for arm
                #elif defined(__x86_64__)
                const long SYSCALL_OPEN = 2;         // open for x86_64
                const long SYSCALL_GETDENTS64 = 217; // getdents64 for x86_64
                const long SYSCALL_CLOSE = 3;        // close for x86_64
                #elif defined(__i386__)
                const long SYSCALL_OPEN = 5;         // open for x86
                const long SYSCALL_GETDENTS64 = 220; // getdents64 for x86
                const long SYSCALL_CLOSE = 6;        // close for x86
                #else
                #error "Unsupported architecture"
        #endif
        // 使用系统调用打开目录
        fd = utils::SyscallUtils::syscall(SYSCALL_OPEN, (long)targetPath, O_RDONLY | O_DIRECTORY);
        if (fd < 0) {
            LOGE("Failed to open directory: %s", strerror(errno));
            return;
        }

        char buf[1024];
        int nread;
        struct linux_dirent64 {
            uint64_t        d_ino;
            int64_t         d_off;
            unsigned short  d_reclen;
            unsigned char   d_type;
            char           d_name[];
        };

        bool foundFrida = false;
        std::string foundFile;

        // 读取目录内容
        while ((nread = utils::SyscallUtils::syscall(SYS_getdents64, fd, (long)buf, sizeof(buf))) > 0) {
            for (int bpos = 0; bpos < nread;) {
                struct linux_dirent64* d = (struct linux_dirent64*)(buf + bpos);
                std::string filename(d->d_name);

                // 检查文件名是否包含 frida-server
                if (filename.find("frida-server") != std::string::npos) {
                    foundFrida = true;
                    foundFile = filename;
                    break;
                }

                bpos += d->d_reclen;
            }

            if (foundFrida) break;
        }

        // 关闭目录
        utils::SyscallUtils::syscall(SYS_close, fd);

        // 如果找到frida-server，报告警告
        if (foundFrida) {
            std::string detail =  targetPath + foundFile;

            DetectorUtils::reportWarning(env, globalCallback,
                                         "checkFridaFile_native",
                                         DetectorUtils::LEVEL_HIGH,
                                         detail);
        }

    } catch (const std::exception& e) {
        LOGE("Exception in detectFridaServer: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in detectFridaServer");
    }
}
void FridaDetector::detectFridaInMaps(JNIEnv* env) {
    // 如果已经报告过，直接返回
    if (isMapsAbnormalReported) {
        return;
    }
    try {
        // Android 系统调用号
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

        #elif defined(__i386__)
        const long SYSCALL_OPENAT = 295;    // openat for x86
        const long SYSCALL_READ = 3;        // read for x86
        const long SYSCALL_CLOSE = 6;       // close for x86
        #else
        #error "Unsupported architecture"
#endif

        // 打开 /proc/self/maps
        int fd = utils::SyscallUtils::syscall(SYSCALL_OPENAT, AT_FDCWD, (long)"/proc/self/maps", O_RDONLY);
        if (fd < 0) {
            LOGE("Failed to open maps: %s", strerror(errno));
            return;
        }

        char buffer[4096];
        std::string content;
        bool foundSuspicious = false;
        std::vector<std::string> suspiciousEntries;

        // 读取文件内容
        while (true) {
            long bytes = utils::SyscallUtils::syscall(SYSCALL_READ, fd, (long)buffer, sizeof(buffer));
            if (bytes <= 0) break;

            content.append(buffer, bytes);

            // 查找可疑字符串
            size_t pos = 0;
            while ((pos = content.find('\n')) != std::string::npos) {
                std::string line = content.substr(0, pos);
                content.erase(0, pos + 1);

                // 检查是否包含可疑字符串
                if (line.find("frida") != std::string::npos ||
                    line.find("agent") != std::string::npos ||
                    line.find("gadget") != std::string::npos ||
                    line.find("gum-js-loop") != std::string::npos){

                    foundSuspicious = true;
                    suspiciousEntries.push_back(line);
                }
            }
        }

        // 关闭文件
        utils::SyscallUtils::syscall(SYSCALL_CLOSE, fd);

        // 如果找到可疑内容，报告警告
        if (foundSuspicious) {
            std::string detail;
            for (const auto& entry : suspiciousEntries) {
                detail += entry + "\n";
            }

            DetectorUtils::reportWarning(env, globalCallback,
                                         "checkFridaInMaps_native",
                                         DetectorUtils::LEVEL_HIGH,
                                         detail);
            isMapsAbnormalReported = true;
        }

    } catch (const std::exception& e) {
        LOGE("Exception in detectFridaInMaps: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in detectFridaInMaps");
    }
}
void FridaDetector::stop() {
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

void FridaDetector::resetDetectionState() {
    detectedPorts.clear();
    isAbnormalStateReported = false;
    isMapsAbnormalReported = false;
}
