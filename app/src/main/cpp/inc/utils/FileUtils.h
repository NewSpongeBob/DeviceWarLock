
#ifndef WARLOCK_FILEUTILS_H
#define WARLOCK_FILEUTILS_H

#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <sys/syscall.h>

namespace utils {
    class FileUtils {
    public:
        static int openFile(const char* path, int flags);
        static ssize_t readFile(int fd, void* buf, size_t count);
        static int closeFile(int fd);
        static std::string readFileAsString(const char* path);
    private:
        static long syscall(long number, ...);
    };
}

#endif