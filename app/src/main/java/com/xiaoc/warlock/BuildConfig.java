package com.xiaoc.warlock;

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
}
