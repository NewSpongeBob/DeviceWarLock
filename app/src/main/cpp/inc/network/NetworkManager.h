#ifndef DEVICEWARLOCK_NETWORKMANAGER_H
#define DEVICEWARLOCK_NETWORKMANAGER_H

#include <string>

class NetworkManager {
public:
    static NetworkManager* getInstance();
    bool sendData(const std::string& encryptedData);

private:
    NetworkManager() = default;
    ~NetworkManager() = default;
    static NetworkManager* instance;
};

#endif //DEVICEWARLOCK_NETWORKMANAGER_H 