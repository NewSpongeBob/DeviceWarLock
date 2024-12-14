#ifndef WARLOCK_SYSTEMINFOCOLLECTOR_H
#define WARLOCK_SYSTEMINFOCOLLECTOR_H

#include "ICollector.h"

class SystemInfoCollector : public ICollector {
public:
    void collect(std::map<std::string, std::string>& info) override;
};

#endif