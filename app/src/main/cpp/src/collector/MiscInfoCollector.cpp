#include "../inc/collector/MiscInfoCollector.h"
void MiscInfoCollector::collectServiceList() {
    FILE* fp = popen("service list", "r");
    if (!fp) {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::SERVICE_LIST_ID
        );
        return;
    }

    std::string result = "{";
    char buffer[4096];
    bool firstService = true;
    bool skipFirstLine = true;  // 用于跳过第一行

    while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
        std::string line(buffer);

        // 跳过第一行（Found xxx services:）
        if (skipFirstLine) {
            skipFirstLine = false;
            continue;
        }

        // 跳过空行
        if (line.empty() || line == "\n") {
            continue;
        }

        // 提取服务ID和描述
        size_t idEnd = line.find_first_of(" \t");
        if (idEnd != std::string::npos) {
            std::string serviceId = line.substr(0, idEnd);
            // 去除开头和结尾的空白字符
            size_t descStart = line.find_first_not_of(" \t", idEnd);
            if (descStart != std::string::npos) {
                std::string serviceDesc = line.substr(descStart);
                // 去除结尾的换行符
                if (!serviceDesc.empty() && serviceDesc.back() == '\n') {
                    serviceDesc.pop_back();
                }

                if (!firstService) {
                    result += ",";
                }
                firstService = false;

                result += "\"" + serviceId + "\":\"" + serviceDesc + "\"";
            }
        }
    }

    result += "}";
    pclose(fp);

    if (result.length() > 2) { // 不只是"{}"
        XsonCollector::getInstance()->put(
                constants::fingerprint::SERVICE_LIST_ID,
                result
        );
    } else {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::SERVICE_LIST_ID
        );
    }

}
void MiscInfoCollector::collect(std::map<std::string, std::string>& info) {
    collectServiceList();   //n16
}