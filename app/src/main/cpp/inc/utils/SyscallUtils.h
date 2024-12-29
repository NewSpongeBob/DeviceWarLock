
#ifndef WARLOCK_SYSCALLUTILS_H
#define WARLOCK_SYSCALLUTILS_H

#include <sys/syscall.h>
#include "allheader.h"
namespace utils {
    class SyscallUtils {
    public:
        static long syscall(long number, ...);
    private:
        #if defined(__aarch64__)
        static long syscall_aarch64(long number, long arg1, long arg2, long arg3);
        #elif defined(__arm__)
        static long syscall_arm(long number, long arg1, long arg2, long arg3);
        #elif defined(__x86_64__)
        static long syscall_x86_64(long number, long arg1, long arg2, long arg3);
        #elif defined(__i386__)
        static long syscall_x86(long number, long arg1, long arg2, long arg3);
        #endif
    };
}

#endif