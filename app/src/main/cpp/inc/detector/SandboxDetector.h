//
// Created by 17267 on 2024-12-17.
//

#ifndef WARLOCK_SANDBOXDETECTOR_H
#define WARLOCK_SANDBOXDETECTOR_H
#include <jni.h>
#include <string>
#include <dirent.h>
#include <unistd.h>
#include <sys/types.h>
#include <dlfcn.h>
#include "../utils/LogUtils.h"
class SandboxDetector {
private:
    typedef DIR* (*OpenDir)(const char*);
    typedef struct dirent* (*ReadDir)(DIR*);

    static std::string getLibcPath();
    static void getNameByPid(pid_t pid, char* buff);
    static void* replaceSecInsns(const char* libPath, const char* symbol);
    static void notifyDetection(JNIEnv* env, jobject thiz, const std::string& details);
    static void checkProcessByProc(JNIEnv* env, jobject thiz);
public:
    static void checkSandbox(JNIEnv* env, jobject thiz);
    static void checkProcessByPs(JNIEnv* env, jobject thiz);

};

#endif //WARLOCK_SANDBOXDETECTOR_H
