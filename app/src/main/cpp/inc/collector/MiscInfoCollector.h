#ifndef WARLOCK_MISCINFOCOLLECTOR_H
#define WARLOCK_MISCINFOCOLLECTOR_H

#include "ICollector.h"
#include "../constants/Constants.h"
#include "../utils/XsonCollector.h"
#include "../inc/utils/SyscallUtils.h"
#include <sys/statfs.h>
#include <linux/magic.h>
#include <rapidjson/document.h>
#include <rapidjson/writer.h>
#include <rapidjson/stringbuffer.h>
#include <dlfcn.h>
#include <media/NdkMediaDrm.h>
#include <EGL/egl.h>
#include <fstream>

using namespace constants::fingerprint;
using namespace constants::path;

class MiscInfoCollector : public ICollector {
public:
    void collect(std::map<std::string, std::string>& info) override;
private:
    void collectServiceList();
    void collectStorageStats();
    void collectDrmId();
    void collectDirStats();
    void collectCpuInfo();
    void collectHardwareFeatures();
    void collectMemoryInfo();
};

#endif