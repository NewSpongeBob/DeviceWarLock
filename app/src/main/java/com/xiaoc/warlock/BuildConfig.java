package com.xiaoc.warlock;

import java.util.HashMap;
import java.util.Map;

public class BuildConfig {
 public static String APP_PACKAGE = "com.xiaoc.warlock";
 public static final String ZYGISK_PATH = "/data/adb/zy";
 public static String APK_PATH = "/data/data/com.xiaoc.warlock";
 public static String AP_PACKAGE_PATH = "/data/data/me.bmax.apatch";
 public static String SDCARD_DOWNLOAD_PATH = "/sdcard/Download";
 public static String SDCARD_ANDROID_PATH = "/sdcard/Android/";
 public static String DATA_LOCAL_TMP_PATH = "/data/local/tmp";
 public static final String[] FINGERPRINT_REGIONS = {
         "build", "bootimage", "odm", "product", "system_ext", "system", "vendor"
 };
 public static final String KEYCHAIN_DIR = "/data/misc/keychain";
 public static final String PUBKEY_BLACKLIST_FILE = "/data/misc/keychain/pubkey_blacklist.txt";
 public static final String SERIAL_BLACKLIST_FILE = "/data/misc/keychain/serial_blacklist.txt";
 public static final Map<String, String> PATH_MAPPINGS = new HashMap<String, String>() {{
  put("/sdcard/Android/data/.nomedia", "SDADN");
  put("/sdcard/Android/data/com.google.android.gms", "SDADC");
  put("/sdcard/", "SD");
  put("/storage/emulated/0", "SD0");
 }};
 public static final String STORAGE_SERIAL_PATH = "/sys/block/mmcblk0/device/serial";
 public static final String SERIAL_PATH = "/sys/devices/soc0/serial_number";
 public static final String DEVICES_PATH = "/proc/bus/input/devices";
 public static final String CID_PATH = "/sys/block/mmcblk0/device/cid";
 public static final int SENSOR_MINIMUM_QUANTITY_LIMIT = 15;
 public static  String[] targetPackages = {
         "com.sankuai.meituan",         // 美团
         "com.tencent.mm",              // 微信
         "bin.mt.plus",                 // MT管理器
         "com.smile.gifmaker",          // 快手
         "com.ss.android.ugc.aweme",    // 抖音
         "com.eg.android.AlipayGphone"  // 支付宝
 };
 public static final String APATCH_STRING = "Apatch";
 public static final String KSU_STRING = "KSU";

 /**
  * 模拟器共享文件夹路径列表
  */
 public static final String[] EMULATOR_MOUNT_PATHS = {
         "/mnt/shared/Sharefolder",    // 通用共享文件夹
         "/tiantian.conf",             // 天天模拟器
         "/data/share1",               // 通用共享目录
         "/hardware_device.conf",      // 硬件配置文件
         "/mnt/shared/products",       // 共享产品目录
         "/mumu_hardware.conf",        // MUMU模拟器
         "/Andy.conf",                 // Andy模拟器
         "/mnt/windows/BstSharedFolder", // BlueStacks
         "/bst.conf",                  // BlueStacks配置
         "/mnt/shared/Applications",   // 共享应用目录
         "/ld.conf",                    // LD模拟器
         "vboxsf"                      //virtualbox
 };
 public static final String[] ROOT_PACKAGES = {
         "com.topjohnwu.magisk",
         "eu.chainfire.supersu",
         "com.noshufou.android.su",
         "com.noshufou.android.su.elite",
         "com.koushikdutta.superuser",
         "com.thirdparty.superuser",
         "com.yellowes.su",
         "com.fox2code.mmm",
         "io.github.vvb2060.magisk",
         "com.kingroot.kinguser",
         "com.kingo.root",
         "com.smedialink.oneclickroot",
         "com.zhiqupk.root.global",
         "com.alephzain.framaroot",
         "io.github.huskydg.magisk",
         "me.weishu.kernelsu",
         "me.bmax.apatch"
 };
 /**
  * Root相关文件路径列表
  */
 public static final String[] ROOT_FILES = {
         "/su/bin/su",
         "/sbin/su",
         "/data/local/xbin/su",
         "/data/local/bin/su",
         "/data/local/su",
         "/system/xbin/su",
         "/system/bin/su",
         "/system/sd/xbin/su",
         "/system/bin/failsafe/su",
         "/system/bin/.ext/.su",
         "/system/etc/.installed_su_daemon",
         "/system/etc/.has_su_daemon",
         "/system/xbin/sugote",
         "/system/xbin/sugote-mksh",
         "/system/xbin/supolicy",
         "/system/etc/init.d/99SuperSUDaemon",
         "/system/.supersu",
         "/product/bin/su",
         "/apex/com.android.runtime/bin/su",
         "/apex/com.android.art/bin/su",
         "/system_ext/bin/su",
         "/system/xbin/bstk/su",
         "/system/app/SuperUser/SuperUser.apk",
         "/system/app/Superuser.apk",
         "/system/xbin/mu_bak",
         "/odm/bin/su",
         "/vendor/bin/su",
         "/vendor/xbin/su",
         "/system/bin/.ext/su",
         "/system/usr/we-need-root/su",
         "/cache/su",
         "/data/su",
         "/dev/su",
         "/system/bin/cph_su",
         "/dev/com.koushikdutta.superuser.daemon",
         "/system/xbin/daemonsu",
         "/sbin/.mianju",
         "/sbin/nvsu",
         "/system/bin/.hid/su",
         "/system/addon.d/99-magisk.sh",
         "/cache/.disable_magisk",
         "/dev/magisk/img",
         "/sbin/.magisk",
         "/cache/magisk.log",
         "/data/adb/magisk",
         "/system/etc/init/magisk",
         "/system/etc/init/magisk.rc",
         "/data/magisk.apk"
 };
 public static final String[] EMULATOR_FILES = {
         // LD模拟器
         "/system/bin/ldinit",
         "/system/bin/ldmountsf",
         "/system/lib/libldutils.so",

         // MicroVirt (逍遥模拟器)
         "/system/bin/microvirt-prop",
         "/system/lib/libdroid4x.so",
         "/system/bin/windroyed",
         "/system/lib/libnemuVMprop.so",
         "/system/bin/microvirtd",

         // NOX (夜神模拟器)
         "/system/bin/nox-prop",
         "/system/lib/libnoxspeedup.so",
         "/data/property/persist.nox.simulator_version",
         "/data/misc/profiles/ref/com.bignox.google.installer",
         "/data/misc/profiles/ref/com.bignox.app.store.hd",

         // 其他模拟器特征
         "/system/bin/ttVM-prop",
         "/system/bin/droid4x-prop",
         "/system/bin/duosconfig",
         "/system/etc/xxzs_prop.sh",

         // MUMU模拟器
         "/system/etc/mumu-configs/device-prop-configs/mumu.config",

         // BlueStacks
         "/boot/bstsetup.env",
         "/boot/bstmods",
         "/system/xbin/bstk",
         "/data/bluestacks.prop",
         "/data/data/com.anrovmconfig",
         "/data/data/com.bluestacks.appmart",
         "/data/data/com.bluestacks.home",

         // MicroVirt相关
         "/data/data/com.microvirt.market",
         "/dev/nemuguest",
         "/data/data/com.microvirt.toolst",

         // MUMU相关
         "/data/data/com.mumu.launcher",
         "/data/data/com.mumu.store",
         "/data/data/com.netease.mumu.cloner",

         // BlueStacks其他特征
         "/system/bin/bstshutdown",
         "/sys/module/bstinput",
         "/sys/class/misc/bstXqpb",

         // PhoenixOS
         "/system/phoenixos",
         "/xbin/phoenix_compat",

         // 遁地模拟器
         "/init.dundi.rc",
         "/system/etc/init.dundi.sh",
         "/data/data/com.ddmnq.dundidevhelper",

         // Andy Cloud
         "/init.andy.cloud.rc",

         // 其他模拟器
         "/system/bin/xiaopiVM-prop",
         "/system/bin/XCPlayer-prop",
         "/system/lib/liblybox_prop.so",

         // 腾讯模拟器
         "/system/bin/tencent_virtual_input",
         "/vendor/bin/init.tencent.sh",

         // YouWave
         "/data/youwave_id",

         // VirtualBox相关
         "/dev/vboxguest",
         "/dev/vboxuser",
         "/sys/bus/pci/drivers/vboxguest",
         "/sys/class/bdi/vboxsf-c",
         "/sys/class/misc/vboxguest",
         "/sys/class/misc/vboxuser",
         "/sys/devices/virtual/bdi/vboxsf-c",
         "/sys/devices/virtual/misc/vboxguest",
         "/sys/devices/virtual/misc/vboxuser",
         "/sys/module/vboxguest",
         "/sys/module/vboxsf",
         "/sys/module/vboxvideo",
         "/system/bin/androVM-vbox-sf",
         "/system/bin/androVM_setprop",
         "/system/bin/get_androVM_host",
         "/system/bin/mount.vboxsf",
         "/system/etc/init.androVM.sh",
         "/system/etc/init.buildroid.sh",
         "/system/lib/vboxguest.ko",
         "/system/lib/vboxsf.ko",
         "/system/lib/vboxvideo.ko",
         "/system/xbin/mount.vboxsf",

         // Goldfish (Android模拟器)
         "/dev/goldfish_pipe",
         "/sys/devices/virtual/misc/goldfish_pipe",
         "/sys/module/goldfish_audio",
         "/sys/module/goldfish_battery",

         // KVM相关
         "/sys/module/kvm_intel/",
         "/sys/module/kvm_amd/",

         // x86相关配置文件
         "/init.android_x86_64.rc",
         "/init.android_x86.rc",
         "/init.androidVM_x86.rc",
         "/init.intel.rc",
         "/init.vbox2345_x86.rc",
         // 新增的模拟器相关路径
         "/system/bin/androVM-prop",
         "/system/bin/microvirt-prop",
         "/system/lib/libdroid4x.so",
         "/system/bin/windroyed",
         "/system/bin/nox-prop",
         "/system/lib/libnoxspeedup.so",
         "/system/bin/ttVM-prop",
         "/data/.bluestacks.prop",
         "/system/bin/duosconfig",
         "/system/etc/xxzs_prop.sh",
         "/system/etc/mumu-configs/device-prop-configs/mumu.config",
         "/system/priv-app/ldAppStore",
         "/system/bin/ldinit",
         "/system/bin/ldmountsf",
         "/system/app/AntStore",
         "/system/app/AntLauncher",
         "/vmos.prop",
         "/fstab.titan",
         "/init.titan.rc",
         "/x8.prop",
         "/system/lib/libc_malloc_debug_qemu.so",

         "/boot/bstmods/vboxsf.ko",
         "/dev/mtp_usb",
         "/dev/qemu_pipe",
         "/dev/socket/baseband_genyd",
         "/dev/socket/genyd",
         "/dev/socket/qemud",
         "/dev/socket/windroyed-audio",
         "/dev/socket/windroyed-camera",
         "/dev/socket/windroyed-gps",
         "/dev/socket/windroyed-sensors",
         "/dev/vboxguest",
         "/dev/vboxpci",
         "/dev/vboxuser",
         "/fstab.goldfish",
         "/fstab.nox",
         "/fstab.ranchu-encrypt",
         "/fstab.ranchu-noencrypt",
         "/fstab.ttVM_x86",
         "/fstab.vbox86",
         "/init.goldfish.rc",
         "/init.magisk.rc",
         "/init.nox.rc",
         "/init.ranchu-encrypt.rc",
         "/init.ranchu-noencrypt.rc",
         "/init.ranchu.rc",
         "/init.ttVM_x86.rc",
         "/init.vbox86.rc",
         "/init.vbox86p.rc",
         "/init.windroye.rc",
         "/init.windroye.sh",
         "/init.x86.rc",
         "/proc/irq/20/vboxguest",
         "/sdcard/Android/data/com.redfinger.gamemanage",
         "/stab.andy",
         "/sys/bus/pci/drivers/vboxguest",
         "/sys/bus/pci/drivers/vboxpci",
         "/sys/bus/platform/drivers/qemu_pipe",
         "/sys/bus/platform/drivers/qemu_pipe/qemu_pipe",
         "/sys/bus/platform/drivers/qemu_trace",
         "/sys/bus/virtio/drivers/itolsvmlgtp",
         "/sys/bus/virtio/drivers/itoolsvmhft",
         "/sys/class/bdi/vboxsf-1",
         "/sys/class/bdi/vboxsf-2",
         "/sys/class/bdi/vboxsf-3",
         "/sys/class/misc/qemu_pipe",
         "/sys/class/misc/vboxguest",
         "/sys/class/misc/vboxuser",
         "/sys/devices/platform/qemu_pipe",
         "/sys/devices/virtual/bdi/vboxsf-1",
         "/sys/devices/virtual/bdi/vboxsf-2",
         "/sys/devices/virtual/bdi/vboxsf-3",
         "/sys/devices/virtual/misc/qemu_pipe",
         "/sys/devices/virtual/misc/vboxguest",
         "/sys/devices/virtual/misc/vboxpci",
         "/sys/devices/virtual/misc/vboxuser",
         "/sys/fs/selinux/booleans/in_qemu",
         "/sys/kernel/debug/bdi/vboxsf-1",
         "/sys/kernel/debug/bdi/vboxsf-2",
         "/sys/kernel/debug/x86",
         "/sys/module/qemu_trace_sysfs",
         "/sys/module/vboxguest",
         "/sys/module/vboxguest/drivers/pci:vboxguest",
         "/sys/module/vboxpcism",
         "/sys/module/vboxsf",
         "/sys/module/vboxvideo",
         "/sys/module/virtio_pt/drivers/virtio:itoolsvmhft",
         "/sys/module/virtio_pt_ie/drivers/virtio:itoolsvmlgtp",
         "/sys/qemu_trace",
         "/system/app/GenymotionLayout",
         "/system/bin/OpenglService",
         "/system/bin/androVM-vbox-sf",
         "/system/bin/droid4x",
         "/system/bin/droid4x-prop",
         "/system/bin/droid4x-vbox-sf",
         "/system/bin/droid4x_setprop",
         "/system/bin/enable_nox",
         "/system/bin/genymotion-vbox-sf",
         "/system/bin/microvirt-prop",
         "/system/bin/microvirt-vbox-sf",
         "/system/bin/microvirt_setprop",
         "/system/bin/microvirtd",
         "/system/bin/mount.vboxsf",
         "/system/bin/nox",
         "/system/bin/nox-prop",
         "/system/bin/nox-vbox-sf",
         "/system/bin/nox_setprop",
         "/system/bin/noxd",
         "/system/bin/noxscreen"

 };

}
