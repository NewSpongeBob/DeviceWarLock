#include "detector/SandboxDetector.h"

std::string SandboxDetector::getLibcPath() {
#if defined(__aarch64__)
    return "/system/lib64/libc.so";
#else
    return "/system/lib/libc.so";
#endif
}

void SandboxDetector::getNameByPid(pid_t pid, char* buff) {
    char path[64] = {0};
    snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
    FILE* fp = fopen(path, "r");
    if (fp) {
        fgets(buff, 200, fp);
        fclose(fp);
    }
}

void* SandboxDetector::replaceSecInsns(const char* libPath, const char* symbol) {
    void* handle = dlopen(libPath, RTLD_NOW);
    if (handle) {
        return dlsym(handle, symbol);
    }
    return nullptr;
}

void SandboxDetector::notifyDetection(JNIEnv* env, jobject thiz, const std::string& details) {
    // 获取类和方法
    jclass cls = env->GetObjectClass(thiz);
    jmethodID method = env->GetMethodID(cls, "onSandboxDetected", "(Ljava/lang/String;)V");

    if (method) {
        // 调用Java层回调
        jstring jDetails = env->NewStringUTF(details.c_str());
        env->CallVoidMethod(thiz, method, jDetails);
        env->DeleteLocalRef(jDetails);
    }
}

void SandboxDetector::checkSandbox(JNIEnv* env, jobject thiz) {
    LOGD("Enter checkSandbox");
    auto orig_opendir = reinterpret_cast<OpenDir>(
            replaceSecInsns(getLibcPath().c_str(), "opendir"));

    DIR *pdr = orig_opendir("/proc");
    if (pdr == nullptr) {
        LOGE("Failed to open /proc directory");
        return;
    }

    auto orig_readdir = reinterpret_cast<ReadDir>(
            replaceSecInsns(getLibcPath().c_str(), "readdir"));

    pid_t main_pid = getpid();
    dirent *read_ptr;
    std::string detailsStr;

    while ((read_ptr = orig_readdir(pdr)) != nullptr) {
        long proc_pid = strtol(read_ptr->d_name, nullptr, 10);

        if (proc_pid && proc_pid == main_pid) {
            char proc_name[200] = {0};
            getNameByPid(proc_pid, proc_name);

            if (strstr(proc_name, "com.xiaoc.warlock") != nullptr) {
                if (!detailsStr.empty()) {
                    detailsStr += "\n";
                }

                detailsStr += "Pid: " + std::to_string(proc_pid) +
                              "(pid name: " + proc_name + ")";

                LOGE("Found sandbox process: PID=%ld, Name=%s", proc_pid, proc_name);
            }
        }
    }

    closedir(pdr);

    if (!detailsStr.empty()) {
        notifyDetection(env, thiz, detailsStr);
    }
}
void SandboxDetector::checkProcessByPs(JNIEnv* env, jobject thiz) {
    const char* cmd;
    int api_level = android_get_device_api_level();

    // Android 10 (API 29) 及以上使用 ps -ef
    // Android 10 以下使用 ps
    if (api_level >= 29) {
        cmd = "ps -ef";
    } else {
        cmd = "ps";
    }

    auto orig_popen = reinterpret_cast<FILE *(*)(const char *, const char *)>(
            replaceSecInsns(getLibcPath().c_str(), "popen"));

    FILE *file = orig_popen(cmd, "r");
    if (file == nullptr) {
        LOGE("Failed to execute ps command");
        return;
    }

    char buf[0x1000];
    std::string detailsStr;
    int processCount = 0;
    bool isFirstLine = true;

    while (fgets(buf, sizeof(buf), file)) {
        // 跳过第一行（标题行）
        if (isFirstLine) {
            isFirstLine = false;
            continue;
        }
        // 检查是否包含我们的包名
        if (strstr(buf, "com.xiaoc.warlock") != nullptr) {
            processCount++;
            // 如果已经有内容，添加换行
            if (!detailsStr.empty()) {
                detailsStr += "\n";
            }
            // 移除行尾的换行符
            std::string line(buf);
            if (!line.empty() && line[line.length()-1] == '\n') {
                line.erase(line.length()-1);
            }
            detailsStr += line;
        }
    }

    pclose(file);

    // 如果找到多于一个进程（不包括ps命令本身）
    if (processCount > 0 && !detailsStr.empty()) {
        LOGE("Found multiple processes: \n%s", detailsStr.c_str());
        notifyDetection(env, thiz, detailsStr);
    }
}