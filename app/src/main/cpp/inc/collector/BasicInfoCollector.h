#ifndef WARLOCK_BASICINFOCOLLECTOR_H
#define WARLOCK_BASICINFOCOLLECTOR_H

#include "ICollector.h"
#include <media/NdkMediaDrm.h>
#include <string>
#include "../inc/utils/Base64Utils.h"
#include "../inc/utils/LogUtils.h"
#include "../inc/constants/Constants.h"
#include "utils/XsonCollector.h"
#include <media/NdkMediaDrm.h>
class BasicInfoCollector : public ICollector {
public:
    ~BasicInfoCollector() override = default;
    void collect(std::map<std::string, std::string>& info) override;

private:
    void getDrmId(std::map<std::string, std::string>& info);
};

#endif