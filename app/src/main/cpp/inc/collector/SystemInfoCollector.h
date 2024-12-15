#ifndef WARLOCK_SYSTEMINFOCOLLECTOR_H
#define WARLOCK_SYSTEMINFOCOLLECTOR_H

#include "ICollector.h"
#include "../constants/Constants.h"
#include "../utils/XsonCollector.h"
#include "../utils/allheader.h"
#include "../constants/Constants.h"
using namespace constants::fingerprint;
using namespace constants::path;
extern "C" {
__attribute__((always_inline)) long raw_syscall(long number, ...);
}
class SystemInfoCollector : public ICollector {
public:
    void collect(std::map<std::string, std::string>& info) override;
    void collectProps();
private:
    std::string readBuildProp(const char* path);
    bool readFileContent(int fd, std::string& content);
    void collectBuildPropFile();
    void readSystemFile(const char* path, const char* fingerprintId);
    void collectDeviceID();
    void collectSystemVersion();
    std::string extractVersion(const std::string& input);
    void collectGetProp();
};

#endif