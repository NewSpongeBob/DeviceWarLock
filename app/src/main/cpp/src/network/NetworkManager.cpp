#include "network/NetworkManager.h"
#include <android/log.h>

#define TAG "NetworkManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

NetworkManager* NetworkManager::instance = nullptr;

NetworkManager* NetworkManager::getInstance() {
    if (instance == nullptr) {
        instance = new NetworkManager();
    }
    return instance;
}

bool NetworkManager::sendData(const std::string& encryptedData) {
    // TODO: 实现实际的网络请求逻辑
    LOGI("Ready to send encrypted data: %s", encryptedData.c_str());
    return true;
} 