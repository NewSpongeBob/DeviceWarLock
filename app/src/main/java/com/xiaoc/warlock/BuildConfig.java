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
 public static  String[] targetPackages = {
         "com.sankuai.meituan",         // 美团
         "com.tencent.mm",              // 微信
         "bin.mt.plus",                 // MT管理器
         "com.smile.gifmaker",          // 快手
         "com.ss.android.ugc.aweme",    // 抖音
         "com.eg.android.AlipayGphone"  // 支付宝
 };
}
