package com.xiaoc.warlock.Core.collector;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Base64;
import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XString;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

public class BasicInfoCollector extends BaseCollector {
    private static final Pattern PROP_PATTERN = Pattern.compile("\\[(.*?)\\]:\\s*\\[(.*?)\\]");
    private static final String WIDEVINE_UUID = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed";
    private static final Uri GSF_URI = Uri.parse("content://com.google.android.gsf.gservices");
    private static final String GSF_PACKAGE = "com.google.android.gsf";
    public BasicInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        try {
            //获取getprop
            getProp();
        } catch (Exception e) {
            putFailedInfo("a1");
            XLog.e("HardwareCollector", "Failed to collect props: " + e.getMessage());
        }

        try {
            // 获取cpu架构
            archInfo();
        } catch (Exception e) {
            putFailedInfo("a2");
        }
        try {
            // 尝试获取设备型号
            putInfo("a8", Build.BRAND);
        } catch (Exception e) {
            putFailedInfo("a8");
        }
        try {
            // 尝试获取设备型号
            putInfo("a9", Build.MODEL);
        } catch (Exception e) {
            putFailedInfo("a9");
        }

        try {
            // 获取bootid
            getBootID();
        } catch (Exception e) {
            putFailedInfo("a4");
        }
        try {
            // 获取Androidid
            getAndroidID();
        } catch (Exception e) {
            putFailedInfo("a4");
        }
        try {
            // 获取Setting的字段
            getSettingVaule();
        } catch (Exception e) {
            putFailedInfo("a13");
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putInfo("a20", Build.getSerial());
            }else {
                putFailedInfo("a20");
            }
        } catch (Exception e) {
            putFailedInfo("a20");
        }
        try {
            getBluetoothAddress();
        } catch (Exception e) {
            putFailedInfo("a15");
        }
        try {
            putInfo("a11", Build.FINGERPRINT);
        } catch (Exception e) {
            putFailedInfo("a11");
        }
        try {
            putInfo("a10", context.getApplicationInfo().dataDir);
        } catch (Exception e) {
            putFailedInfo("a10");
        }
        try {
            getAppPath();

        }    catch (Exception e) {
        putFailedInfo("a14");
    }
        try {
            TimeZone timeZone = TimeZone.getDefault();
            putInfo("a12",timeZone.getID());
        }catch (Exception e){
            putFailedInfo("a12");
            XLog.e(Arrays.toString(e.getStackTrace()));
        }
        try {
            String drmId = getDrmIdSha256();
            if (!drmId.equals("Not available")) {
                putInfo("a6", drmId);
            } else {
                putFailedInfo("a6");
            }
        } catch (Exception e) {
            putFailedInfo("a6");
            XLog.e("DrmCollector", "Failed to collect DRM ID: " + e.getMessage());
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

    }
    private void archInfo (){
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
    }
    private void getProp() {
        XCommandUtil.CommandResult result = XCommandUtil.execute("getprop");
        if (result.isSuccess()) {
            Map<String, String> propMap = new LinkedHashMap<>();
            String content = result.getSuccessMsg();
            XLog.d(content);

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
    }
    private void getAndroidID(){
        String android_id_1 = "";
        String android_id_2 = "";
        String android_id_3 = "";
        Map<String, String> androidIdInfo = new LinkedHashMap<>();
        try {
            android_id_1 = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            Bundle bundle = context.getContentResolver().call(
                    Uri.parse("content://settings/secure"), "GET_secure", "android_id", new Bundle()
            );
            android_id_2 = bundle.getString("value");
            ArrayMap mValues = null;
            try {
                Field sNameValueCache = Settings.Secure.class.getDeclaredField("sNameValueCache");
                sNameValueCache.setAccessible(true);
                Object sLockSettings = sNameValueCache.get(null);
                Field fieldmValues = sLockSettings.getClass().getDeclaredField("mValues");
                fieldmValues.setAccessible(true);
                mValues = (ArrayMap<String,String>) fieldmValues.get(sLockSettings);
                android_id_3 = (String)mValues.get("android_id");
                //XLog.i(String.format("android_id -> 3333 %s", android_id));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            XLog.e(e.getMessage());
        }

        boolean android_id_B = XString.compareIgnoreSpaces(android_id_1,android_id_2,android_id_3);
        if (android_id_B){
            putInfo("a5",android_id_1);
        }else {
            androidIdInfo.put("androidSettings" ,android_id_1);
            androidIdInfo.put("androidbundle" ,android_id_1);
            androidIdInfo.put("androidCache",android_id_3);
            putInfo("a5",androidIdInfo);
        }
        try {
            // 收集所有获取包名的方法结果
            String contextPkg = context.getPackageName();
            String appInfoPkg = context.getApplicationInfo().packageName;
            String pmPkg = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).packageName;
            String buildConfigPkg = BuildConfig.APPPACKAGE;

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
            XLog.e("PackageInfoCollector", "Failed to collect package info: " + e.getMessage());
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
                XLog.i("GSFCollector", "Google Services not available on this device");
            }

            putInfo("a7", gsfInfo);

        } catch (Exception e) {
            putFailedInfo("a7");
            XLog.e("GSFCollector", "Failed to collect GSF info: " + e.getMessage());
        }
    }
    private void getSettingVaule (){
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
    }
    @SuppressLint("HardwareIds")
    private void getBluetoothAddress() {
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

    }
    private String getDrmIdSha256() {
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

            return hexString.toString();

        } catch (Exception e) {
            XLog.e("DrmCollector", "Failed to get SHA256 DRM ID: " + e.getMessage());
            return "Not available";
        } finally {
            if (mediaDrm != null) {
                try {
                    mediaDrm.close();
                } catch (Exception e) {
                    XLog.e("DrmCollector", "Failed to close MediaDrm: " + e.getMessage());
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
            XLog.e("GSFCollector", "Failed to get GSF ID from ContentResolver: " + e.getMessage());
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
            XLog.e("GSFCollector", "Failed to get GSF ID via command: " + e.getMessage());
        }

        // 方法3：通过 Settings.Secure（备选方法）
        try {
            gsfId = Settings.Secure.getString(context.getContentResolver(), "android_id");
            if (gsfId != null && !gsfId.isEmpty()) {
                return gsfId;
            }
        } catch (Exception e) {
            XLog.e("GSFCollector", "Failed to get GSF ID from Settings.Secure: " + e.getMessage());
        }

        return gsfId;
    }
}
