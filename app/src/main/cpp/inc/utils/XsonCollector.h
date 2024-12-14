// inc/utils/XsonCollector.h
#ifndef WARLOCK_XSONCOLLECTOR_H
#define WARLOCK_XSONCOLLECTOR_H

#include <string>
#include <map>
#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/prettywriter.h"  // 添加这个头文件
class XsonCollector {
public:
    static XsonCollector* getInstance();


    void put(const std::string& key, const std::string& value);
    void putFailed(const std::string& key);
    void putNotCollected(const std::string& key);
    std::string toString();
    void clear();

private:
    XsonCollector();
    ~XsonCollector();

    static XsonCollector* instance;
    rapidjson::Document doc;  // 使用 RapidJSON 的 Document
};

#endif