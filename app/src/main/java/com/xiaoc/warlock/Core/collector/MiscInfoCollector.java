package com.xiaoc.warlock.Core.collector;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XString;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        collectNetworkCountry();   // a45
        collectNetState();         // a49
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
}
