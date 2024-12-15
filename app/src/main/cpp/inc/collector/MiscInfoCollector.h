#ifndef WARLOCK_MISCINFOCOLLECTOR_H
#define WARLOCK_MISCINFOCOLLECTOR_H

#include "ICollector.h"
#include "../constants/Constants.h"
#include "../utils/XsonCollector.h"
using namespace constants::fingerprint;
using namespace constants::path;
class MiscInfoCollector : public ICollector {
public:
    void collect(std::map<std::string, std::string>& info) override;
private:
    void collectServiceList();
};

#endif