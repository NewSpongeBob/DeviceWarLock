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
    bool StringUtils::contains(const std::string& str, const std::string& substr) {
        return str.find(substr) != std::string::npos;
    }
    bool StringUtils::containsManual(const char* str, const char* pattern) {
        if (!str || !pattern) return false;

        const char* s = str;
        while (*s) {
            const char* p = pattern;
            const char* current = s;

            while (*p && *current && *p == *current) {
                p++;
                current++;
            }

            if (!*p) return true;
            s++;
        }
        return false;
    }

    bool StringUtils::containsBM(const char* text, const char* pattern) {
        if (!text || !pattern) return false;

        int textLen = strlen(text);
        int patternLen = strlen(pattern);

        if (patternLen == 0) return false;

        int skip[256] = {0};
        for (int i = 0; i < 256; i++) {
            skip[i] = patternLen;
        }
        for (int i = 0; i < patternLen - 1; i++) {
            skip[static_cast<unsigned char>(pattern[i])] = patternLen - 1 - i;
        }

        int i = patternLen - 1;
        while (i < textLen) {
            int j = patternLen - 1;
            int k = i;

            while (j >= 0 && text[k] == pattern[j]) {
                j--;
                k--;
            }

            if (j < 0) return true;

            i += skip[static_cast<unsigned char>(text[i])];
        }
        return false;
    }

    bool StringUtils::containsBytes(const char* str, const char* pattern) {
        if (!str || !pattern) return false;

        size_t patternLen = strlen(pattern);
        size_t strLen = strlen(str);

        if (patternLen > strLen) return false;

        for (size_t i = 0; i <= strLen - patternLen; i++) {
            bool found = true;
            for (size_t j = 0; j < patternLen; j++) {
                if (str[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }

    bool StringUtils::containsSafe(const std::string& text, const std::string& pattern) {
        if (pattern.empty() || text.empty()) return false;

        const char* textCStr = text.c_str();
        const char* patternCStr = pattern.c_str();

        // 使用多种方法检测
        bool result1 = contains(text, pattern);           // 标准方法
        bool result2 = containsManual(textCStr, patternCStr);    // 手动查找
        bool result3 = containsBM(textCStr, patternCStr);        // Boyer-Moore
        bool result4 = containsBytes(textCStr, patternCStr);     // 字节比较

        // 至少两种方法检测到才认为是真实存在
        int count = result1 + result2 + result3 + result4;
        return count >= 2;
    }
}