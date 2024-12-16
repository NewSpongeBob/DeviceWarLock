
#include "../inc/utils/SyscallUtils.h"
#include "../inc/utils/LogUtils.h"
#include <stdarg.h>

namespace utils {

    #if defined(__aarch64__)
    long SyscallUtils::syscall_aarch64(long number, long arg1, long arg2, long arg3) {
        long result;
        __asm__ volatile(
            "mov x8, %1\n"    // 系统调用号
            "mov x0, %2\n"    // 第一个参数
            "mov x1, %3\n"    // 第二个参数
            "mov x2, %4\n"    // 第三个参数
            "svc #0\n"        // 触发系统调用
            "mov %0, x0"      // 获取返回值
            : "=r"(result)
            : "r"(number), "r"(arg1), "r"(arg2), "r"(arg3)
            : "x0", "x1", "x2", "x8"
        );
        return result;
    }
    #elif defined(__arm__)
    long SyscallUtils::syscall_arm(long number, long arg1, long arg2, long arg3) {
        long result;
        __asm__ volatile(
            "mov r7, %1\n"    // 系统调用号
            "mov r0, %2\n"    // 第一个参数
            "mov r1, %3\n"    // 第二个参数
            "mov r2, %4\n"    // 第三个参数
            "swi #0\n"        // 触发系统调用
            "mov %0, r0"      // 获取返回值
            : "=r"(result)
            : "r"(number), "r"(arg1), "r"(arg2), "r"(arg3)
            : "r0", "r1", "r2", "r7"
        );
        return result;
    }
    #elif defined(__x86_64__)
    long SyscallUtils::syscall_x86_64(long number, long arg1, long arg2, long arg3) {
        long result;
        __asm__ volatile(
            "mov %1, %%rax\n"    // 系统调用号
            "mov %2, %%rdi\n"    // 第一个参数
            "mov %3, %%rsi\n"    // 第二个参数
            "mov %4, %%rdx\n"    // 第三个参数
            "syscall\n"          // 触发系统调用
            "mov %%rax, %0"      // 获取返回值
            : "=r"(result)
            : "r"(number), "r"(arg1), "r"(arg2), "r"(arg3)
            : "rax", "rdi", "rsi", "rdx"
        );
        return result;
    }
    #elif defined(__i386__)
    long SyscallUtils::syscall_x86(long number, long arg1, long arg2, long arg3) {
        long result;
        __asm__ volatile(
                "pushl %%ebx\n"      // 保存ebx的值，因为它是PIC寄存器
                "movl %2, %%ebx\n"   // 加载第一个参数到ebx
                "movl %1, %%eax\n"   // 系统调用号到eax
                "movl %3, %%ecx\n"   // 第二个参数到ecx
                "movl %4, %%edx\n"   // 第三个参数到edx
                "int $0x80\n"        // 触发系统调用
                "popl %%ebx\n"       // 恢复ebx的值
                : "=a"(result)       // 输出：eax到result
                : "g"(number),       // 输入：系统调用号
        "r"(arg1),         // 输入：第一个参数
        "r"(arg2),         // 输入：第二个参数
        "r"(arg3)          // 输入：第三个参数
                : "ecx", "edx"       // 告诉编译器这些寄存器会被修改
                );
        return result;
    }
    #endif

    long SyscallUtils::syscall(long number, ...) {
        va_list args;
        va_start(args, number);
        long arg1 = va_arg(args, long);
        long arg2 = va_arg(args, long);
        long arg3 = va_arg(args, long);
        va_end(args);

        #if defined(__aarch64__)
        return syscall_aarch64(number, arg1, arg2, arg3);
        #elif defined(__arm__)
        return syscall_arm(number, arg1, arg2, arg3);
        #elif defined(__x86_64__)
        return syscall_x86_64(number, arg1, arg2, arg3);
        #elif defined(__i386__)
        return syscall_x86(number, arg1, arg2, arg3);
        #else
        LOGE("Unsupported architecture");
        return -1;
        #endif
    }
}