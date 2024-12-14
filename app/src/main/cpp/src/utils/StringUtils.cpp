// utils/StringUtils.cpp
#include "../inc/utils/StringUtils.h"
#include <algorithm>

namespace utils {

    bool StringUtils::isEmpty(const std::string& str) {
        return str.empty() || str.find_first_not_of(" \t\n\r") == std::string::npos;
    }

    std::string StringUtils::trim(const std::string& str) {
        if (str.empty()) {
            return str;
        }

        size_t first = str.find_first_not_of(" \t\n\r");
        if (first == std::string::npos) {
            return "";
        }

        size_t last = str.find_last_not_of(" \t\n\r");
        return str.substr(first, (last - first + 1));
    }

    std::vector<std::string> StringUtils::split(const std::string& str, const std::string& delim) {
        std::vector<std::string> tokens;
        size_t prev = 0, pos = 0;
        do {
            pos = str.find(delim, prev);
            if (pos == std::string::npos) pos = str.length();
            std::string token = str.substr(prev, pos - prev);
            if (!token.empty()) tokens.push_back(token);
            prev = pos + delim.length();
        } while (pos < str.length() && prev < str.length());
        return tokens;
    }

    std::string StringUtils::join(const std::vector<std::string>& vec, const std::string& delim) {
        std::string result;
        for (size_t i = 0; i < vec.size(); ++i) {
            if (i > 0) result += delim;
            result += vec[i];
        }
        return result;
    }
}