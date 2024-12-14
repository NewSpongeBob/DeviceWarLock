
#ifndef WARLOCK_COMMANDUTILS_H
#define WARLOCK_COMMANDUTILS_H

#include <string>

namespace utils {
    class CommandUtils {
    public:
        static std::string execCommand(const char* cmd);
    private:
        static long syscall(long number, ...);
    };
}

#endif