#include "../inc/collector/NativeCollector.h"
#include "../inc/collector/SystemInfoCollector.h"
#include "../inc/collector/BasicInfoCollector.h"
#include "../inc/collector/MiscInfoCollector.h"
#include "../inc/utils/LogUtils.h"

NativeCollector* NativeCollector::instance = nullptr;

NativeCollector::NativeCollector() : isComplete(false), javaVM(nullptr), globalCallback(nullptr) {
    pthread_mutex_init(&mutex, nullptr);
    initCollectors();
}

NativeCollector::~NativeCollector() {
    cleanup();
    pthread_mutex_destroy(&mutex);
}

void NativeCollector::initCollectors() {
    collectors.push_back(std::make_unique<SystemInfoCollector>());
    collectors.push_back(std::make_unique<BasicInfoCollector>());
    collectors.push_back(std::make_unique<MiscInfoCollector>());
}

NativeCollector* NativeCollector::getInstance() {
    if (instance == nullptr) {
        instance = new NativeCollector();
    }
    return instance;
}

void NativeCollector::startCollect(JNIEnv* env, jobject callback) {
    pthread_mutex_lock(&mutex);
    if (javaVM == nullptr) {
        env->GetJavaVM(&javaVM);
        globalCallback = env->NewGlobalRef(callback);
    }
    pthread_mutex_unlock(&mutex);

    pthread_create(&threadId, nullptr, collectThread, this);
}

void* NativeCollector::collectThread(void* arg) {
    auto* collector = static_cast<NativeCollector*>(arg);
    collector->collect();
    collector->notifyComplete();
    return nullptr;
}

void NativeCollector::collect() {
    try {
        pthread_mutex_lock(&mutex);
        collectedInfo.clear();
        
        for (const auto& collector : collectors) {
            collector->collect(collectedInfo);
        }
        
        // 收集完成后立即进行加密
        encryptCollectedInfo();
        
        pthread_mutex_unlock(&mutex);
        LOGI("Native fingerprint collection and encryption completed");
    } catch (const std::exception& e) {
        pthread_mutex_unlock(&mutex);
        LOGE("Error during native collection: %s", e.what());
    }
}

void NativeCollector::encryptCollectedInfo() {
    rawInfo = XsonCollector::getInstance()->toString();
    encryptedInfo = EncryptManager::getInstance()->encryptData(rawInfo);
    LOGI("Encrypted info: %s", encryptedInfo.c_str());
}

void NativeCollector::notifyComplete() {
    pthread_mutex_lock(&mutex);
    isComplete = true;
    
    if (javaVM && globalCallback) {
        JNIEnv* env;
        javaVM->AttachCurrentThread(&env, nullptr);
        
        jclass callbackClass = env->GetObjectClass(globalCallback);
        jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onNativeCollectComplete", "()V");
        
        env->CallVoidMethod(globalCallback, onCompleteMethod);
        
        javaVM->DetachCurrentThread();
    }
    
    pthread_mutex_unlock(&mutex);
}

bool NativeCollector::isCollectComplete() const {
    return isComplete;
}
//前端指纹展示需要为加密过的数据进行支撑，所以第一次调用该方法返回原始数据，第二次调用该方法则为上报服务器，此时需要加密数据
std::string NativeCollector::getCollectedInfo()  {
    if (!rawInfo.empty()) {
        // 第一次调用，返回原始数据，然后清空rawInfo
        std::string temp = rawInfo;
        rawInfo = "";
        return temp;
    } else {
        // 第二次调用，返回加密数据
        return encryptedInfo;
    }
}


void NativeCollector::cleanup() {
    pthread_mutex_lock(&mutex);
    
    if (javaVM && globalCallback) {
        JNIEnv* env;
        javaVM->AttachCurrentThread(&env, nullptr);
        env->DeleteGlobalRef(globalCallback);
        javaVM->DetachCurrentThread();
    }
    
    javaVM = nullptr;
    globalCallback = nullptr;
    isComplete = false;
    collectedInfo.clear();
    collectors.clear();
    
    pthread_mutex_unlock(&mutex);
}