
#ifndef WARLOCK_CONSTANTS_H
#define WARLOCK_CONSTANTS_H

#include <cstdint>

namespace constants {
    // DRM相关常量
    namespace drm {
        // Widevine UUID
        const uint8_t WIDEVINE_UUID[] = {
            0xed, 0xef, 0x8b, 0xa9, 0x79, 0xd6, 0x4a, 0xce,
            0xa3, 0xc8, 0x27, 0xdc, 0xd5, 0x1d, 0x21, 0xed
        };
        
        // DRM属性名

    }

    // 指纹Key常量
    namespace fingerprint {
        // 基础信息
        const char* const KEY_DRM_ID = "n1";
        // ... 其他指纹key
    }

    // 文件路径常量
    namespace path {
        const char* const BUILD_PROP = "/system/build.prop";
        const char* const CPU_INFO = "/proc/cpuinfo";
        const char* const MEM_INFO = "/proc/meminfo";
        // ... 其他路径
    }

    // 命令常量
    namespace cmd {
        const char* const GET_PROP = "getprop";
        // ... 其他命令
    }
}

#endif