#ifndef WARLOCK_BASICINFOCOLLECTOR_H
#define WARLOCK_BASICINFOCOLLECTOR_H

#include "ICollector.h"
#include <media/NdkMediaDrm.h>

#include "../inc/utils/Base64Utils.h"
#include "../inc/utils/LogUtils.h"
#include "../inc/constants/Constants.h"
#include "utils/XsonCollector.h"
#include <media/NdkMediaDrm.h>
#include "../../src/netlink/bionic_netlink.h"
#include "../../src/netlink/ifaddrs.h"
#include "../utils/allheader.h"
#include <net/if.h>
class BasicInfoCollector : public ICollector {
public:
    ~BasicInfoCollector() override = default;
    void collect(std::map<std::string, std::string>& info) override;

private:
    void getDrmId(std::map<std::string, std::string>& info);
    void getNetworkInfo();
    bool getNetworkInfoLegacy();  // Android 10以下
    bool getNetworkInfoModern();  // Android 10及以上
    bool tryIoctlMethod();
    bool tryNetlinkMethod();
    void collectNetworkInterfaces();
};

#endif