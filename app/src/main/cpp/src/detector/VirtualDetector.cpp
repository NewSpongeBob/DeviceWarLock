#include "../../inc/detector/VirtualDetector.h"
#include "../../inc/utils/SyscallUtils.h"
#include "../../inc/utils/MiscUtil.h"

const std::string VirtualDetector::CHECK_VIRTUAL = "virtual_check";
const std::string VirtualDetector::CHECK_THERMAL = "thermal_check";
const std::string VirtualDetector::CHECK_PROCESS = "process_check";
void VirtualDetector::detect(JNIEnv* env, jobject callback) {
    detectArch(env, callback);
    detectThermal(env, callback);

    if (MiscUtil::SystemUtils::getSDKLevel() > __ANDROID_API_Q__) {
        detectProcess(env, callback);
    }
}

void VirtualDetector::install_check_arch_seccomp() {
    struct sock_filter filter[15] = {
            BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (uint32_t) offsetof(struct seccomp_data, nr)),
            BPF_JUMP(BPF_JMP + BPF_JEQ, __NR_getpid, 0, 12),
            BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (uint32_t) offsetof(struct seccomp_data, args[0])),
            BPF_JUMP(BPF_JMP + BPF_JEQ, DetectX86Flag, 0, 10),
            BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (uint32_t) offsetof(struct seccomp_data, arch)),
            BPF_JUMP(BPF_JMP + BPF_JEQ, AUDIT_ARCH_X86_64, 0, 1),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ERRNO | (864 & SECCOMP_RET_DATA)),
            BPF_JUMP(BPF_JMP + BPF_JEQ, AUDIT_ARCH_I386, 0, 1),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ERRNO | (386 & SECCOMP_RET_DATA)),
            BPF_JUMP(BPF_JMP + BPF_JEQ, AUDIT_ARCH_ARM, 0, 1),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ERRNO | (0xA32 & SECCOMP_RET_DATA)),
            BPF_JUMP(BPF_JMP + BPF_JEQ, AUDIT_ARCH_AARCH64, 0, 1),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ERRNO | (0xA64 & SECCOMP_RET_DATA)),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ERRNO | (6 & SECCOMP_RET_DATA)),
            BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ALLOW)
    };

    struct sock_fprog program = {
            .len = (unsigned short) (sizeof(filter) / sizeof(filter[0])),
            .filter = filter
    };

    errno = 0;
    if (utils::SyscallUtils::syscall(__NR_prctl, PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)) {
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %s", strerror(errno));
        return;
    }

    errno = 0;
    if (utils::SyscallUtils::syscall(__NR_prctl, PR_SET_SECCOMP, SECCOMP_MODE_FILTER, (long)&program)) {
        LOGE("prctl(PR_SET_SECCOMP) failed: %s", strerror(errno));
        return;
    }
}

std::string VirtualDetector::check_arch_by_seccomp() {
    if (MiscUtil::SystemUtils::getSDKLevel() < __ANDROID_API_N_MR1__) {
        return "";
    }

    errno = 0;
    utils::SyscallUtils::syscall(__NR_getpid, DetectX86Flag);

    if (errno == 386) {
        return "I386设备";
    } else if (errno == 864) {
        return "X86_64设备";
    } else if (errno == 0xA32 || errno == 0xA64) {
        return "";  // ARM设备，正常
    } else if (errno == 0) {
        return "";  // 可能是没有开启seccomp
    }
    return std::to_string(errno);
}
void VirtualDetector::detectArch(JNIEnv* env, jobject callback) {
    if (!env || !callback) {
        LOGE("Invalid JNI parameters");
        return;
    }

    install_check_arch_seccomp();
    std::string result = check_arch_by_seccomp();

    if (!result.empty()) {
        DetectorUtils::reportWarning(env, callback,
                                     CHECK_VIRTUAL,
                                     DetectorUtils::LEVEL_HIGH,
                                    result);
    }
}
void VirtualDetector::detectProcess(JNIEnv* env, jobject callback) {
    if (!env || !callback) {
        LOGE("Invalid JNI parameters");
        return;
    }

    FILE* file = nullptr;
    try {
        file = popen("ps -ef", "r");
        if (!file) {
            throw std::runtime_error("无法执行ps命令: " + std::string(strerror(errno)));
        }

        char buf[0x1000];
        std::string buffStr;
        uint32_t process_count = 0;

        while (fgets(buf, sizeof(buf), file)) {
            try {
                std::string line(buf);
                buffStr += line;

                if (!utils::StringUtils::contains(line, "xiaoc")) {
                    process_count++;
                    LOGI("ps -ef match: %s", line.c_str());
                }
            } catch (const std::exception& e) {
                LOGE("Error processing line: %s", e.what());
                continue;
            }
        }

        if (file) {
            pclose(file);
            file = nullptr;
        }

        if (process_count > MAX_NORMAL_PROCESS) {
            DetectorUtils::reportWarning(env, callback,
                                         CHECK_PROCESS,
                                         DetectorUtils::LEVEL_HIGH,
                                         buffStr);
        } else {
            LOGE("No sandbox detected in process check");
        }
    } catch (const std::exception& e) {
        if (file) {
            pclose(file);
        }
        throw; // 重新抛出异常，让上层处理
    }
}

void VirtualDetector::detectThermal(JNIEnv* env, jobject callback) {
    if (!env || !callback) {
        LOGE("Invalid JNI parameters");
        return;
    }

    try {
        int thermal_count = check_thermal_zones();

        if (thermal_count < MIN_THERMAL_ZONES) {
            std::string detail = thermal_count == -1 ?
                                 "无法访问温度传感器目录: " + std::string(strerror(errno)) :
                                 "温度传感器数量异常: " + std::to_string(thermal_count);

            DetectorUtils::reportWarning(env, callback,
                                         CHECK_THERMAL,
                                         DetectorUtils::LEVEL_HIGH,
                                         "疑似虚拟环境: " + detail);
        }
    } catch (const std::exception& e) {
        LOGE("Error in thermal detection: %s", e.what());
        throw;
    }
}

int VirtualDetector::check_thermal_zones() {
    DIR* dir_ptr = nullptr;
    int count = 0;

    try {
        dir_ptr = opendir("/sys/class/thermal/");
        if (!dir_ptr) {
            return -1;
        }

        struct dirent* entry;
        while ((entry = readdir(dir_ptr))) {
            if (!entry->d_name ||
                !strcmp(entry->d_name, ".") ||
                !strcmp(entry->d_name, "..")) {
                continue;
            }
            if (strstr(entry->d_name, "thermal_zone") != nullptr) {
                count++;
            }
        }

        closedir(dir_ptr);
        return count;
    } catch (const std::exception& e) {
        if (dir_ptr) {
            closedir(dir_ptr);
        }
        throw;
    }
}