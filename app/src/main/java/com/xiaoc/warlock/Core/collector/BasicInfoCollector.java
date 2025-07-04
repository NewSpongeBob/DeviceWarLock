package com.xiaoc.warlock.Core.collector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaDrm;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.MiscUtil;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XString;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

public class BasicInfoCollector extends BaseCollector {
    private static final String TAG = "BasicInfoCollector";
    private static final Pattern PROP_PATTERN = Pattern.compile("\\[(.*?)\\]:\\s*\\[(.*?)\\]");
    private static final String WIDEVINE_UUID = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed";
    private static final Uri GSF_URI = Uri.parse("content://com.google.android.gsf.gservices");
    private static final String GSF_PACKAGE = "com.google.android.gsf";

    public BasicInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        // 设备标识信息
        getMacAddress();            // MAC地址
        getAndroidID();            // a5: Android ID
        getAaid();                 // a3: Google Advertising ID
        getDrmIdSha256();          // a6: DRM ID
        getBootID();               // a4: Boot ID
        
        // 系统属性信息
        getProp();                 // a1: System Properties
        archInfo();                // a2: CPU架构信息
        
        // 设备基本信息
        collectDeviceBrands();     // a8, a9: 品牌和型号
        collectDeviceSerial();     // a20: 序列号
        collectFingerprint();      // a11: 设备指纹
        collectDataDir();          // a10: 数据目录
        collectTimeZone();         // a12: 时区信息
        
        // 应用相关信息
        getAppPackage();           // a16: 包名信息
        getAppPath();              // a14: 应用路径
        
        // 其他信息
        collectAccounts();         // a60: 账户信息
        getBluetoothAddress();     // a15: 蓝牙地址
    }

    /**
     * 收集设备品牌和型号信息
     */
    private void collectDeviceBrands() {
        try {
            putInfo("a8", Build.BRAND);
        } catch (Exception e) {
            putFailedInfo("a8");
        }

        try {
            putInfo("a9", Build.MODEL);
        } catch (Exception e) {
            putFailedInfo("a9");
        }
    }

    /**
     * 收集设备序列号
     */
    private void collectDeviceSerial() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putInfo("a20", Build.getSerial());
            } else {
                putFailedInfo("a20");
            }
        } catch (Exception e) {
            putFailedInfo("a20");
        }
    }

    /**
     * 收集设备指纹
     */
    private void collectFingerprint() {
        try {
            putInfo("a11", Build.FINGERPRINT);
        } catch (Exception e) {
            putFailedInfo("a11");
        }
    }

    /**
     * 收集数据目录
     */
    private void collectDataDir() {
        try {
            putInfo("a10", context.getApplicationInfo().dataDir);
        } catch (Exception e) {
            putFailedInfo("a10");
        }
    }

    /**
     * 收集时区信息
     */
    private void collectTimeZone() {
        try {
            TimeZone timeZone = TimeZone.getDefault();
            putInfo("a12", timeZone.getID());
        } catch (Exception e) {
            putFailedInfo("a12");
        }
    }

    public void getAppPath() {
        Map<String, String> pathInfo = new LinkedHashMap<>();

        String pm_Path = "";
        String src_Path= "";
        String shell_Path= "";
        try {
            // 方法1：通过ApplicationInfo获取
             src_Path = context.getApplicationInfo().sourceDir;

            // 方法2：通过PackageManager获取
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 0);
            pm_Path = ai.sourceDir;

            // 方法3：通过命令行获取
            String packageName = context.getPackageName();
            String command = "pm path " + packageName;
            XCommandUtil.CommandResult result = XCommandUtil.execute(command);
            if (result.isSuccess()) {
                shell_Path = result.getSuccessMsg().replace("package:", "");
            }
            if (XString.compareIgnoreSpaces(src_Path,pm_Path,shell_Path)){
                putInfo("a14",src_Path);
            }else {
                pathInfo.put("src_Path",pm_Path);
                pathInfo.put("pm_Path",pm_Path);
                pathInfo.put("shell_Path",shell_Path);
                putInfo("a14",pathInfo);
            }

        } catch (Exception e) {
            putFailedInfo("a14");
        }
    }
    //通过两种方式获取并判断是否一致，如果获取出来的结果一致就写入bootFile获取的结果，如果不一致则写入两种不同的结果
    private  void getBootID(){
        try {
            Map<String, String> bootInfo = new LinkedHashMap<>();
            String bootFile = XFile.readFile("/proc/sys/kernel/random/boot_id");
            String bootShell;
            XCommandUtil.CommandResult result = XCommandUtil.execute("cat /proc/sys/kernel/random/boot_id");
            if (result.isSuccess()) {
                bootShell = result.getSuccessMsg();
            }else {
                bootShell = "null";
            }
            boolean bootB = XString.compareIgnoreSpaces(bootFile,bootShell);
            if (bootB){
                putInfo("a4",bootShell);
            }else {
                bootInfo.put("bootFile" ,bootFile);
                bootInfo.put("bootShell" ,bootShell);
                putInfo("a4",bootInfo);
            }
        }catch (Exception e){
            putFailedInfo("a4");

        }

    }
    private void archInfo (){
        try {
            Map<String, String> archInfo = new LinkedHashMap<>();

            // 方法1：通过Build类获取
            String cpuAbi = Build.CPU_ABI;
            if (!XString.isEmpty(cpuAbi)) {
                archInfo.put("ARCH_BUILD" , cpuAbi);
            }

            String arch;
            // 方法2：通过系统属性获取
            arch = System.getProperty("os.arch");
            if (!XString.isEmpty(arch)) {
                archInfo.put("ARCH_SYSTEM" , cpuAbi);
            }

            // 方法3：通过执行命令获取
            try {
                Process process = Runtime.getRuntime().exec("getprop ro.product.cpu.abi");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                arch = reader.readLine();
                reader.close();
                if (!XString.isEmpty(arch)) {
                    archInfo.put("ARCH_PROP" , arch);
                }
            } catch (Exception ignored) {
            }
            XLog.d(archInfo.toString());
            // 如果收集到信息，则存储
            if (!archInfo.isEmpty()) {
                // 直接存储List，Gson会自动转换为数组
                putInfo("a2", archInfo);
            } else {
                putFailedInfo("a2");
            }
        }catch (Exception e){
            putFailedInfo("a2");

        }

    }
    private void getProp() {
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("getprop");
            if (result.isSuccess()) {
                Map<String, String> propMap = new LinkedHashMap<>();
                String content = result.getSuccessMsg();
                // XLog.d(content);

                // 按行分割输出
                String[] lines = content.split("\n");
                for (String line : lines) {
                    // 去掉首尾空白字符
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // 查找键和值
                    int keyStart = line.indexOf('[');
                    int keyEnd = line.indexOf(']', keyStart);
                    int valueStart = line.indexOf('[', keyEnd);
                    int valueEnd = line.indexOf(']', valueStart);

                    if (keyStart != -1 && keyEnd != -1 && valueStart != -1 && valueEnd != -1) {
                        String key = line.substring(keyStart + 1, keyEnd).trim();
                        String value = line.substring(valueStart + 1, valueEnd).trim();
                        if (!key.isEmpty()) {
                            propMap.put(key, value);
                        }
                    }
                }

                // 输出剩余的属性
                if (!propMap.isEmpty()) {
                    putInfo("a1", propMap);
                }
            } else {
                putFailedInfo("a1");
            }
        }catch (Exception e) {
            putFailedInfo("a1");
            XLog.e(TAG, "Failed to collect props: " + e.getMessage());
        }

    }
    /**
     * 通过Binder方式获取Android ID
     */
    private String getAndroidIdViaBinder() {
        Parcel data = null;
        Parcel reply = null;

        try {
            // 获取ActivityThread实例
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = activityThreadClass
                    .getMethod("currentActivityThread")
                    .invoke(null);

            // 获取ContentProvider
            Object provider = activityThreadClass
                    .getMethod("acquireProvider", Context.class, String.class, int.class, boolean.class)
                    .invoke(currentActivityThread, context, "settings", 0, true);

            if (provider == null) return "";

            // 获取IContentProvider的Binder对象
            Field mRemoteField = provider.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            IBinder binder = (IBinder) mRemoteField.get(provider);

            if (binder == null) return "";

            // 准备Parcel数据
            data = Parcel.obtain();
            reply = Parcel.obtain();

            // 写入接口标识
            data.writeInterfaceToken("android.content.IContentProvider");

            // 根据Android版本写入不同参数
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12及以上
                context.getAttributionSource().writeToParcel(data, 0);
                writeCommonParcelData(data);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                // Android 11
                data.writeString(context.getPackageName());
                data.writeString(null); // featureId
                writeCommonParcelData(data);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10
                data.writeString(context.getPackageName());
                writeCommonParcelData(data);
            } else {
                // Android 9及以下
                data.writeString(context.getPackageName());
                data.writeString("GET_secure");
                data.writeString("android_id");
                data.writeBundle(Bundle.EMPTY);
            }

            // 执行Binder调用
            int callTransaction = Class.forName("android.content.IContentProvider")
                    .getDeclaredField("CALL_TRANSACTION")
                    .getInt(null);

            if (binder.transact(callTransaction, data, reply, 0)) {
                reply.readException();
                Bundle resultBundle = reply.readBundle();
                return resultBundle != null ? resultBundle.getString("value") : "";
            }

            return "";
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get Android ID via binder", e);
            return "";
        } finally {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        }
    }

    /**
     * 写入公共Parcel数据
     */
    private void writeCommonParcelData(Parcel data) {
        data.writeString("settings");     // authority
        data.writeString("GET_secure");   // method
        data.writeString("android_id");   // stringArg
        data.writeBundle(Bundle.EMPTY);
    }

    private void getAndroidID() {
        String android_id_1 = "";  // Settings.Secure方式
        String android_id_2 = "";  // ContentResolver方式
        String android_id_3 = "";  // NameValueCache方式
        String android_id_4 = "";  // Binder方式
        Map<String, String> androidIdInfo = new LinkedHashMap<>();
        try {
            try {
                // 方法1: Settings.Secure方式
                android_id_1 = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                // 方法2: ContentResolver方式
                Bundle bundle = context.getContentResolver().call(
                        Uri.parse("content://settings/secure"),
                        "GET_secure",
                        "android_id",
                        new Bundle()
                );
                android_id_2 = bundle != null ? bundle.getString("value") : "";

                // 方法3: NameValueCache方式
                try {
                    Field sNameValueCache = Settings.Secure.class.getDeclaredField("sNameValueCache");
                    sNameValueCache.setAccessible(true);
                    Object sLockSettings = sNameValueCache.get(null);
                    Field fieldmValues = sLockSettings.getClass().getDeclaredField("mValues");
                    fieldmValues.setAccessible(true);
                    ArrayMap<String, String> mValues = (ArrayMap<String, String>) fieldmValues.get(sLockSettings);
                    android_id_3 = mValues != null ? mValues.get("android_id") : "";
                } catch (Throwable ignored) {
                }

                // 方法4: Binder方式
                android_id_4 = getAndroidIdViaBinder();

            } catch (Exception e) {

                XLog.e(TAG, "Failed to get Android ID", e);
            }

            // 检查所有获取到的ID是否一致
            boolean android_id_match = XString.compareIgnoreSpaces(
                    android_id_1,
                    android_id_2,
                    android_id_3,
                    android_id_4
            );

            if (android_id_match) {
                putInfo("a5", android_id_1);
            } else {
                androidIdInfo.put("androidSettings", android_id_1);
                androidIdInfo.put("androidBundle", android_id_2);
                androidIdInfo.put("androidCache", android_id_3);
                androidIdInfo.put("androidBinder", android_id_4);
                putInfo("a5", androidIdInfo);
            }
        }catch (Exception e){
            putFailedInfo("a5");

        }

    }

    @SuppressLint("DefaultLocale")
    private void getAppPackage(){
        try {
            List<PackageInfo> packageInfoList = context.getPackageManager().getInstalledPackages(128);
            int x = 0, y = 0, z = 0;
            for (PackageInfo packageinfo: packageInfoList) {
                if ((packageinfo.applicationInfo.flags & 1) == 1){
                    x += 1;
                }else if (Long.toString(packageinfo.firstInstallTime).endsWith("000")){
                    y += 1;
                }else {
                    z += 1;
                    Log.d(TAG, "getAppPackage: " + packageinfo.packageName);
                }
            }
            putInfo("a90", String.format("%1$d_%2$d_%3$d", z, y, x));
            // 收集所有获取包名的方法结果
            String contextPkg = context.getPackageName();
            String appInfoPkg = context.getApplicationInfo().packageName;
            String pmPkg = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).packageName;
            String buildConfigPkg = BuildConfig.APP_PACKAGE;

            // 检查所有包名是否一致
            boolean allMatch = XString.compareIgnoreSpaces(contextPkg, appInfoPkg) &&
                    XString.compareIgnoreSpaces(contextPkg, pmPkg) &&
                    XString.compareIgnoreSpaces(contextPkg, buildConfigPkg);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("package_name", contextPkg);
            if (!allMatch) {
                Map<String, String> allMethods = new LinkedHashMap<>();
                allMethods.put("context", contextPkg);
                allMethods.put("application_info", appInfoPkg);
                allMethods.put("package_manager", pmPkg);
                allMethods.put("build_config", buildConfigPkg);
                result.put("all_methods", allMethods);
                putInfo("a16", result);

            }else {
                putInfo("a16", contextPkg);
            }
        } catch (Exception e) {
            putFailedInfo("a16");
            XLog.e(TAG, "Failed to collect package info: " + e.getMessage());
        }
        try {
            Map<String, String> gsfInfo = new LinkedHashMap<>();

            // 首先检查 Google 服务是否可用
            boolean isGoogleServicesAvailable = isGoogleServicesAvailable();
            boolean isGSFAvailable = isGSFAvailable();

            gsfInfo.put("google_services_available", String.valueOf(isGoogleServicesAvailable));
            gsfInfo.put("gsf_available", String.valueOf(isGSFAvailable));

            // 只有当两个服务都可用时才尝试获取 GSF ID
            if (isGoogleServicesAvailable && isGSFAvailable) {
                String gsfId = getGSFIdMultiMethod();
                gsfInfo.put("gsf_id", gsfId != null ? gsfId : "Not available");
            } else {
                gsfInfo.put("gsf_id", "Services not available");
                XLog.i(TAG, "Google Services not available on this device");
            }

            putInfo("a7", gsfInfo);

        } catch (Exception e) {
            putFailedInfo("a7");
            XLog.e(TAG, "Failed to collect GSF info: " + e.getMessage());
        }
    }
    private void getSettingVaule (){
        try {
            Map<String, String> settingValueInfo = new LinkedHashMap<>();
            String miHealthId = Settings.Global.getString(context.getContentResolver(), "mi_health_id");
            String gcboosterUuid = Settings.Global.getString(context.getContentResolver(), "gcbooster_uuid");
            String keyMqsUuid = Settings.Global.getString(context.getContentResolver(), "key_mqs_uuid");
            String adAaid = Settings.Global.getString(context.getContentResolver(), "ad_aaid");


            if (miHealthId != null) {
                settingValueInfo.put("mi_health_id", miHealthId);
            } else {
                settingValueInfo.put("mi_health_id", "Not available");
            }

            if (gcboosterUuid != null) {
                settingValueInfo.put("gcbooster_uuid", gcboosterUuid);
            } else {
                settingValueInfo.put("gcbooster_uuid", "Not available");
            }

            if (keyMqsUuid != null) {
                settingValueInfo.put("key_mqs_uuid", keyMqsUuid);
            } else {
                settingValueInfo.put("key_mqs_uuid", "Not available");
            }

            if (adAaid != null) {
                settingValueInfo.put("ad_aaid", adAaid);
            } else {
                settingValueInfo.put("ad_aaid", "Not available");
            }
            putInfo("a13", settingValueInfo);
        }catch (Exception e){
            putFailedInfo("a13");
        }

    }
    private void getAaid(){
        try {
            Map<String, String> aaidMap = new LinkedHashMap<>();

            // 首先尝试通过 Settings.Secure 获取
            String secureAaid = Settings.Secure.getString(
                    context.getContentResolver(),
                    "advertising_id"
            );

            if (secureAaid != null && !secureAaid.isEmpty()) {
                aaidMap.put("secure_aaid", secureAaid);
            }

            // 然后尝试通过 Google API 获取
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                if (adInfo != null) {
                    String googleAaid = adInfo.getId();
                    if (googleAaid != null && !googleAaid.isEmpty()) {
                        aaidMap.put("google_aaid", googleAaid);
                    }
                }
            } catch (Exception e) {
                XLog.e(TAG, "Failed to get Google AAID: " + e.getMessage());
            }

            // 检查是否至少有一个值
            if (!aaidMap.isEmpty()) {
                putInfo("a3", aaidMap);
            } else {
                putFailedInfo("a3");
            }
        }catch (Exception e){
            putFailedInfo("a3");

        }

    }
    @SuppressLint("HardwareIds")
    private void getBluetoothAddress() {
        try {
            String address = "";
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                address = bluetoothAdapter.getAddress();
            } catch (Exception e) {
                XLog.e(e.getMessage());
            }
            if (XString.isEmpty(address)){
                putFailedInfo("a15");
            }else {
                putInfo("a15",address);
            }
        }catch (Exception e){
            putFailedInfo("a15");
        }

    }
    public void getMacAddress() {
        try {
            String macAddress = null;

            WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = (null == wifiManager ? null : wifiManager.getConnectionInfo());

            macAddress = info.getMacAddress();
            putInfo("a46",macAddress);
        }catch (Exception e) {
            putFailedInfo("a46");
                XLog.e(e.getMessage());
        }
    }
    private void getDrmIdSha256() {
        MediaDrm mediaDrm = null;
        try {
            UUID uuid = UUID.fromString(WIDEVINE_UUID);
            mediaDrm = new MediaDrm(uuid);
            byte[] widewineId = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);
            String drmId = Base64.encodeToString(widewineId, Base64.NO_WRAP);

            // SHA-256 加密
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] sha256Bytes = md.digest(drmId.getBytes());

            // 转换为十六进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : sha256Bytes) {
                hexString.append(String.format("%02x", b));
            }

            putInfo("a6", drmId);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to get SHA256 DRM ID: " + e.getMessage());
            putFailedInfo("a6");
        } finally {
            if (mediaDrm != null) {
                try {
                    mediaDrm.close();
                } catch (Exception e) {
                    XLog.e(TAG, "Failed to close MediaDrm: " + e.getMessage());
                }
            }
        }
    }
    /**
     * 检查 Google Play Services 是否可用
     */
    private boolean isGoogleServicesAvailable() {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.google.android.gms", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查 Google Services Framework 是否可用
     */
    private boolean isGSFAvailable() {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(GSF_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    /**
     * 收集设备上的账户信息
     * 通过 AccountManager 获取所有账户,包括:
     * - Google账户
     * - 小米账户
     * - 其他第三方账户
     * 结果格式:
     * {
     *   "t": "账户类型",  // 如 com.google, com.xiaomi
     *   "n": "账户名称"   // 如 example@gmail.com
     * }
     */
    private void collectAccounts() {
        try {
            // 获取AccountManager实例
            AccountManager accountManager = AccountManager.get(context);
            // 获取所有账户信息
            Account[] accounts = accountManager.getAccounts();

            if (accounts != null && accounts.length > 0) {
                // 创建账户信息列表
                List<Map<String, String>> accountList = new ArrayList<>();

                // 遍历所有账户
                for (Account account : accounts) {
                    Map<String, String> accountInfo = new LinkedHashMap<>();
                    // 保存账户类型(type)和名称(name)
                    accountInfo.put("t", account.type);      // t = type(类型)
                    accountInfo.put("n", account.name);      // n = name(名称)
                    accountList.add(accountInfo);
                }

                // 保存收集到的账户信息
                putInfo("a60", accountList);
            } else {
                // 没有找到任何账户,标记为失败
                putFailedInfo("a60");
            }
        } catch (SecurityException e) {
            // 处理权限不足的情况
            XLog.e(TAG, "Permission denied: " + e.getMessage());
            putFailedInfo("a60");
        } catch (Exception e) {
            // 处理其他异常情况
            XLog.e(TAG, "Failed to collect accounts: " + e.getMessage());
            putFailedInfo("a60");
        }
    }
    /**
     * 通过多种方法尝试获取 GSF ID
     */
    private String getGSFIdMultiMethod() {
        String gsfId = null;

        // 方法1：通过 ContentResolver
        try {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(GSF_URI, null, null, new String[]{"android_id"}, null);

            if (cursor != null && cursor.moveToFirst()) {
                gsfId = cursor.getString(1);
                cursor.close();
                if (gsfId != null && !gsfId.isEmpty()) {
                    return gsfId;
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get GSF ID from ContentResolver: " + e.getMessage());
        }

        // 方法2：通过命令行
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute(
                    "content query --uri content://com.google.android.gsf.gservices --where \"name=\'android_id\'\"");
            if (result.isSuccess()) {
                String output = result.getSuccessMsg();
                if (output != null && !output.isEmpty()) {
                    String[] parts = output.split("=");
                    if (parts.length > 1) {
                        gsfId = parts[1].trim();
                        if (!gsfId.isEmpty()) {
                            return gsfId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get GSF ID via command: " + e.getMessage());
        }

        // 方法3：通过 Settings.Secure（备选方法）
        try {
            gsfId = Settings.Secure.getString(context.getContentResolver(), "android_id");
            if (gsfId != null && !gsfId.isEmpty()) {
                return gsfId;
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get GSF ID from Settings.Secure: " + e.getMessage());
        }

        return gsfId;
    }
    private void getFingerPrintForGather(){
        try {
            StringBuilder concatenated = new StringBuilder();
            StringBuilder indexBuilder = new StringBuilder();
            boolean hasValidValue = false;

            for (int i = 0; i < BuildConfig.FINGERPRINT_REGIONS.length; i++) {
                String region = BuildConfig.FINGERPRINT_REGIONS[i];
                String propName;
                String fingerprint;

                // 特殊处理 build region
                if ("build".equals(region)) {
                    fingerprint = MiscUtil.getSystemProperty("ro.build.fingerprint");
                    if (fingerprint == null || fingerprint.isEmpty()) {
                        fingerprint = MiscUtil.getSystemProperty("ro.build.build.fingerprint");
                    }
                } else {
                    propName = "ro." + region + ".build.fingerprint";
                    fingerprint = MiscUtil.getSystemProperty(propName);
                }

                if (fingerprint != null && !fingerprint.isEmpty()) {
                    hasValidValue = true;
                    // 计算单个指纹的MD5
                    String md5 = calculateMD5(fingerprint);
                    // 拼接格式：region=md5
                    if (concatenated.length() > 0) {
                        concatenated.append("&");
                    }
                    concatenated.append(region).append("=").append(md5);

                    // 记录索引（从1开始）
                    if (indexBuilder.length() > 0) {
                        indexBuilder.append(",");
                    }
                    indexBuilder.append(i + 1);
                }
            }
            XLog.d(concatenated.toString());
            if (hasValidValue) {
                // 创建结果Map
                Map<String, String> resultMap = new LinkedHashMap<>();
                // 计算最终的MD5
                resultMap.put("md5", calculateMD5(concatenated.toString()));
                // 添加索引字符串
                resultMap.put("index", indexBuilder.toString());

                putInfo("a18", resultMap);
            } else {
                putFailedInfo("a18");
            }
        }catch (Exception e){
            putFailedInfo("a18");
        }

    }


    private String calculateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            XLog.e(TAG, "Failed to calculate MD5: " + e.getMessage());
            return "";
        }
    }
}
