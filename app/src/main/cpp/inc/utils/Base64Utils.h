#ifndef WARLOCK_BASE64UTILS_H
#define WARLOCK_BASE64UTILS_H

#include <string>

class Base64Utils {
public:
    static std::string Encode(const uint8_t* input, size_t length);
    static std::string Decode(const std::string& input);
};

#endif