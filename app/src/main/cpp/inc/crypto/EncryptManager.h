#ifndef DEVICEWARLOCK_ENCRYPTMANAGER_H
#define DEVICEWARLOCK_ENCRYPTMANAGER_H

#include <string>
#include <vector>
#include <android/log.h>
#include "crypto.h"

class EncryptManager {
public:
    static EncryptManager* getInstance();
    std::string encryptData(const std::string& data);

private:
    EncryptManager();
    ~EncryptManager();
    static EncryptManager* instance;
    
    // 加密相关
    std::string generateUUID();
    std::string generateTimestamp();
    std::string generateSalt(const std::string& uuid, const std::string& timestamp);
    std::string generateKey(const std::string& uuid, const std::string& timestamp, const std::string& salt);
    std::string generateAESKey(const std::string& salt);
    unsigned char generateSum(const std::string& salt);
    std::string insertKey(const std::string& base64Result, const std::string& key, unsigned char sum);
    
    // 加密上下文
    MD5_CTX md5_ctx;
    AES_CTX aes_ctx;
};

#endif //DEVICEWARLOCK_ENCRYPTMANAGER_H 