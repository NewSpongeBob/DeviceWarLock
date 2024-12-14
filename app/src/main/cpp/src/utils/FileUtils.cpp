
#include "../inc/utils/FileUtils.h"
#include "../inc/utils/LogUtils.h"
#include <stdarg.h>
#include <errno.h>
#include "../inc/utils/SyscallUtils.h"
namespace utils {

    long FileUtils::syscall(long number, ...) {
        va_list args;
        va_start(args, number);
        long arg1 = va_arg(args, long);
        long arg2 = va_arg(args, long);
        long arg3 = va_arg(args, long);
        va_end(args);
        
        long result = SyscallUtils::syscall(number, arg1, arg2, arg3);
        return result;
    }

    int FileUtils::openFile(const char* path, int flags) {
        return (int)syscall(__NR_openat, AT_FDCWD, (long)path, flags);
    }

    ssize_t FileUtils::readFile(int fd, void* buf, size_t count) {
        return syscall(__NR_read, fd, (long)buf, count);
    }

    int FileUtils::closeFile(int fd) {
        return (int)syscall(__NR_close, fd, 0, 0);
    }

    std::string FileUtils::readFileAsString(const char* path) {
        std::string content;
        int fd = openFile(path, O_RDONLY);
        if (fd < 0) {
            LOGE("Failed to open file: %s, errno: %d", path, errno);
            return content;
        }

        char buffer[4096];
        ssize_t bytesRead;
        while ((bytesRead = readFile(fd, buffer, sizeof(buffer))) > 0) {
            content.append(buffer, bytesRead);
        }

        closeFile(fd);
        return content;
    }
}