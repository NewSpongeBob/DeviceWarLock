// inc/collector/NativeCollector.h
#ifndef WARLOCK_NATIVECOLLECTOR_H
#define WARLOCK_NATIVECOLLECTOR_H

#include "../utils/allheader.h"
#include "ICollector.h"
#include "utils/XsonCollector.h"
#include "crypto/EncryptManager.h"

class NativeCollector {
public:
    static NativeCollector* getInstance();
    void startCollect(JNIEnv* env, jobject callback);
    bool isCollectComplete() const;
    std::string getCollectedInfo() ;
    void cleanup();

private:
    NativeCollector();
    ~NativeCollector();
    
    static void* collectThread(void* arg);
    void collect();
    void notifyComplete();
    void initCollectors();
    void encryptCollectedInfo();

    static NativeCollector* instance;
    pthread_t threadId;
    bool isComplete;
    std::string rawInfo;
    std::map<std::string, std::string> collectedInfo;
    std::vector<std::unique_ptr<ICollector>> collectors;
    std::string encryptedInfo;  // 存储加密后的信息
    
    // 线程同步
    pthread_mutex_t mutex;
    
    // JNI回调相关
    JavaVM* javaVM;
    jobject globalCallback;
};

#endif