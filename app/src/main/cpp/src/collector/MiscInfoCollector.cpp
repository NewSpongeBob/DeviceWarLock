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

void MiscInfoCollector::collectStorageStats() {
    struct statfs sf;
    const char* path = "/storage/emulated/0";
    
    // 使用 SyscallUtils 调用 statfs
    long result = utils::SyscallUtils::syscall(SYS_statfs, 
                                             (long)path, 
                                             (long)&sf, 
                                             0);
    
    if (result == 0) {
        // 计算存储信息
        unsigned long blockSize = sf.f_bsize;
        unsigned long totalBlocks = sf.f_blocks;
        unsigned long freeBlocks = sf.f_bfree;
        unsigned long availableBlocks = sf.f_bavail;
        
        // 创建 RapidJSON 对象
        rapidjson::Document document;
        document.SetObject();
        rapidjson::Document::AllocatorType& allocator = document.GetAllocator();
        
        // 添加数据
        document.AddMember("t", rapidjson::Value(std::to_string(totalBlocks * blockSize).c_str(), allocator), allocator);
        document.AddMember("f", rapidjson::Value(std::to_string(freeBlocks * blockSize).c_str(), allocator), allocator);
        document.AddMember("a", rapidjson::Value(std::to_string(availableBlocks * blockSize).c_str(), allocator), allocator);
        document.AddMember("bs", rapidjson::Value(std::to_string(blockSize).c_str(), allocator), allocator);
        
        // 转换为字符串
        rapidjson::StringBuffer buffer;
        rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
        document.Accept(writer);
        
        // 保存结果
        XsonCollector::getInstance()->put(
            constants::fingerprint::STORAGE_STATS_ID,  // n17
            buffer.GetString()
        );
    } else {
        // 如果调用失败，记录失败状态
        XsonCollector::getInstance()->putFailed(
            constants::fingerprint::STORAGE_STATS_ID
        );
    }
}

void MiscInfoCollector::collectDrmId() {
    try {
        // 加载 libmediandk.so
        void* handle = dlopen("libmediandk.so", RTLD_NOW);
        if (!handle) {
            // 尝试加载备选库
            handle = dlopen("libmedia_jni.so", RTLD_NOW);
            if (!handle) {
                XsonCollector::getInstance()->putFailed(
                    constants::fingerprint::DRM_ID_ID
                );
                return;
            }
        }

        // 定义函数指针类型
        typedef AMediaDrm* (*CreateFunc)(const uint8_t* uuid, size_t size, media_status_t* status);
        typedef media_status_t (*GetPropertyByteArrayFunc)(AMediaDrm*, const char* propertyName,
                                                         void** data, size_t* dataSize);
        typedef void (*DeleteFunc)(AMediaDrm*);
        typedef void (*ReleaseArrayFunc)(AMediaDrm*, const void* data);

        // 获取函数地址（尝试不同的函数名）
        CreateFunc createDrm = nullptr;
        const char* createFuncNames[] = {
            "AMediaDrm_createByUUID",
            "_Z21AMediaDrm_createByUUIDPKhyP13media_status_t",  // 符号修饰名
            "Java_android_media_MediaDrm_native_1createByUUID"   // JNI 名
        };
        for (const char* funcName : createFuncNames) {
            createDrm = (CreateFunc)dlsym(handle, funcName);
            if (createDrm) break;
        }

        GetPropertyByteArrayFunc getPropertyByteArray = nullptr;
        const char* getPropertyFuncNames[] = {
            "AMediaDrm_getPropertyByteArray",
            "_Z28AMediaDrm_getPropertyByteArrayP9AMediaDrmPKcPPvPy",
            "Java_android_media_MediaDrm_native_1getPropertyByteArray"
        };
        for (const char* funcName : getPropertyFuncNames) {
            getPropertyByteArray = (GetPropertyByteArrayFunc)dlsym(handle, funcName);
            if (getPropertyByteArray) break;
        }

        DeleteFunc deleteDrm = nullptr;
        const char* deleteFuncNames[] = {
            "AMediaDrm_delete",
            "_Z15AMediaDrm_deleteP9AMediaDrm",
            "Java_android_media_MediaDrm_native_1release"
        };
        for (const char* funcName : deleteFuncNames) {
            deleteDrm = (DeleteFunc)dlsym(handle, funcName);
            if (deleteDrm) break;
        }

        ReleaseArrayFunc releaseArray = nullptr;
        const char* releaseFuncNames[] = {
            "AMediaDrm_releaseByteArray",
            "_Z25AMediaDrm_releaseByteArrayP9AMediaDrmPKv",
            "Java_android_media_MediaDrm_native_1releaseByteArray"
        };
        for (const char* funcName : releaseFuncNames) {
            releaseArray = (ReleaseArrayFunc)dlsym(handle, funcName);
            if (releaseArray) break;
        }

        // 检查是否所有函数都获取成功
        if (!createDrm || !getPropertyByteArray || !deleteDrm || !releaseArray) {
            dlclose(handle);
            XsonCollector::getInstance()->putFailed(
                constants::fingerprint::DRM_ID_ID
            );
            return;
        }

        // Widevine UUID
        const uint8_t widevine_uuid[16] = {
            0xED, 0xEF, 0x8B, 0xA9,
            0x79, 0xD6, 0x4A, 0xCE,
            0xA3, 0xC8, 0x27, 0xDC,
            0xD5, 0x1D, 0x21, 0xED
        };

        // 创建 MediaDrm 实例
        media_status_t status = AMEDIA_OK;
        AMediaDrm* mediaDrm = createDrm(widevine_uuid, 16, &status);
        if (!mediaDrm || status != AMEDIA_OK) {
            dlclose(handle);
            XsonCollector::getInstance()->putFailed(
                constants::fingerprint::DRM_ID_ID
            );
            return;
        }

        // 获取设备唯一ID
        void* propertyValue = nullptr;
        size_t propertySize = 0;
        status = getPropertyByteArray(mediaDrm, "deviceUniqueId", &propertyValue, &propertySize);
        
        if (status == AMEDIA_OK && propertyValue && propertySize > 0) {
            // 将字节数组转换为十六进制字符串
            std::string hexString;
            const uint8_t* bytes = static_cast<const uint8_t*>(propertyValue);
            char hex[3];
            for (size_t i = 0; i < propertySize; i++) {
                snprintf(hex, sizeof(hex), "%02x", bytes[i]);
                hexString += hex;
            }

            // 保存结果
            XsonCollector::getInstance()->put(
                constants::fingerprint::DRM_ID_ID,
                hexString
            );

            // 释放资源
            releaseArray(mediaDrm, propertyValue);
        } else {
            XsonCollector::getInstance()->putFailed(
                constants::fingerprint::DRM_ID_ID
            );
        }

        // 清理资源
        deleteDrm(mediaDrm);
        dlclose(handle);

    } catch (const std::exception& e) {
        XsonCollector::getInstance()->putFailed(
            constants::fingerprint::DRM_ID_ID
        );
    }
}



void MiscInfoCollector::collect(std::map<std::string, std::string>& info) {
    collectServiceList();    // n16
    collectStorageStats();   // n17
    collectDrmId();         // n18
}