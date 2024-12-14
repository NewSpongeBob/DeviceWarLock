package com.xiaoc.warlock.Core.collector;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class MiscInfoCollector  extends BaseCollector {
    private String TAG = "MiscInfoCollector";

    public MiscInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        collectStorageInfo();      // a21
        collectMemoryInfo();       // a22
        collectBatteryHealth();    // a23
        collectBatteryCapacity();  // a24
        collectNetworkInterface(); // a41
        collectOpenGLVersion();    // a42
        collectProcessorCount();   // a43
        collectReflectionAvailable(); //a44
        collectNetworkCountry();   // a45
        collectNetState();         // a49
        collectScreenInfo();       // a54
        collectStorageSerial();    // a55
        collectDeviceSerial();     // a56
        collectStorageCID();       // a57
        collectInputDevices();     // a58
        collectInputMethod();      // a59
        collectAudioVolumes();     // a61

    }
    /**
     * 获取OpenGL ES版本
     */
    private void collectOpenGLVersion() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
                if (configInfo != null) {
                    // 获取OpenGL ES版本字符串并转换
                    int glEsVersion = configInfo.reqGlEsVersion;
                    int major = ((glEsVersion & 0xffff0000) >> 16);
                    int minor = (glEsVersion & 0xffff);
                    String version = major + "." + minor;
                    putInfo("a42", version);
                    return;
                }
            }

            // 备用方法：通过EGL获取
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            egl.eglInitialize(display, version);
            String openGLVersion = version[0] + "." + version[1];
            putInfo("a42", openGLVersion);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to get OpenGL ES version: " + e.getMessage());
            putFailedInfo("a42");
        }
    }
    /**
     * 获取处理器数量、处理器频率、处理器温度
     */
    private void collectProcessorCount() {
        try {
            Runtime runtime = Runtime.getRuntime();
            int processorCount = runtime.availableProcessors();

            Map<String, Object> cpuInfo = new LinkedHashMap<>();
            cpuInfo.put("count", processorCount);

            // 尝试获取CPU频率
            boolean freqSuccess = collectCpuFrequencies(cpuInfo, processorCount);

            // 如果获取频率失败，尝试获取温度
            if (!freqSuccess) {
                collectCpuTemperature(cpuInfo);
            }

            putInfo("a43", processorCount);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to get processor count: " + e.getMessage());
            putFailedInfo("a43");
        }
    }
    /**
     * 获取设备存储总字节数
     */
    private void collectStorageInfo() {
        try {
            File dataDir = Environment.getDataDirectory();
            StatFs statFs = new StatFs(dataDir.getPath());
            long totalBytes;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                totalBytes = statFs.getTotalBytes();
            } else {
                totalBytes = (long) statFs.getBlockSize() * statFs.getBlockCount();
            }

            putInfo("a21", totalBytes);
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get storage info: " + e.getMessage());
            putFailedInfo("a21");
        }
    }

    /**
     * 获取设备内存大小
     */
    private void collectMemoryInfo() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            putInfo("a22", memoryInfo.totalMem);
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get memory info: " + e.getMessage());
            putFailedInfo("a22");
        }
    }

    /**
     * 获取电池健康状态
     */
    private void collectBatteryHealth() {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);

            if (batteryStatus != null) {
                int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                String healthStatus;

                switch (health) {
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        healthStatus = "good";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        healthStatus = "overheat";
                        break;
                    default:
                        healthStatus = "unknown";
                        break;
                }

                putInfo("a23", healthStatus);
            } else {
                putFailedInfo("a23");
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get battery health: " + e.getMessage());
            putFailedInfo("a23");
        }
    }

    /**
     * 获取电池容量
     */
    private void collectBatteryCapacity() {
        Map<String, String> capacityMap = new LinkedHashMap<>();

        // 方法1: PowerProfile
        try {
            Object powerProfile = Class.forName("com.android.internal.os.PowerProfile")
                    .getConstructor(Context.class)
                    .newInstance(context);

            double capacity = (double) Class.forName("com.android.internal.os.PowerProfile")
                    .getMethod("getBatteryCapacity")
                    .invoke(powerProfile);

            capacityMap.put("power_profile", String.valueOf(capacity));
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get battery capacity from PowerProfile: " + e.getMessage());
        }

        // 方法2: 从系统文件读取
        try {
            String capacity = XFile.readFile("/sys/class/power_supply/battery/charge_full_design");
            if (!XString.isEmpty(capacity)) {
                // 转换为mAh
                long capacityMah = Long.parseLong(capacity) / 1000;
                capacityMap.put("sys_file", String.valueOf(capacityMah));
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get battery capacity from sys file: " + e.getMessage());
        }

        // 方法3: 通过BatteryManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                capacityMap.put("battery_manager", String.valueOf(capacity));
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get battery capacity from BatteryManager: " + e.getMessage());
        }

        // 方法4: 从Build.PROP读取
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get", String.class);
            Object capacity = getMethod.invoke(null, "ro.boot.hardware.battery.capacity");
            if (!XString.isEmpty(capacity.toString())) {
                capacityMap.put("build_prop", capacity.toString());
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get battery capacity from build prop: " + e.getMessage());
        }

        // 如果至少有一种方法成功
        if (!capacityMap.isEmpty()) {
            putInfo("a24", capacityMap);
        } else {
            putFailedInfo("a24");
        }
    }
    private void collectNetState(){
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                putFailedInfo("a49");
                return;
            }

            int dataState = telephonyManager.getDataState();
            int result;

            switch (dataState) {
                case TelephonyManager.DATA_CONNECTED:        // 2
                case TelephonyManager.DATA_CONNECTING:       // 1
                case TelephonyManager.DATA_SUSPENDED:        // 3
                    result = 3;  // 数据已开启
                    break;

                case TelephonyManager.DATA_DISCONNECTED:     // 0
                default:
                    result = 1;  // 数据未开启
                    break;
            }

            putInfo("a49", result);

        } catch (SecurityException e) {
            XLog.e(TAG, "No permission to access data state: " + e.getMessage());
            putFailedInfo("a49");
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get data state: " + e.getMessage());
            putFailedInfo("a49");
        }
    }
    private void collectNetworkCountry(){
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                putFailedInfo("a45");
                return;
            }

            // 获取网络国家代码
            String countryIso = telephonyManager.getNetworkCountryIso();

            // 检查是否有效
            if (!XString.isEmpty(countryIso)) {
                // 转换为小写并去除空格
                countryIso = countryIso.toLowerCase().trim();
                putInfo("a45", countryIso);
            } else {
                // 尝试从 SIM 卡获取
                String simCountryIso = telephonyManager.getSimCountryIso();
                if (!XString.isEmpty(simCountryIso)) {
                    simCountryIso = simCountryIso.toLowerCase().trim();
                    putInfo("a45", simCountryIso);
                } else {
                    // 如果都获取不到，尝试从系统区域设置获取
                    String locale = context.getResources().getConfiguration().locale.getCountry();
                    if (!XString.isEmpty(locale)) {
                        putInfo("a45", locale.toLowerCase());
                    } else {
                        putFailedInfo("a45");
                    }
                }
            }

        } catch (SecurityException e) {
            XLog.e(TAG, "No permission to access network country: " + e.getMessage());
            putFailedInfo("a45");
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get network country: " + e.getMessage());
            putFailedInfo("a45");
        }
    }
    private void collectNetworkInterface(){
        try {
            List<Map<String, Object>> interfaceList = new ArrayList<>();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    try {
                        Map<String, Object> interfaceInfo = new LinkedHashMap<>();

                        // 基本信息
                        interfaceInfo.put("n", networkInterface.getName());
                        interfaceInfo.put("u", networkInterface.isUp());

                        // 额外信息（如果需要）

                        interfaceInfo.put("display_name", networkInterface.getDisplayName());
                        interfaceInfo.put("loopback", networkInterface.isLoopback());
                        interfaceInfo.put("virtual", networkInterface.isVirtual());
                        interfaceInfo.put("point_to_point", networkInterface.isPointToPoint());
                        interfaceInfo.put("multicast", networkInterface.supportsMulticast());
                        interfaceInfo.put("mtu", networkInterface.getMTU());

                        // 获取硬件地址（MAC地址）
                        byte[] mac = networkInterface.getHardwareAddress();
                        if (mac != null) {
                            StringBuilder macBuilder = new StringBuilder();
                            for (byte b : mac) {
                                macBuilder.append(String.format("%02X:", b));
                            }
                            if (macBuilder.length() > 0) {
                                macBuilder.deleteCharAt(macBuilder.length() - 1);
                            }
                            interfaceInfo.put("mac", macBuilder.toString());
                        }

                        // 获取IP地址
                        List<String> addresses = new ArrayList<>();
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        while (inetAddresses.hasMoreElements()) {
                            addresses.add(inetAddresses.nextElement().getHostAddress());
                        }
                        if (!addresses.isEmpty()) {
                            interfaceInfo.put("addresses", addresses);
                        }


                        interfaceList.add(interfaceInfo);
                    } catch (Exception e) {
                        XLog.e(TAG,
                                "Failed to get interface info: " + e.getMessage());
                    }
                }
            }

            if (!interfaceList.isEmpty()) {
                putInfo("a41", interfaceList);
            } else {
                putFailedInfo("a41");
            }

        } catch (Exception e) {
            XLog.e(TAG,
                    "Failed to get network interfaces: " + e.getMessage());
            putFailedInfo("a41");
        }
    }
    /**
     * 获取CPU频率
     * @return 是否成功获取到频率
     */
    private boolean collectCpuFrequencies(Map<String, Object> cpuInfo, int processorCount) {
        try {
            List<Long> frequencies = new ArrayList<>();
            for (int i = 0; i < processorCount; i++) {
                String freqPath = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
                String freq = XFile.readFile(freqPath);
                if (freq != null && !freq.isEmpty()) {
                    try {
                        freq = freq.trim().replace("\n", "");
                        long frequency = Long.parseLong(freq) / 1000L; // 转换为MHz
                        frequencies.add(frequency);
                    } catch (NumberFormatException e) {
                        XLog.e(TAG, "Invalid frequency value: " + freq);
                    }
                }
            }
            if (!frequencies.isEmpty()) {
                cpuInfo.put("frequencies", frequencies);
                return true;
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get CPU frequencies: " + e.getMessage());
        }
        return false;
    }

    /**
     * 获取CPU温度
     */
    private void collectCpuTemperature(Map<String, Object> cpuInfo) {
        try {
            String tempPath = "/sys/class/thermal/thermal_zone0/temp";
            String temp = XFile.readFile(tempPath);
            if (temp != null && !temp.isEmpty()) {
                try {
                    temp = temp.trim().replace("\n", "");
                    float temperature = Float.parseFloat(temp) / 1000f; // 转换为摄氏度
                    cpuInfo.put("temperature", temperature);
                } catch (NumberFormatException e) {
                    XLog.e(TAG, "Invalid temperature value: " + temp);
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get CPU temperature: " + e.getMessage());
        }
    }
    /**
     * 检测当前设备是否能够正常调用反射
     */
    private void collectReflectionAvailable(){
        try {
            boolean reflectionAvailable = false;

            // 检查反射是否可用
            try {
                Class<?> statClass = Class.forName("android.system.StructStat");
                reflectionAvailable = true;
            } catch (Exception e) {
                reflectionAvailable = false;
            }

            putInfo("a44", reflectionAvailable);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to check reflection availability: " + e.getMessage());
            putFailedInfo("a44");
        }
    }
    /**
     * 获取屏幕亮度、屏幕宽高和屏幕超时时间
     */
    private  void collectScreenInfo(){
        try {
            Map<String, Object> result = new LinkedHashMap<>();

            // 获取屏幕亮度
            try {
                int brightness = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS);
                result.put("b", brightness);
            } catch (Exception e) {
                XLog.e(TAG, "Failed to get screen brightness: " + e.getMessage());
            }

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics realMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(realMetrics);
            result.put("w", realMetrics.widthPixels);
            result.put("h", realMetrics.heightPixels);

            // 获取屏幕超时时间（单位：毫秒）
            try {
                int timeout = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT);
                result.put("t", timeout);
            } catch (Exception e) {
                XLog.e(TAG, "Failed to get screen timeout: " + e.getMessage());
            }

            putInfo("a54", result);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect screen info: " + e.getMessage());
            putFailedInfo("a54");
        }
    }
    /**
     * 获取内部存储序列号
     */
    private void collectStorageSerial(){
        try {
            String serial = "";
            File file = new File(BuildConfig.STORAGE_SERIAL_PATH);

            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    serial = reader.readLine();
                    if (serial != null) {
                        serial = serial.trim();
                    }
                }
            }

            if (!XString.isEmpty(serial)) {
                putInfo("a55", serial);
            } else {
                putFailedInfo("a55");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to read storage serial: " + e.getMessage());
            putFailedInfo("a55");
        }
    }

    /**
     * 获取设备序列号
     */
    private void collectDeviceSerial(){
        try {
            String serial = "";
            File file = new File(BuildConfig.SERIAL_PATH);

            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    serial = reader.readLine();
                    if (serial != null) {
                        serial = serial.trim();
                    }
                }
            }

            if (!XString.isEmpty(serial)) {
                putInfo("a56", serial);
            } else {
                putFailedInfo("a56");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to read device serial: " + e.getMessage());
            putFailedInfo("a56");
        }
    }
    /**
     * 获取内部存储SD卡的CID
     */
    private void collectStorageCID(){
        try {
            String cid = "";
            File file = new File(BuildConfig.CID_PATH);

            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    cid = reader.readLine();
                    if (cid != null) {
                        cid = cid.trim();
                    }
                }
            }

            if (!XString.isEmpty(cid)) {
                putInfo("a57", cid);
            } else {
                putFailedInfo("a57");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to read storage CID: " + e.getMessage());
            putFailedInfo("a57");
        }
    }
    /**
     * 获取注册的input设备信息
     */
    private void collectInputDevices(){
        List<Map<String, String>> devices = null;

        // 首先尝试从文件读取
        devices = getDevicesFromFile();

        // 如果文件读取失败，尝试使用InputManager
        if (devices == null || devices.isEmpty()) {
            devices = getDevicesFromInputManager();
        }

        if (devices != null && !devices.isEmpty()) {
            putInfo("a58", devices);
        } else {
            putFailedInfo("a58");
        }
    }
    private List<Map<String, String>> getDevicesFromFile() {
        List<Map<String, String>> devices = new ArrayList<>();
        File file = new File(BuildConfig.DEVICES_PATH);

        if (file.exists() && file.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                Map<String, String> currentDevice = null;
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty()) {
                        if (currentDevice != null && !currentDevice.isEmpty()) {
                            devices.add(currentDevice);
                        }
                        currentDevice = new LinkedHashMap<>();
                        continue;
                    }

                    if (currentDevice == null) {
                        currentDevice = new LinkedHashMap<>();
                    }

                    // 解析各种设备信息
                    if (line.startsWith("N: Name=")) {
                        currentDevice.put("n", line.substring(8).replace("\"", "").trim());
                    } else if (line.startsWith("S: Sysfs=")) {
                        currentDevice.put("s", line.substring(8).replace("\"", "").trim());
                    } else if (line.startsWith("I: Bus=")) {
                        // 解析Bus、Vendor、Product、Version信息
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (part.startsWith("Bus=")) {
                                currentDevice.put("b", part.substring(4));
                            } else if (part.startsWith("Vendor=")) {
                                currentDevice.put("v", part.substring(7));
                            } else if (part.startsWith("Product=")) {
                                currentDevice.put("p", part.substring(8));
                            } else if (part.startsWith("Version=")) {
                                currentDevice.put("ver", part.substring(8));
                            }
                        }
                    } else if (line.startsWith("H: Handlers=")) {
                        currentDevice.put("h", line.substring(11).trim());
                    } else if (line.startsWith("B: PROP=")) {
                        currentDevice.put("prop", line.substring(8).trim());
                    }
                }

                // 添加最后一个设备
                if (currentDevice != null && !currentDevice.isEmpty()) {
                    devices.add(currentDevice);
                }
            } catch (Exception e) {
                XLog.e(TAG, "Failed to read from file: " + e.getMessage());
                return null;
            }
        }

        return devices;
    }

    @SuppressLint("MissingPermission")
    private List<Map<String, String>> getDevicesFromInputManager() {
        List<Map<String, String>> devices = new ArrayList<>();

        try {
            InputManager inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            int[] deviceIds = inputManager.getInputDeviceIds();

            for (int deviceId : deviceIds) {
                InputDevice device = inputManager.getInputDevice(deviceId);
                if (device != null) {
                    Map<String, String> deviceInfo = new LinkedHashMap<>();

                    // 基本信息
                    deviceInfo.put("n", device.getName());
                    deviceInfo.put("s", device.getDescriptor());

                    // 设备ID
                    deviceInfo.put("id", String.valueOf(device.getId()));

                    // 产品信息
                    deviceInfo.put("p", String.format("%04x", device.getProductId()));
                    deviceInfo.put("v", String.format("%04x", device.getVendorId()));

                    // 设备类型
                    StringBuilder sources = new StringBuilder();
                    int sources_raw = device.getSources();
                    if ((sources_raw & InputDevice.SOURCE_KEYBOARD) != 0) sources.append("keyboard ");
                    if ((sources_raw & InputDevice.SOURCE_TOUCHSCREEN) != 0) sources.append("touchscreen ");
                    if ((sources_raw & InputDevice.SOURCE_MOUSE) != 0) sources.append("mouse ");
                    if ((sources_raw & InputDevice.SOURCE_TOUCHPAD) != 0) sources.append("touchpad ");
                    deviceInfo.put("src", sources.toString().trim());

                    if (!deviceInfo.isEmpty()) {
                        devices.add(deviceInfo);
                    }
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get devices from InputManager: " + e.getMessage());
            return null;
        }

        return devices;
    }

    /**
     * 获取输入法列表的信息
     */
    private void collectInputMethod(){
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<Map<String, String>> inputMethods = new ArrayList<>();

            // 获取所有输入法列表
            List<InputMethodInfo> allInputMethods = imm.getInputMethodList();
            // 获取已启用的输入法列表
            List<InputMethodInfo> enabledInputMethods = imm.getEnabledInputMethodList();
            // 创建已启用输入法的ID集合，用于快速查找
            Set<String> enabledIds = new HashSet<>();
            if (enabledInputMethods != null) {
                for (InputMethodInfo imi : enabledInputMethods) {
                    enabledIds.add(imi.getId());
                }
            }

            if (allInputMethods != null) {
                for (InputMethodInfo imi : allInputMethods) {
                    Map<String, String> methodInfo = new LinkedHashMap<>();

                    // 获取输入法基本信息
                    methodInfo.put("id", imi.getId());  // 输入法ID
                    methodInfo.put("n", imi.loadLabel(context.getPackageManager()).toString());  // 输入法名称
                    methodInfo.put("p", imi.getPackageName());  // 包名
                    methodInfo.put("s", imi.getServiceName());  // 服务名

                    // 标记是否启用
                    methodInfo.put("e", String.valueOf(enabledIds.contains(imi.getId())));

                    // 获取输入法设置activity（如果有）
                    if (imi.getSettingsActivity() != null) {
                        methodInfo.put("a", imi.getSettingsActivity());
                    }

                    // 获取支持的语言列表
                    List<String> languages = new ArrayList<>();
                    for (int i = 0; i < imi.getSubtypeCount(); i++) {
                        InputMethodSubtype subtype = imi.getSubtypeAt(i);
                        String locale = subtype.getLocale();
                        if (!XString.isEmpty(locale) && !languages.contains(locale)) {
                            languages.add(locale);
                        }
                    }
                    if (!languages.isEmpty()) {
                        methodInfo.put("l", TextUtils.join(",", languages));
                    }

                    inputMethods.add(methodInfo);
                }
            }

            if (!inputMethods.isEmpty()) {
                // 获取当前默认输入法
                String defaultIme = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD
                );

                // 构建结果
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("d", defaultIme);  // 默认输入法
                result.put("l", inputMethods);  // 输入法列表

                putInfo("a59", result);
            } else {
                putFailedInfo("a59");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect input methods: " + e.getMessage());
            putFailedInfo("a59");
        }
    }
    /**
     * 收集设备音频流音量信息
     * 包含以下音频流类型的音量:
     * - STREAM_ALARM: 闹钟音量
     * - STREAM_MUSIC: 媒体音量
     * - STREAM_NOTIFICATION: 通知音量
     * - STREAM_RING: 铃声音量
     * - STREAM_SYSTEM: 系统音量
     * - STREAM_VOICE_CALL: 通话音量
     * 结果格式:
     * {
     *   "a": 音量值,     // alarm音量
     *   "m": 音量值,     // music音量
     *   "n": 音量值,     // notification音量
     *   "r": 音量值,     // ring音量
     *   "s": 音量值,     // system音量
     *   "v": 音量值      // voice_call音量
     * }
     */
    private void collectAudioVolumes() {
        try {
            // 获取AudioManager实例
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                putFailedInfo("a61");
                return;
            }

            // 创建音量信息Map
            Map<String, Integer> volumeInfo = new LinkedHashMap<>();

            // 获取闹钟音量
            int alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            volumeInfo.put("a", alarmVolume);

            // 获取媒体音量
            int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeInfo.put("m", musicVolume);

            // 获取通知音量
            int notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            volumeInfo.put("n", notificationVolume);

            // 获取铃声音量
            int ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            volumeInfo.put("r", ringVolume);

            // 获取系统音量
            int systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            volumeInfo.put("s", systemVolume);

            // 获取通话音量
            int voiceCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            volumeInfo.put("v", voiceCallVolume);

            // 保存收集到的音量信息
            putInfo("a61", volumeInfo);

        } catch (Exception e) {
            XLog.e("AudioCollector", "Failed to collect audio volumes: " + e.getMessage());
            putFailedInfo("a61");
        }
    }
}
