
#ifndef WARLOCK_CONSTANTS_H
#define WARLOCK_CONSTANTS_H

#include <cstdint>
#include <sys/system_properties.h>
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
        const char* const SYSTEM_PROP_ID = "n2";
        const char* const NETWORK_INFO_ID = "n3";
        const char* const SYSTEM_BUILD_ID = "n4";
        const char* const ODM_ETC_BUILD_ID = "n5";
        const char* const PRODUCT_BUILD_ID = "n6";
        const char* const VENDOR_BUILD_ID = "n7";
        const char* const UUID_ID = "n8";
        const char* const CID_ID = "n9";
        const char* const SERIAL_NUMBER_ID = "n10";
        const char* const MISC_ID = "n11";
        const char* const BOOT_ID = "n12";
        const char* const SYSTEM_VERSION_ID = "n13";
        const char* const GET_PROPS_ID = "n14";
        const char* const NETWORK_INTERFACES_ID = "n15";
        const char* const SERVICE_LIST_ID = "n16";
        const char* const STORAGE_STATS_ID = "n17";
        const char* const DRM_ID_ID = "n18";

    }

    // 文件路径常量
    namespace path {
        const char* const BUILD_PROP = "/system/build.prop";
        const char* const CPU_INFO = "/proc/cpuinfo";
        const char* const MEM_INFO = "/proc/meminfo";
        const char* const SYSTEM_BUILD_FILE = "/system/build.prop";
        const char* const ODM_ETC_BUILD_FILE = "/odm/etc/build.prop";
        const char* const PRODUCT_BUILD_FILE = "/product/build.prop";
        const char* const VENDOR_BUILD_FILE = "/vendor/build.prop";
        const char* const UUID_PATH = "/proc/sys/kernel/random/uuid";
        const char* const CID_PATH = "/sys/block/mmcblk0/device/cid";
        const char* const SERIAL_NUMBER_PATH = "/sys/devices/soc0/serial_number";
        const char* const MISC_PATH = "/proc/misc";
        const char* const BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id";

    }

    // 命令常量
    namespace cmd {
        const char* const GET_PROP = "getprop";
        // ... 其他命令
    }
    namespace system_props {
        // 常见系统属性数组
        static const char* const COMMON_PROPS[] = {
                "ro.build.version.sdk",
                "ro.build.version.release",
                "ro.product.model",
                "ro.product.brand",
                "ro.boot.bootloader",
                "ro.build.version.securitypatch",
                "ro.build.version.incremental",
                "gsm.version.baseband",
                "gsm.version.ril-impl",
                "ro.build.fingerprint",
                "ro.build.description",
                "ro.build.product",
                "ro.boot.vbmeta.digest",
                "ro.hardware",
                "ro.product.name",
                "ro.product.board",
                "ro.recovery_id",
                "ro.expect.recovery_id",
                "ro.board.platform",
                "ro.product.manufacturer",
                "ro.product.device",
                "ro.odm.build.id",
                "sys.usb.state",
                "ro.setupwizard.mode",
                "ro.build.id",
                "ro.build.tags",
                "ro.build.type",
                "ro.debuggable",
                "persist.sys.meid"
        };

        // 不常见系统属性数组
        static const char* const RARE_PROPS[] = {
                "vendor.serialno",
                "sys.serialno",
                "persist.sys.wififactorymac",
                "ro.boot.deviceid",
                "ro.rpmb.board",
                "ro.vold.serialno",
                "persist.oppo.wlan.macaddress",
                "persist.sys.oppo.serialno",
                "ril.serialnumber",
                "ro.boot.ap_serial",
                "ro.boot.uniqueno",
                "persist.sys.oppo.opmuuid",
                "persist.sys.oppo.nlp.id",
                "persist.sys.oplus.nlp.id",
                "persist.sys.dcs.hash",
                "ro.ril.oem.sno",
                "ro.ril.oem.psno",
                "persist.vendor.sys.fp.uid",
                "ro.ril.miui.imei0",
                "ro.ril.miui.imei1",
                "ro.ril.oem.imei",
                "ro.ril.oem.meid",
                "persist.radio.imei",
                "persist.radio.imei1",
                "persist.radio.imei2",
                "persist.sys.lite.uid",
                "persist.radio.serialno",
                "vendor.boot.serialno",
                "persist.sys.oneplus.serialno",
                "ro.meizu.hardware.imei1",
                "ro.meizu.hardware.imei2",
                "ro.meizu.hardware.meid",
                "ro.meizu.hardware.psn",
                "ro.meizu.hardware.sn",
                "persist.radio.factory_phone_sn",
                "persist.radio.factory_sn",
                "ro.meizu.serialno",
                "ro.boot.psn",
                "ro.boot.meid",
                "ro.boot.imei1",
                "ro.boot.imei2",
                "ro.wifimac",
                "ro.wifimac_2",
                "ro.vendor.deviceid",
                "ro.isn",
                "ro.vendor.isn",
                "persist.radio.device.imei",
                "persist.radio.device.imei2",
                "persist.radio.device.meid",
                "persist.radio.device.meid2",
                "persist.asus.serialno",
                "sys.wifimac",
                "sys.bt.address",
                "persist.btpw.bredr",
                "persist.radio.imei",
                "persist.radio.imei2",
                "persist.radio.meid",
                "persist.radio.meid2",
                "ro.boot.fpd.uid",
                "ro.vendor.boot.serialno",
                "ro.boot.wifimacaddr",
                "persist.sys.wifi.mac",
                "persist.sys.wifi_mac",
                "sys.prop.writeimei",
                "ril.gm.imei",
                "ril.cdma.meid",
                "ro.boot.em.did",
                "ro.qchip.serialno",
                "ro.ril.oem.btmac",
                "ro.ril.oem.ifimac"
        };

        // 获取数组大小的辅助函数
        constexpr size_t COMMON_PROPS_SIZE = sizeof(COMMON_PROPS) / sizeof(COMMON_PROPS[0]);
        constexpr size_t RARE_PROPS_SIZE = sizeof(RARE_PROPS) / sizeof(RARE_PROPS[0]);
    }
}

#endif