#ifndef WARLOCK_MISCINFOCOLLECTOR_H
#define WARLOCK_MISCINFOCOLLECTOR_H

#include "ICollector.h"

class MiscInfoCollector : public ICollector {
public:
    void collect(std::map<std::string, std::string>& info) override;
};

#endif