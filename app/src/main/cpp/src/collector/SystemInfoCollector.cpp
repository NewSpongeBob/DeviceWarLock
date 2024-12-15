#include "../inc/collector/SystemInfoCollector.h"

void SystemInfoCollector::collectProps() {
    rapidjson::Document doc;
    doc.SetObject();
    auto& allocator = doc.GetAllocator();

    // 创建基础结构
    rapidjson::Value root(rapidjson::kObjectType);
    root.AddMember("s", 0, allocator);

    rapidjson::Value v(rapidjson::kObjectType);
    rapidjson::Value commonProps(rapidjson::kObjectType);
    rapidjson::Value rareProps(rapidjson::kObjectType);

    // 收集常见属性
    char value[PROP_VALUE_MAX];
    for (size_t i = 0; i < constants::system_props::COMMON_PROPS_SIZE; i++) {
        memset(value, 0, PROP_VALUE_MAX);
        __system_property_get(constants::system_props::COMMON_PROPS[i], value);
        if (strlen(value) > 0) {
            commonProps.AddMember(
                    rapidjson::Value(constants::system_props::COMMON_PROPS[i], allocator),
                    rapidjson::Value(value, allocator),
                    allocator
            );
        }
    }

    // 收集不常见属性
    for (size_t i = 0; i < constants::system_props::RARE_PROPS_SIZE; i++) {
        memset(value, 0, PROP_VALUE_MAX);
        __system_property_get(constants::system_props::RARE_PROPS[i], value);
        if (strlen(value) > 0) {
            rareProps.AddMember(
                    rapidjson::Value(constants::system_props::RARE_PROPS[i], allocator),
                    rapidjson::Value(value, allocator),
                    allocator
            );
        }
    }

    // 构建最终结构
    v.AddMember("s", commonProps, allocator);
    v.AddMember("r", rareProps, allocator);
    root.AddMember("v", v, allocator);

    // 转换为字符串
    rapidjson::StringBuffer buffer;
    rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
    root.Accept(writer);

    // 保存结果
    XsonCollector::getInstance()->put(constants::fingerprint::SYSTEM_PROP_ID, buffer.GetString());
}
std::string SystemInfoCollector::readBuildProp(const char* path) {
    // 使用 openat 打开文件
    int fd = raw_syscall(__NR_openat, AT_FDCWD, path, O_RDONLY);
    if (fd < 0) {
        return "";
    }

    std::string content;
    bool success = readFileContent(fd, content);
    raw_syscall(__NR_close, fd);

    return success ? content : "";
}

bool SystemInfoCollector::readFileContent(int fd, std::string& content) {
    char buffer[4096];
    ssize_t bytesRead;
    content.clear();

    while ((bytesRead = raw_syscall(__NR_read, fd, buffer, sizeof(buffer))) > 0) {
        content.append(buffer, bytesRead);
    }

    return bytesRead >= 0;
}
void SystemInfoCollector::readSystemFile(const char* path, const char* fingerprintId) {
    int fd = raw_syscall(__NR_openat, AT_FDCWD, path, O_RDONLY);
    if (fd < 0) {
        XsonCollector::getInstance()->putFailed(fingerprintId);
        return;
    }

    std::string content;
    bool success = readFileContent(fd, content);
    raw_syscall(__NR_close, fd);

    if (success && !content.empty()) {
        // 特殊处理 /proc/misc 文件
        if (strcmp(path, "/proc/misc") == 0) {
            std::string result = "[";
            std::istringstream iss(content);
            std::string line;
            bool first = true;

            while (std::getline(iss, line)) {
                // 跳过空行
                if (line.empty()) continue;

                // 添加逗号分隔符
                if (!first) {
                    result += ",";
                }
                first = false;

                // 去除行首尾空格
                line.erase(0, line.find_first_not_of(" \t"));
                line.erase(line.find_last_not_of(" \t") + 1);

                result += "\"" + line + "\"";
            }
            result += "]";

            XsonCollector::getInstance()->put(fingerprintId, result);
        } else {
            // 其他文件正常处理
            if (content.back() == '\n') {
                content.pop_back();
            }
            XsonCollector::getInstance()->put(fingerprintId, content);
        }
    } else {
        XsonCollector::getInstance()->putFailed(fingerprintId);
    }
}
void SystemInfoCollector::collectBuildPropFile(){
    // 读取各个 build.prop 文件
    std::string systemProp = readBuildProp(SYSTEM_BUILD_FILE);
    if (!systemProp.empty()) {
        XsonCollector::getInstance()->put(SYSTEM_BUILD_ID,
                                          "{\"s\":0,\"v\":\"" + systemProp + "\"}");
    } else {
        XsonCollector::getInstance()->putFailed(SYSTEM_BUILD_ID);
    }

    std::string odmProp = readBuildProp(ODM_ETC_BUILD_FILE);
    if (!odmProp.empty()) {
        XsonCollector::getInstance()->put(ODM_ETC_BUILD_ID,
                                          "{\"s\":0,\"v\":\"" + odmProp + "\"}");
    } else {
        XsonCollector::getInstance()->putFailed(ODM_ETC_BUILD_ID);
    }

    std::string productProp = readBuildProp(PRODUCT_BUILD_FILE);
    if (!productProp.empty()) {
        XsonCollector::getInstance()->put(PRODUCT_BUILD_ID,
                                          "{\"s\":0,\"v\":\"" + productProp + "\"}");
    } else {
        XsonCollector::getInstance()->putFailed(PRODUCT_BUILD_ID);
    }

    std::string vendorProp = readBuildProp(VENDOR_BUILD_FILE);
    if (!vendorProp.empty()) {
        XsonCollector::getInstance()->put(VENDOR_BUILD_ID,
                                          "{\"s\":0,\"v\":\"" + vendorProp + "\"}");
    } else {
        XsonCollector::getInstance()->putFailed(VENDOR_BUILD_ID);
    }
}
void SystemInfoCollector::collectDeviceID(){

    // 读取 UUID
    readSystemFile(UUID_PATH, UUID_ID);

    // 读取 CID
    readSystemFile(CID_PATH, CID_ID);

    // 读取序列号
    readSystemFile(SERIAL_NUMBER_PATH, SERIAL_NUMBER_ID);

    // 读取 misc
    readSystemFile(MISC_PATH, MISC_ID);

    // 读取 boot_id
    readSystemFile(BOOT_ID_PATH, BOOT_ID);
}
#include <regex>

std::string extractVersion(const std::string& input) {
    std::regex pattern("Linux\\s+\\w+\\s+([\\d.-]+[\\w-]+)");
    std::smatch matches;
    if (std::regex_search(input, matches, pattern) && matches.size() > 1) {
        return matches[1].str();
    }
    return "";
}

void SystemInfoCollector::collectSystemVersion() {
    std::string popenResult, unameResult, versionResult;
    bool hasAnyResult = false;

    // 1. popen获取 uname -a
    FILE* fp = popen("uname -a", "r");
    if (fp) {
        char buffer[4096];
        if (fgets(buffer, sizeof(buffer), fp) != nullptr) {
            popenResult = buffer;
            if (popenResult.back() == '\n') {
                popenResult.pop_back();
            }
            hasAnyResult = true;
        }
        pclose(fp);
    }

    // 2. svc uname 调用
    struct utsname un;
    if (raw_syscall(__NR_uname, &un) == 0) {
        unameResult = std::string(un.sysname) + " " +
                      un.nodename + " " +
                      un.release + " " +
                      un.version + " " +
                      un.machine;
        hasAnyResult = true;
    }

    // 3. svc openat 读取 /proc/version
    int fd = raw_syscall(__NR_openat, AT_FDCWD, "/proc/version", O_RDONLY);
    if (fd >= 0) {
        char buffer[4096];
        ssize_t bytes = raw_syscall(__NR_read, fd, buffer, sizeof(buffer) - 1);
        if (bytes > 0) {
            buffer[bytes] = '\0';
            versionResult = buffer;
            if (versionResult.back() == '\n') {
                versionResult.pop_back();
            }
            hasAnyResult = true;
        }
        raw_syscall(__NR_close, fd);
    }

    if (hasAnyResult) {
        // 构建包含所有获取到的结果的JSON
        std::string result = "{";
        bool first = true;

        if (!popenResult.empty()) {
            result += "\"popen\":\"" + popenResult + "\"";
            first = false;
        }
        if (!unameResult.empty()) {
            if (!first) result += ",";
            result += "\"suname\":\"" + unameResult + "\"";
            first = false;
        }
        if (!versionResult.empty()) {
            if (!first) result += ",";
            result += "\"sopenat\":\"" + versionResult + "\"";
        }
        result += "}";

        XsonCollector::getInstance()->put(
                constants::fingerprint::SYSTEM_VERSION_ID,
                result
        );
    } else {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::SYSTEM_VERSION_ID
        );
    }
}
void SystemInfoCollector::collectGetProp() {
    FILE* fp = popen("getprop", "r");
    if (!fp) {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::GET_PROPS_ID
        );
        return;
    }

    std::string result = "{";
    char buffer[4096];
    bool first = true;

    while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
        std::string line(buffer);

        // 查找属性名和值的起始位置
        size_t nameStart = line.find('[');
        size_t nameEnd = line.find(']');
        size_t valueStart = line.find('[', nameEnd);
        size_t valueEnd = line.find(']', valueStart);

        if (nameStart != std::string::npos && nameEnd != std::string::npos &&
            valueStart != std::string::npos && valueEnd != std::string::npos) {

            std::string name = line.substr(nameStart + 1, nameEnd - nameStart - 1);
            std::string value = line.substr(valueStart + 1, valueEnd - valueStart - 1);

            if (!first) {
                result += ",";
            }
            first = false;

            result += "\"" + name + "\":\"" + value + "\"";
        }
    }

    result += "}";
    pclose(fp);

    if (result.length() > 2) { //
        XsonCollector::getInstance()->put(
                constants::fingerprint::GET_PROPS_ID,
                result
        );
    } else {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::GET_PROPS_ID
        );
    }
}
void SystemInfoCollector::collect(std::map<std::string, std::string>& info) {
    collectProps(); //n2
    collectBuildPropFile(); //n4-n7
    collectDeviceID();  //n8-n12
    collectSystemVersion(); //n13
    collectGetProp();   //n14
}