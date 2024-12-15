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
// BasicInfoCollector.cpp
void BasicInfoCollector::getNetworkInfo() {
    bool success = false;
    rapidjson::Document doc;
    doc.SetObject();
    auto& allocator = doc.GetAllocator();
    rapidjson::Value networkInfo(rapidjson::kObjectType);

    if (android_get_device_api_level() >= 29) {  // Android 10及以上
        success = getNetworkInfoModern();
    } else {
        success = getNetworkInfoLegacy();
    }

    if (!success) {
        XsonCollector::getInstance()->putFailed(constants::fingerprint::NETWORK_INFO_ID);
        return;
    }
}

bool BasicInfoCollector::getNetworkInfoLegacy() {
    struct ifaddrs *ifap, *ifaptr;
    rapidjson::Document doc;
    doc.SetObject();
    auto& allocator = doc.GetAllocator();
    rapidjson::Value networkInfo(rapidjson::kObjectType);

    if (myGetifaddrs(&ifap) != 0) {
        return false;
    }

    bool hasValidInterface = false;
    for (ifaptr = ifap; ifaptr != nullptr; ifaptr = ifaptr->ifa_next) {
        if (ifaptr->ifa_addr != nullptr) {
            sa_family_t family = ifaptr->ifa_addr->sa_family;
            if (family == AF_PACKET) {
                auto *sockadd = (struct sockaddr_ll *) (ifaptr->ifa_addr);
                char mac[18] = {0};
                int len = 0;
                for (int i = 0; i < 6; i++) {
                    len += sprintf(mac + len, "%02X%s",
                                   sockadd->sll_addr[i], (i < 5 ? ":" : ""));
                }

                rapidjson::Value interfaceName(ifaptr->ifa_name, allocator);
                rapidjson::Value macAddr(mac, allocator);
                networkInfo.AddMember(interfaceName, macAddr, allocator);
                hasValidInterface = true;
            }
        }
    }
    freeifaddrs(ifap);

    if (!hasValidInterface) {
        return false;
    }

    rapidjson::StringBuffer buffer;
    rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
    doc.SetObject();
    doc.AddMember("s", 0, allocator);
    doc.AddMember("v", networkInfo, allocator);
    doc.Accept(writer);

    XsonCollector::getInstance()->put(
            constants::fingerprint::NETWORK_INFO_ID,
            buffer.GetString()
    );
    return true;
}

bool BasicInfoCollector::getNetworkInfoModern() {
    // 先尝试 netlink 方式
    if (tryNetlinkMethod()) {
        return true;
    }

    // netlink 失败则尝试 ioctl
    return tryIoctlMethod();
}

bool BasicInfoCollector::tryNetlinkMethod() {
    int sock = socket(AF_NETLINK, SOCK_RAW, NETLINK_ROUTE);
    if (sock < 0) {
        return false;
    }

    struct {
        struct nlmsghdr nh;
        struct ifinfomsg ifi;
        char attrbuf[512];
    } req;

    memset(&req, 0, sizeof(req));
    req.nh.nlmsg_len = NLMSG_LENGTH(sizeof(struct ifinfomsg));
    req.nh.nlmsg_type = RTM_GETLINK;
    req.nh.nlmsg_flags = NLM_F_REQUEST | NLM_F_DUMP;
    req.ifi.ifi_family = AF_UNSPEC;

    if (send(sock, &req, req.nh.nlmsg_len, 0) < 0) {
        close(sock);
        return false;
    }

    rapidjson::Document doc;
    doc.SetObject();
    auto& allocator = doc.GetAllocator();
    rapidjson::Value networkInfo(rapidjson::kObjectType);
    bool hasValidInterface = false;

    char buf[8192];
    while (true) {
        int len = recv(sock, buf, sizeof(buf), 0);
        if (len < 0) {
            break;
        }

        struct nlmsghdr* h = (struct nlmsghdr*)buf;
        for (; NLMSG_OK(h, len); h = NLMSG_NEXT(h, len)) {
            if (h->nlmsg_type == NLMSG_DONE) {
                goto done;
            }

            if (h->nlmsg_type == RTM_NEWLINK) {
                struct ifinfomsg* ifi = (struct ifinfomsg*)NLMSG_DATA(h);
                struct rtattr* rta = IFLA_RTA(ifi);
                int rtl = IFLA_PAYLOAD(h);

                char ifname[IF_NAMESIZE] = {0};
                char mac[18] = {0};
                bool got_mac = false;

                for (; RTA_OK(rta, rtl); rta = RTA_NEXT(rta, rtl)) {
                    if (rta->rta_type == IFLA_ADDRESS) {
                        unsigned char* data = (unsigned char*)RTA_DATA(rta);
                        sprintf(mac, "%02X:%02X:%02X:%02X:%02X:%02X",
                                data[0], data[1], data[2],
                                data[3], data[4], data[5]);
                        got_mac = true;
                    } else if (rta->rta_type == IFLA_IFNAME) {
                        strncpy(ifname, (char*)RTA_DATA(rta), IF_NAMESIZE - 1);
                    }
                }

                if (got_mac && strlen(ifname) > 0) {
                    rapidjson::Value interfaceName(ifname, allocator);
                    rapidjson::Value macAddr(mac, allocator);
                    networkInfo.AddMember(interfaceName, macAddr, allocator);
                    hasValidInterface = true;
                }
            }
        }
    }

    done:
    close(sock);

    if (!hasValidInterface) {
        return false;
    }

    rapidjson::StringBuffer buffer;
    rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
    doc.AddMember("s", 0, allocator);
    doc.AddMember("v", networkInfo, allocator);
    doc.Accept(writer);

    XsonCollector::getInstance()->put(
            constants::fingerprint::NETWORK_INFO_ID,
            buffer.GetString()
    );
    return true;
}

bool BasicInfoCollector::tryIoctlMethod() {
    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0) {
        return false;
    }

    rapidjson::Document doc;
    doc.SetObject();
    auto& allocator = doc.GetAllocator();
    rapidjson::Value networkInfo(rapidjson::kObjectType);
    bool hasValidInterface = false;

    // 尝试常见的网卡接口
    const char* interfaces[] = {"wlan0", "eth0", "rmnet0"};
    for (const char* ifname : interfaces) {
        struct ifreq ifr;
        memset(&ifr, 0, sizeof(ifr));
        strcpy(ifr.ifr_name, ifname);

        if (ioctl(sock, SIOCGIFHWADDR, &ifr) >= 0) {
            char mac[18];
            sprintf(mac, "%02X:%02X:%02X:%02X:%02X:%02X",
                    (unsigned char)ifr.ifr_hwaddr.sa_data[0],
                    (unsigned char)ifr.ifr_hwaddr.sa_data[1],
                    (unsigned char)ifr.ifr_hwaddr.sa_data[2],
                    (unsigned char)ifr.ifr_hwaddr.sa_data[3],
                    (unsigned char)ifr.ifr_hwaddr.sa_data[4],
                    (unsigned char)ifr.ifr_hwaddr.sa_data[5]);

            rapidjson::Value interfaceName(ifname, allocator);
            rapidjson::Value macAddr(mac, allocator);
            networkInfo.AddMember(interfaceName, macAddr, allocator);
            hasValidInterface = true;
        }
    }

    close(sock);

    if (!hasValidInterface) {
        return false;
    }

    rapidjson::StringBuffer buffer;
    rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
    doc.AddMember("s", 0, allocator);
    doc.AddMember("v", networkInfo, allocator);
    doc.Accept(writer);

    XsonCollector::getInstance()->put(
            constants::fingerprint::NETWORK_INFO_ID,
            buffer.GetString()
    );
    return true;
}
void BasicInfoCollector::collectNetworkInterfaces() {
    FILE* fp = popen("ip a", "r");
    if (!fp) {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::NETWORK_INTERFACES_ID
        );
        return;
    }

    std::string result = "{";
    char buffer[4096];
    bool firstInterface = true;
    std::string currentInterface;
    std::string state, ip, mac;

    const std::string targetInterfaces[] = {"lo", "dummy0", "wlan0", "wlan1", "eth0", "rmnet0"};

    while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
        std::string line(buffer);

        // 新接口的开始
        if (line[0] >= '0' && line[0] <= '9' && line.find(": ") != std::string::npos) {
            // 如果有之前的接口信息，保存它
            if (!currentInterface.empty()) {
                for (const auto& target : targetInterfaces) {
                    if (currentInterface == target) {
                        if (!firstInterface) {
                            result += ",";
                        }
                        firstInterface = false;
                        result += "\"" + currentInterface + "\":{";
                        result += "\"state\":\"" + state + "\"";
                        if (!ip.empty()) {
                            result += ",\"ip\":\"" + ip + "\"";
                        }
                        if (!mac.empty()) {
                            result += ",\"mac\":\"" + mac + "\"";
                        }
                        result += "}";
                        break;
                    }
                }
            }

            // 重置新接口的信息
            currentInterface = "";
            state = "DOWN";  // 默认状态
            ip = "";
            mac = "";

            // 提取新接口名称和状态
            size_t nameStart = line.find(": ") + 2;
            size_t nameEnd = line.find(":", nameStart);
            if (nameEnd != std::string::npos) {
                currentInterface = line.substr(nameStart, nameEnd - nameStart);
                // 提取状态信息
                size_t statePos = line.find("state ");
                if (statePos != std::string::npos) {
                    statePos += 6;
                    size_t stateEnd = line.find(' ', statePos);
                    if (stateEnd != std::string::npos) {
                        state = line.substr(statePos, stateEnd - statePos);
                    }
                }
            }
        }
            // 提取MAC地址
        else if (!currentInterface.empty() && line.find("link/ether") != std::string::npos) {
            size_t macStart = line.find("link/ether") + 11;
            size_t macEnd = line.find(' ', macStart);
            if (macEnd != std::string::npos) {
                mac = line.substr(macStart, macEnd - macStart);
            }
        }
            // 提取IP地址
        else if (!currentInterface.empty() && line.find("inet ") != std::string::npos) {
            size_t ipStart = line.find("inet ") + 5;
            size_t ipEnd = line.find('/', ipStart);
            if (ipEnd != std::string::npos) {
                ip = line.substr(ipStart, ipEnd - ipStart);
            }
        }
    }

    // 处理最后一个接口
    if (!currentInterface.empty()) {
        for (const auto& target : targetInterfaces) {
            if (currentInterface == target) {
                if (!firstInterface) {
                    result += ",";
                }
                result += "\"" + currentInterface + "\":{";
                result += "\"state\":\"" + state + "\"";
                if (!ip.empty()) {
                    result += ",\"ip\":\"" + ip + "\"";
                }
                if (!mac.empty()) {
                    result += ",\"mac\":\"" + mac + "\"";
                }
                result += "}";
                break;
            }
        }
    }

    result += "}";
    pclose(fp);

    if (result.length() > 2) {
        XsonCollector::getInstance()->put(
                constants::fingerprint::NETWORK_INTERFACES_ID,
                result
        );
    } else {
        XsonCollector::getInstance()->putFailed(
                constants::fingerprint::NETWORK_INTERFACES_ID
        );
    }
}
void BasicInfoCollector::collect(std::map<std::string, std::string>& info) {
        getDrmId(info);
        getNetworkInfo();
    collectNetworkInterfaces(); //a15
}