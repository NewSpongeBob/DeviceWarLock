// inc/collector/ICollector.h
#ifndef WARLOCK_ICOLLECTOR_H
#define WARLOCK_ICOLLECTOR_H

#include "../utils/allheader.h"

class ICollector {
public:
    virtual ~ICollector() = default;
    virtual void collect(std::map<std::string, std::string>& info) = 0;
};

#endif