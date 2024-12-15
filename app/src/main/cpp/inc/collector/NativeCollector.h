// inc/collector/NativeCollector.h
#ifndef WARLOCK_NATIVECOLLECTOR_H
#define WARLOCK_NATIVECOLLECTOR_H

#include "../utils/allheader.h"
#include "ICollector.h"
#include "utils/XsonCollector.h"
class NativeCollector {
public:
    static NativeCollector* getInstance();
    void startCollect(JNIEnv* env, jobject callback);
    bool isCollectComplete() const;
    std::string getCollectedInfo() const;
    void cleanup();

private:
    NativeCollector();
    ~NativeCollector();
    
    static void* collectThread(void* arg);
    void collect();
    void notifyComplete();
    void initCollectors();

    static NativeCollector* instance;
    pthread_t threadId;
    bool isComplete;
    std::map<std::string, std::string> collectedInfo;
    std::vector<std::unique_ptr<ICollector>> collectors;
    
    // 线程同步
    pthread_mutex_t mutex;
    
    // JNI回调相关
    JavaVM* javaVM;
    jobject globalCallback;
};

#endif