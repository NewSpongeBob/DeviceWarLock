// src/collector/BasicInfoCollector.cpp
#include "../inc/collector/BasicInfoCollector.h"


using namespace constants;

void BasicInfoCollector::getDrmId(std::map<std::string, std::string>& info) {
    try {
        AMediaDrm* mediaDrm = AMediaDrm_createByUUID(drm::WIDEVINE_UUID);
        if (!mediaDrm) {
            LOGE("Failed to create MediaDrm instance");
            XsonCollector::getInstance()->putFailed(fingerprint::KEY_DRM_ID);
            return;
        }

        AMediaDrmByteArray deviceId;
        media_status_t status = AMediaDrm_getPropertyByteArray(
            mediaDrm,
            PROPERTY_DEVICE_UNIQUE_ID,
            &deviceId
        );

        if (status == AMEDIA_OK) {
            std::string result = Base64Utils::Encode(
                reinterpret_cast<const uint8_t*>(deviceId.ptr),
                deviceId.length
            );
            XsonCollector::getInstance()->put(fingerprint::KEY_DRM_ID, result);
        } else {
            LOGE("Failed to get DRM device ID, status: %d", status);
            XsonCollector::getInstance()->putFailed(fingerprint::KEY_DRM_ID);
        }

        AMediaDrm_release(mediaDrm);
    } catch (const std::exception& e) {
        LOGE("Error collecting DRM ID: %s", e.what());
        XsonCollector::getInstance()->putFailed(fingerprint::KEY_DRM_ID);
    }
}

void BasicInfoCollector::collect(std::map<std::string, std::string>& info) {
        getDrmId(info);
}