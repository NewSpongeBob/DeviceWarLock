#ifndef WARLOCK_SYSTEMDETECTOR_H
#define WARLOCK_SYSTEMDETECTOR_H

#include "IDetector.h"
#include "../utils/allheader.h"
#include "../utils/LogUtils.h"
#include "../utils/DetectorUtils.h"
#include "../crypto/crypto.h"
#include <sys/system_properties.h>
#include <fstream>

class SystemDetector : public IDetector {
public:
    virtual ~SystemDetector() = default;
    void detect(JNIEnv* env, jobject callback) override;

private:
    static const std::string CHECK_SYSTEM;

    bool checkDmVerity();
    bool checkSystemPartition();
    std::string calculateSystemHash();
    bool checkAVB();
    std::string getSystemDetails();
};

#endif