#include "../../inc/detector/SystemDetector.h"

const std::string SystemDetector::CHECK_SYSTEM = "checkSystem_native";

void SystemDetector::detect(JNIEnv* env, jobject callback) {
    if (!env || !callback) {
        LOGE("Invalid JNI parameters");
        return;
    }

    try {
        std::vector<std::string> abnormalDetails;

        // 1. 检查 dm-verity 状态
        if (!checkDmVerity()) {
            abnormalDetails.push_back("Dm-Verity is diasble");
        }

        // 2. 检查系统分区状态
        if (!checkSystemPartition()) {
            abnormalDetails.push_back("SystemPart is write");
        }

        // 3. 检查 AVB 状态
        if (!checkAVB()) {
            abnormalDetails.push_back("AVB diasble");
        }

        // 如果发现任何异常
        if (!abnormalDetails.empty()) {
            std::string detail = "System Abnormal:\n";
            for (const auto& item : abnormalDetails) {
                detail += "- " + item + "\n";
            }
            DetectorUtils::reportWarning(
                    env,
                    callback,
                    CHECK_SYSTEM,
                    DetectorUtils::LEVEL_MEDIUM,
                    detail
            );
        }
    } catch (const std::exception& e) {
        LOGE("Error in system detection: %s", e.what());
    }
}

bool SystemDetector::checkDmVerity() {
    std::ifstream verityStatus("/sys/module/dm_verity/parameters/status");
    if (!verityStatus.is_open()) {
        return false;
    }
    std::string status;
    std::getline(verityStatus, status);
    return status.find("enabled") != std::string::npos;
}

bool SystemDetector::checkSystemPartition() {
    FILE* mounts = fopen("/proc/mounts", "r");
    if (!mounts) {
        return false;
    }

    bool isReadOnly = true;
    char line[512];
    while (fgets(line, sizeof(line), mounts)) {
        if (strstr(line, "/system") && !strstr(line, " ro,")) {
            isReadOnly = false;
            break;
        }
    }
    fclose(mounts);
    return isReadOnly;
}

bool SystemDetector::checkAVB() {
    return access("/sys/fs/avb/", F_OK) == 0;
}

