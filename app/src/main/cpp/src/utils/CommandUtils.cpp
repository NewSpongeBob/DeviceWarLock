#include "../inc/utils/CommandUtils.h"
#include "../inc/utils/LogUtils.h"
#include <stdarg.h>
#include <sys/wait.h>
#include "../inc/utils/SyscallUtils.h"

namespace utils {

    long CommandUtils::syscall(long number, ...) {
        va_list args;
        va_start(args, number);
        long arg1 = va_arg(args, long);
        long arg2 = va_arg(args, long);
        long arg3 = va_arg(args, long);
        va_end(args);
        
        long result = SyscallUtils::syscall(number, arg1, arg2, arg3);
        return result;
    }

    std::string CommandUtils::execCommand(const char* cmd) {
        std::string result;
        FILE* pipe = popen(cmd, "r");
        if (!pipe) {
            LOGE("Failed to execute command: %s", cmd);
            return result;
        }

        char buffer[128];
        while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
            result += buffer;
        }

        pclose(pipe);
        return result;
    }
}