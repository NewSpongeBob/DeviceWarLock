
#ifndef WARLOCK_STRINGUTILS_H
#define WARLOCK_STRINGUTILS_H

#include <string>
#include <vector>

namespace utils {
    class StringUtils {
    public:
        static bool isEmpty(const std::string& str);
        static std::string trim(const std::string& str);
        static std::vector<std::string> split(const std::string& str, const std::string& delim);
        static std::string join(const std::vector<std::string>& vec, const std::string& delim);
    };
}

#endif