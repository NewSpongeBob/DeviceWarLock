// src/utils/XsonCollector.cpp
#include "../inc/utils/XsonCollector.h"
#include "../inc/utils/LogUtils.h"


XsonCollector* XsonCollector::instance = nullptr;

XsonCollector::XsonCollector() {
    doc.SetObject();
}

XsonCollector::~XsonCollector() = default;

XsonCollector* XsonCollector::getInstance() {
    if (instance == nullptr) {
        instance = new XsonCollector();
    }
    return instance;
}

void XsonCollector::put(const std::string& key, const std::string& value) {
    try {
        rapidjson::Document::AllocatorType& allocator = doc.GetAllocator();
        rapidjson::Value item(rapidjson::kObjectType);

        item.AddMember("s", 0, allocator);  // 成功状态
        item.AddMember(
                "v",
                rapidjson::Value(value.c_str(), allocator).Move(),
                allocator
        );

        doc.AddMember(
                rapidjson::Value(key.c_str(), allocator).Move(),
                item,
                allocator
        );
    } catch (const std::exception& e) {
        putFailed(key);
    }
}

void XsonCollector::putFailed(const std::string& key) {
    rapidjson::Document::AllocatorType& allocator = doc.GetAllocator();
    rapidjson::Value item(rapidjson::kObjectType);

    item.AddMember("s", -1, allocator);  // 失败状态

    doc.AddMember(
            rapidjson::Value(key.c_str(), allocator).Move(),
            item,
            allocator
    );
}

void XsonCollector::putNotCollected(const std::string& key) {
    rapidjson::Document::AllocatorType& allocator = doc.GetAllocator();
    rapidjson::Value item(rapidjson::kObjectType);

    item.AddMember("s", -2, allocator);  // 未收集状态
    item.AddMember("v", rapidjson::Value(rapidjson::kNullType), allocator);

    doc.AddMember(
            rapidjson::Value(key.c_str(), allocator).Move(),
            item,
            allocator
    );
}

std::string XsonCollector::toString() {
    try {
        rapidjson::StringBuffer buffer;
        rapidjson::PrettyWriter<rapidjson::StringBuffer> writer(buffer);
        doc.Accept(writer);
        return buffer.GetString();
    } catch (const std::exception& e) {
        return "{}";
    }
}

void XsonCollector::clear() {
    doc.SetObject();
}