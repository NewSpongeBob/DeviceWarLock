#include <android/log.h>
#include "../../inc/crypto/EncryptManager.h"
#include <sstream>
#include <iomanip>
#include <chrono>
#include <random>
#include <vector>
#include <cstring>
#include <cstdlib>
#include <ctime>
#define TAG "EncryptManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

EncryptManager* EncryptManager::instance = nullptr;

EncryptManager::EncryptManager() {
    // 初始化MD5上下文
    MD5Init(&md5_ctx);
}

EncryptManager::~EncryptManager() {
    // 清理工作（如果需要）
}

EncryptManager* EncryptManager::getInstance() {
    if (instance == nullptr) {
        instance = new EncryptManager();
    }
    return instance;
}

std::string EncryptManager::generateUUID() {
    static std::random_device rd;
    static std::mt19937 gen(rd());
    static std::uniform_int_distribution<> dis(0, 15);
    static const char* digits = "0123456789abcdef";

    std::string uuid;
    uuid.reserve(36);

    for (int i = 0; i < 36; i++) {
        if (i == 8 || i == 13 || i == 18 || i == 23) {
            uuid += '-';
        } else {
            uuid += digits[dis(gen)];
        }
    }

    return uuid;
}

std::string EncryptManager::generateTimestamp() {
    auto now = std::chrono::system_clock::now();
    auto duration = now.time_since_epoch();
    auto seconds = std::chrono::duration_cast<std::chrono::seconds>(duration).count();
    return std::to_string(seconds);
}

std::string EncryptManager::generateSalt(const std::string& uuid, const std::string& timestamp) {
    std::string combined = uuid + timestamp;
    
    // 使用魔改的MD5
    MD5Init(&md5_ctx);
    MD5Update(&md5_ctx, (const uint8_t*)combined.c_str(), combined.length());
    
    uint8_t digest[16];
    MD5Final(digest, &md5_ctx);
    
    // 将MD5结果转换为十六进制字符串
    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    for (int i = 0; i < 16; i++) {
        ss << std::setw(2) << static_cast<int>(digest[i]);
    }
    
    return ss.str();
}

std::string EncryptManager::generateKey(const std::string& uuid, const std::string& timestamp, const std::string& salt) {
    return timestamp + uuid + salt;
}

std::string EncryptManager::generateAESKey(const std::string& salt) {
    // 1. 生成 key1：每间隔一位取16位
    std::string key1;
    for (size_t i = 0; i < salt.length() && key1.length() < 16; i += 2) {
        key1 += salt[i];
    }
    // 补齐 key1 到16位
    while (key1.length() < 16) {
        key1 += '0';
    }
    
    // 2. 生成 key2：剩余位倒序
    std::string key2;
    for (size_t i = 1; i < salt.length() && key2.length() < 16; i += 2) {
        key2 = salt[i] + key2; // 倒序添加
    }
    // 补齐 key2 到16位
    while (key2.length() < 16) {
        key2 = '0' + key2;
    }
    
    // 3. key1 和 key2 每一位异或生成最终的 AES key
    std::string aesKey;
    for (size_t i = 0; i < 16; i++) {
        aesKey += static_cast<char>(key1[i] ^ key2[i]);
    }
    
    LOGI("Key1: %s", key1.c_str());
    LOGI("Key2: %s", key2.c_str());
    
    return aesKey;
}

unsigned char EncryptManager::generateSum(const std::string& salt) {
    // 获取 timestamp 的最后一位
    std::string timestamp = generateTimestamp();
    char timestampLastChar = timestamp[timestamp.length() - 1];

    // 获取 salt 的最后一位
    char saltLastChar = salt[salt.length() - 1];

    // 进行异或运算
    char result = timestampLastChar ^ saltLastChar;

    // 如果结果为负数，生成1-50的随机数
    if (result < 0) {
        // 使用当前时间作为种子
        std::srand(std::time(nullptr));
        // 生成1-50的随机数
        result = (std::rand() % 50) + 1;
    }

    return static_cast<unsigned char>(result);
}

std::string EncryptManager::insertKey(const std::string& base64Result, const std::string& key, unsigned char sum) {
    std::string result = base64Result;
    size_t step = sum + 1; // 间隔值
    size_t currentPos = sum % result.length(); // 初始位置
    
    // 按照间隔值插入 key 的每个字符
    for (char c : key) {
        result.insert(currentPos, 1, c);
        currentPos = (currentPos + step) % (result.length() + 1); // 移动到下一个插入位置
    }
    
    return result + "+" + std::to_string(static_cast<int>(sum));
}

std::string EncryptManager::encryptData(const std::string& data) {
    // 1. 生成基础值
    std::string timestamp = generateTimestamp();
    std::string uuid = generateUUID();
    std::string salt = generateSalt(uuid, timestamp);
    std::string key = generateKey(uuid, timestamp, salt);
    
    LOGI("Timestamp: %s", timestamp.c_str());
    LOGI("UUID: %s", uuid.c_str());
    LOGI("Salt: %s", salt.c_str());
    LOGI("Key: %s", key.c_str());
    
    // 2. 生成AES密钥
    std::string aesKey = generateAESKey(salt);
    LOGI("AES Key: %s", aesKey.c_str());
    
    // 3. 初始化AES上下文并加密
    AES_init(&aes_ctx, (const uint8_t*)aesKey.c_str());
    
    // 计算填充后的长度
    size_t padded_len = ((data.length() + AES_BLOCK_SIZE - 1) / AES_BLOCK_SIZE) * AES_BLOCK_SIZE;
    std::vector<uint8_t> input_data(padded_len, 0);
    std::vector<uint8_t> encrypted(padded_len, 0);
    
    // 复制数据并填充
    memcpy(input_data.data(), data.c_str(), data.length());
    uint8_t pad_value = padded_len - data.length();
    for (size_t i = data.length(); i < padded_len; i++) {
        input_data[i] = pad_value;
    }
    
    // 逐块加密
    for (size_t i = 0; i < padded_len; i += AES_BLOCK_SIZE) {
        AES_encrypt_block(&aes_ctx, &input_data[i], &encrypted[i]);
    }
    
    // 4. Base64编码
    size_t base64_len;
    char* base64_result = BASE64_encode_alloc(encrypted.data(), padded_len, &base64_len);
    
    std::string base64String(base64_result);
    delete[] base64_result;
    
    LOGI("Base64 Result: %s", base64String.c_str());
    
    // 5. 生成sum并插入key
    unsigned char sum = generateSum(salt);
    LOGI("Sum: %d", sum);
    
    // 6. 最终加密结果
    std::string finalResult = insertKey(base64String, key, sum);
    LOGI("Final Result: %s", finalResult.c_str());
    
    return finalResult;
} 