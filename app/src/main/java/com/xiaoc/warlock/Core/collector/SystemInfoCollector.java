package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.media.MediaDrm;
import android.os.StatFs;
import android.view.InputDevice;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XString;
import com.xiaoc.warlock.crypto.MD5Util;

import android.os.Process;

import java.io.File;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemInfoCollector extends BaseCollector {
    private static final String TAG = "SystemInfoCollector";

    public SystemInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        collectCpuInfo();     //a19
        collectSensors();     // a25
        collectMountStats();  // a26
        collectCameraInfo();    // a27
        collectInputDevices();  // a28
        collectMediaDrmProperties();    // a62
        collectWlan0Address();  // a63
        collectDiskInfo();      // a64
        collectArpInfo();       // a65
        collectIPv6Info();     // a66
        collectUnameInfo();     // a67
        collectAppUid();     // a68
        collectDataDirUid();     // a69
        collectTargetApkPaths();    // a70
        collectSystemFontHash();    // a80
        collectSystemServices();    // a75
    }
    /**
     * 收集传感器信息
     * 使用 getSensorList 获取设备上所有可用的传感器信息
     * 结果格式:
     * {
     *   "s": "传感器名称",
     *   "v": "传感器厂商",
     *   "t": "传感器类型",
     *   "m": "最大量程",
     *   "r": "分辨率",
     *   "p": "功耗(mA)"
     * }
     */
    private void collectSensors() {
        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) {
                putFailedInfo("a25");
                return;
            }

            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            List<Map<String, String>> sensorInfo = new ArrayList<>();

            for (Sensor sensor : sensorList) {
                Map<String, String> info = new LinkedHashMap<>();
                info.put("s", sensor.getName());
                info.put("v", sensor.getVendor());
                info.put("t", String.valueOf(sensor.getType()));
                info.put("m", String.valueOf(sensor.getMaximumRange()));
                info.put("r", String.valueOf(sensor.getResolution()));
                info.put("p", String.valueOf(sensor.getPower()));
                sensorInfo.add(info);
            }

            if (!sensorInfo.isEmpty()) {
                putInfo("a25", sensorInfo);
            } else {
                putFailedInfo("a25");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect sensors: " + e.getMessage());
            putFailedInfo("a25");
        }
    }

    /**
     * 收集挂载信息
     */
    private void collectMountStats() {
        try {
            String mountStats = XFile.readFile("/proc/self/mountstats");
            if (mountStats == null || mountStats.isEmpty()) {
                // 尝试从 /proc/mounts 读取
                mountStats = XFile.readFile("/proc/mounts");
            }

            if (mountStats != null && !mountStats.isEmpty()) {
                List<Map<String, String>> mountInfo = new ArrayList<>();
                String[] lines = mountStats.split("\n");

                for (String line : lines) {
                    try {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            Map<String, String> info = new LinkedHashMap<>();
                            info.put("d", parts[0]);  // device
                            info.put("p", parts[1]);  // path
                            info.put("t", parts[2]);  // type
                            mountInfo.add(info);
                        }
                    } catch (Exception e) {
                        XLog.e(TAG, "Failed to parse mount line: " + line);
                    }
                }

                if (!mountInfo.isEmpty()) {
                    putInfo("a26", mountInfo);
                } else {
                    putFailedInfo("a26");
                }
            } else {
                putFailedInfo("a26");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect mount stats: " + e.getMessage());
            putFailedInfo("a26");
        }
    }
    /**
     * 收集相机信息
     */
    private void collectCameraInfo() {
        try {
            List<Map<String, String>> cameraInfoList = new ArrayList<>();

            // 获取相机数量
            int numberOfCameras = Camera.getNumberOfCameras();

            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);

                Map<String, String> info = new LinkedHashMap<>();
                info.put("n", String.valueOf(i));
                info.put("t", cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back");
                info.put("o", String.valueOf(cameraInfo.orientation));

                cameraInfoList.add(info);
            }

            if (!cameraInfoList.isEmpty()) {
                putInfo("a27", cameraInfoList);
            } else {
                putFailedInfo("a27");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect camera info: " + e.getMessage());
            putFailedInfo("a27");
        }
    }

    /**
     * 收集输入设备信息
     */
    private void collectInputDevices() {
        try {
            InputManager inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            if (inputManager == null) {
                putFailedInfo("a28");
                return;
            }

            int[] deviceIds = inputManager.getInputDeviceIds();
            List<Map<String, String>> inputDeviceList = new ArrayList<>();

            for (int deviceId : deviceIds) {
                InputDevice device = inputManager.getInputDevice(deviceId);
                if (device != null) {
                    Map<String, String> info = new LinkedHashMap<>();

                    // 获取设备名称
                    String name = device.getName();
                    if (name == null) continue;

                    // 获取vendor ID
                    // 注意：这里vendor用0和1来表示，你可能需要根据实际情况调整逻辑
                    String vendor = "0";
                    if (device.getVendorId() > 0) {
                        vendor = "1";
                    }

                    info.put("v", vendor);
                    info.put("n", name);

                    inputDeviceList.add(info);
                }
            }

            if (!inputDeviceList.isEmpty()) {
                putInfo("a28", inputDeviceList);
            } else {
                putFailedInfo("a28");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect input devices: " + e.getMessage());
            putFailedInfo("a28");
        }
    }
    private void collectCpuInfo(){
        try {
            String cpuInfo = XFile.readFile("/proc/cpuinfo");
            if (cpuInfo == null || cpuInfo.isEmpty()) {
                putFailedInfo("a19");
                return;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, String> commonInfo = new LinkedHashMap<>();
            Map<String, String> regularInfo = new LinkedHashMap<>();
            List<Map<String, String>> uniqueInfo = new ArrayList<>();

            // 按处理器分割内容
            String[] processors = cpuInfo.split("\n\n");
            Map<String, String> currentProcessor = null;

            // 先处理第一个处理器的信息来获取通用信息
            if (processors.length > 0) {
                String[] lines = processors[0].split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) continue;

                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // 收集通用信息
                    switch (key) {
                        case "Hardware":
                        case "Processor":
                        case "Model name":
                        case "CPU implementer":
                        case "CPU architecture":
                        case "CPU revision":
                        case "BogoMIPS":    // 性能指标
                        case "Serial":      // 序列号
                        case "Model":       // 型号
                            commonInfo.put(key, value);
                            break;
                    }
                }
            }

            // 处理所有处理器的信息
            for (String processor : processors) {
                String[] lines = processor.split("\n");
                boolean isNewProcessor = true;

                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) continue;

                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    switch (key) {
                        case "processor":
                            if (isNewProcessor) {
                                currentProcessor = new LinkedHashMap<>();
                                isNewProcessor = false;
                            }
                            break;
                        case "Features":
                            // Features 放在 regular 信息中
                            regularInfo.put(key, value);
                            break;
                        case "CPU variant":
                        case "CPU part":
                            // 处理器特定信息
                            if (currentProcessor != null) {
                                currentProcessor.put(key, value);
                            }
                            break;
                    }
                }

                if (currentProcessor != null && !currentProcessor.isEmpty()) {
                    uniqueInfo.add(new LinkedHashMap<>(currentProcessor));
                }
            }

            result.put("c", commonInfo);    // common info
            result.put("r", regularInfo);   // regular info
            result.put("u", uniqueInfo);    // unique info

            putInfo("a19", result);

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect CPU info: " + e.getMessage());
            putFailedInfo("a19");
        }

    }
    /**
     * 收集 MediaDrm 字符串属性信息
     * 获取以下属性:
     * - vendor: 供应商名称
     * - version: DRM版本
     * - description: 描述信息
     * - algorithms: 支持的算法
     * - systemId: 系统ID
     * 结果格式:
     * {
     *   "v": "供应商",
     *   "ver": "版本",
     *   "d": "描述",
     *   "a": "算法",
     *   "s": "系统ID"
     * }
     */
    private void collectMediaDrmProperties() {
        MediaDrm mediaDrm = null;
        try {
            // 使用 Widevine UUID 创建 MediaDrm 实例
            UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
            mediaDrm = new MediaDrm(WIDEVINE_UUID);

            Map<String, String> drmInfo = new LinkedHashMap<>();

            // 获取供应商信息
            String vendor = mediaDrm.getPropertyString(MediaDrm.PROPERTY_VENDOR);
            if (!XString.isEmpty(vendor)) {
                drmInfo.put("v", vendor);
            }

            // 获取版本信息
            String version = mediaDrm.getPropertyString(MediaDrm.PROPERTY_VERSION);
            if (!XString.isEmpty(version)) {
                drmInfo.put("ver", version);
            }

            // 获取描述信息
            String description = mediaDrm.getPropertyString(MediaDrm.PROPERTY_DESCRIPTION);
            if (!XString.isEmpty(description)) {
                drmInfo.put("d", description);
            }

            // 获取算法信息
            String algorithms = mediaDrm.getPropertyString(MediaDrm.PROPERTY_ALGORITHMS);
            if (!XString.isEmpty(algorithms)) {
                drmInfo.put("a", algorithms);
            }

            // 获取系统ID
            String systemId = mediaDrm.getPropertyString("systemId");
            if (!XString.isEmpty(systemId)) {
                drmInfo.put("s", systemId);
            }

            if (!drmInfo.isEmpty()) {
                putInfo("a62", drmInfo);
            } else {
                putFailedInfo("a62");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect MediaDrm properties: " + e.getMessage());
            putFailedInfo("a62");
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
     * 收集 wlan0 MAC 地址信息
     * 从两个路径获取:
     * - /sys/class/net/wlan0/address
     * - /sys/devices/virtual/net/wlan0/address
     * 结果格式:
     * 如果两个地址相同:
     * {
     *   "v": "MAC地址"
     * }
     * 如果两个地址不同:
     * {
     *   "c": "class路径下的MAC地址",
     *   "d": "devices路径下的MAC地址"
     * }
     */
    private void collectWlan0Address() {
        try {
            String classPath = "/sys/class/net/wlan0/address";
            String devicesPath = "/sys/devices/virtual/net/wlan0/address";

            // 读取两个路径的地址
            String classAddress = XFile.readFile(classPath);
            String devicesAddress = XFile.readFile(devicesPath);

            // 去除可能的空白字符
            if (classAddress != null) {
                classAddress = classAddress.trim();
            }
            if (devicesAddress != null) {
                devicesAddress = devicesAddress.trim();
            }

            // 如果两个地址都为空,标记失败
            if (XString.isEmpty(classAddress) && XString.isEmpty(devicesAddress)) {
                putFailedInfo("a63");
                return;
            }

            // 比较两个地址是否相同
            if (XString.compareExact(classAddress, devicesAddress)) {
                // 如果相同,只保存一个值
                putInfo("a63", classAddress);
            } else {
                // 如果不同,分别保存
                Map<String, String> addressMap = new LinkedHashMap<>();
                if (!XString.isEmpty(classAddress)) {
                    addressMap.put("c", classAddress);
                }
                if (!XString.isEmpty(devicesAddress)) {
                    addressMap.put("d", devicesAddress);
                }
                putInfo("a63", addressMap);
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect wlan0 address: " + e.getMessage());
            putFailedInfo("a63");
        }
    }
    /**
     * 收集硬盘信息
     * 通过多种方式获取存储信息:
     * - StatFs API获取基本存储信息
     * - XCommandUtil执行stat命令获取文件系统信息
     * 结果格式:
     * {
     *   "t": "总字节数",
     *   "f": "空闲字节数",
     *   "a": "可用字节数",
     *   "bs": "块大小",
     *   "s": "stat命令输出"
     * }
     */
    private void collectDiskInfo() {
        try {
            Map<String, String> diskInfo = new LinkedHashMap<>();

            // 使用StatFs获取存储信息
            StatFs statFs = new StatFs("/storage/emulated/0");

            // 获取总字节数
            long totalBytes = statFs.getTotalBytes();
            diskInfo.put("t", String.valueOf(totalBytes));

            // 获取空闲字节数
            long freeBytes = statFs.getFreeBytes();
            diskInfo.put("f", String.valueOf(freeBytes));

            // 获取可用字节数
            long availableBytes = statFs.getAvailableBytes();
            diskInfo.put("a", String.valueOf(availableBytes));

            // 获取块大小
            long blockSize = statFs.getBlockSizeLong();
            diskInfo.put("bs", String.valueOf(blockSize));

            // 使用XCommandUtil执行stat命令获取文件系统信息
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat -f /storage/emulated/0");
            if (result.isSuccess() && !XString.isEmpty(result.getSuccessMsg())) {
                diskInfo.put("s", result.getSuccessMsg().trim());
            }

            if (!diskInfo.isEmpty()) {
                putInfo("a64", diskInfo);
            } else {
                putFailedInfo("a64");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect disk info: " + e.getMessage());
            putFailedInfo("a64");
        }
    }
    /**
     * 收集ARP表信息
     * 优先从/proc/net/arp读取,失败则使用ip neigh show命令
     * 结果格式:
     * [{
     *   "ip": "IP地址",
     *   "hw": "硬件地址(MAC)",
     *   "d": "设备名称"
     * }]
     */
    private void collectArpInfo() {
        try {
            List<Map<String, String>> arpList = new ArrayList<>();
            boolean success = false;

            // 首先尝试读取/proc/net/arp文件
            try {
                String arpContent = XFile.readFile("/proc/net/arp");
                if (!XString.isEmpty(arpContent)) {
                    String[] lines = arpContent.split("\n");
                    // 跳过标题行
                    for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (!line.isEmpty()) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 6) {
                                Map<String, String> entry = new LinkedHashMap<>();
                                entry.put("ip", parts[0]);  // IP address
                                entry.put("hw", parts[3]);  // HW address
                                entry.put("d", parts[5]);   // Device
                                arpList.add(entry);
                            }
                        }
                    }
                    success = true;
                }
            } catch (Exception e) {
                XLog.d(TAG, "Failed to read /proc/net/arp: " + e.getMessage());
            }

            // 如果读取文件失败,尝试使用ip neigh show命令
            if (!success) {
                XCommandUtil.CommandResult result = XCommandUtil.execute("ip neigh show");
                if (result.isSuccess()) {
                    String[] lines = result.getSuccessMsg().split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            // 解析命令输出,格式类似:
                            // 192.168.1.1 dev wlan0 lladdr 00:11:22:33:44:55 REACHABLE
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 6) {
                                Map<String, String> entry = new LinkedHashMap<>();
                                entry.put("ip", parts[0]);          // IP address
                                entry.put("hw", parts[4]);          // HW address (lladdr后面的值)
                                entry.put("d", parts[2]);           // Device (dev后面的值)
                                arpList.add(entry);
                            }
                        }
                    }
                    success = true;
                }
            }

            if (success && !arpList.isEmpty()) {
                putInfo("a65", arpList);
            } else {
                putFailedInfo("a65");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect ARP info: " + e.getMessage());
            putFailedInfo("a65");
        }
    }
    /**
     * 收集设备IPv6地址信息
     * 优先使用NetworkInterface API获取,失败则使用ip命令
     * 结果格式:
     * [{
     *   "a": "IPv6地址",
     *   "i": "网络接口名称"
     * }]
     */
    private void collectIPv6Info() {
        try {
            List<Map<String, String>> ipv6List = new ArrayList<>();
            boolean success = false;

            // 首先尝试使用NetworkInterface API获取
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet6Address) {
                            Map<String, String> ipInfo = new LinkedHashMap<>();
                            ipInfo.put("a", address.getHostAddress());    // IPv6地址
                            ipInfo.put("i", networkInterface.getName());  // 接口名称
                            ipv6List.add(ipInfo);
                            success = true;
                        }
                    }
                }
            } catch (Exception e) {
                XLog.d(TAG, "Failed to get IPv6 from NetworkInterface: " + e.getMessage());
            }

            // 如果API方法失败,使用ip命令获取
            if (!success) {
                XCommandUtil.CommandResult result = XCommandUtil.execute("ip -6 addr show");
                if (result.isSuccess()) {
                    String[] lines = result.getSuccessMsg().split("\n");
                    String currentInterface = "";

                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        // 解析接口名
                        if (line.contains(": ")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length > 1) {
                                currentInterface = parts[1].trim().split("\\s+")[0];
                            }
                            continue;
                        }

                        // 解析IPv6地址
                        if (line.contains("inet6")) {
                            String[] parts = line.trim().split("\\s+");
                            for (String part : parts) {
                                if (part.contains(":")) {  // IPv6地址包含冒号
                                    Map<String, String> ipInfo = new LinkedHashMap<>();
                                    ipInfo.put("a", part.split("/")[0]);  // 去掉前缀长度
                                    ipInfo.put("i", currentInterface);
                                    ipv6List.add(ipInfo);
                                    success = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (success && !ipv6List.isEmpty()) {
                putInfo("a66", ipv6List);
            } else {
                putFailedInfo("a66");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect IPv6 info: " + e.getMessage());
            putFailedInfo("a66");
        }
    }
    /**
     * 收集系统uname信息
     * 使用 uname -a 命令获取系统信息,包括:
     * - 内核名称
     * - 主机名
     * - 内核版本
     * - 系统架构等
     * 结果格式:
     * {
     *   "v": "完整的uname输出信息"
     * }
     */
    private void collectUnameInfo() {
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("uname -a");
            if (result.isSuccess() && !XString.isEmpty(result.getSuccessMsg())) {
                // 去除首尾空白字符
                String unameInfo = result.getSuccessMsg().trim();
                putInfo("a67", unameInfo);
            } else {
                putFailedInfo("a67");
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect uname info: " + e.getMessage());
            putFailedInfo("a67");
        }
    }
    /**
     * 收集应用UID信息
     * 获取当前应用的User ID,可通过以下方式:
     * - Process.myUid()
     * - context.getApplicationInfo().uid
     * 结果格式:
     * {
     *   "v": "应用UID"
     * }
     */
    private void collectAppUid() {
        try {
            // 方法一: 通过Process获取
            int uid = Process.myUid();

            // 方法二: 通过ApplicationInfo获取(作为备选验证)
            int appUid = context.getApplicationInfo().uid;

            // 两种方法获取的结果应该一致
            if (uid == appUid) {
                putInfo("a68", String.valueOf(uid));
            } else {
                // 如果不一致,保存两个值
                Map<String, String> uidInfo = new LinkedHashMap<>();
                uidInfo.put("p", String.valueOf(uid));      // Process获取的UID
                uidInfo.put("a", String.valueOf(appUid));   // ApplicationInfo获取的UID
                putInfo("a68", uidInfo);
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect app UID: " + e.getMessage());
            putFailedInfo("a68");
        }
    }
    /**
     * 收集应用数据目录权限信息
     * 通过ls -l命令获取/data/data/包名目录的权限信息
     * 从输出中提取uid信息(u0_axxx格式)
     * 结果格式:
     * {
     *   "v": "用户ID" // 例如: u0_a325
     * }
     */
    private void collectDataDirUid() {
        try {
            String packagePath = "/data/data/" + context.getPackageName();
            XCommandUtil.CommandResult result = XCommandUtil.execute("ls -l " + packagePath);

            if (result.isSuccess() && !XString.isEmpty(result.getSuccessMsg())) {
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");

                // 查找包含u0_a的行
                for (String line : lines) {
                    if (line.contains("u0_a")) {
                        // 使用正则表达式提取u0_axxx格式的uid
                        Pattern pattern = Pattern.compile("u0_a\\d+");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String uid = matcher.group();
                            putInfo("a69", uid);
                            return;
                        }
                    }
                }
                putFailedInfo("a69");
            } else {
                putFailedInfo("a69");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect data dir uid: " + e.getMessage());
            putFailedInfo("a69");
        }
    }
    /**
     * 收集指定应用的APK路径信息
     * 包括:
     * - 美团
     * - 微信
     * - MT管理器
     * - 快手
     * - 抖音
     * - 支付宝
     * 结果格式:
     * [{
     *   "p": "包名",
     *   "s": "源文件路径"  // /data/app/xxx/base.apk
     * }]
     */
    private void collectTargetApkPaths() {
        try {

            List<Map<String, String>> apkList = new ArrayList<>();
            PackageManager pm = context.getPackageManager();

            for (String packageName : BuildConfig.targetPackages) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    String sourceDir = appInfo.sourceDir;
                    if (!XString.isEmpty(sourceDir)) {
                        Map<String, String> info = new LinkedHashMap<>();
                        info.put("p", packageName);
                        info.put("s", sourceDir);
                        apkList.add(info);
                    }
                } catch (Exception ignored) {
                }
            }

            if (!apkList.isEmpty()) {
                putInfo("a70", apkList);
            } else {
                putFailedInfo("a70");
            }

        } catch (Exception e) {
            putFailedInfo("a70");
        }
    }

    /**
     * 遍历/system/fonts目录下的tf字体文件
     */
    private void collectSystemFontHash() {
        File fontDir = new File("/system/fonts");
        StringBuilder allHashes = new StringBuilder();

        if (fontDir.exists()) {
            Collection<File> fontFiles = XFile.listFiles(fontDir, new String[]{"ttf"}, true);

            for (File font : fontFiles) {
                String md5;
                try {
                    md5 = MD5Util.md5(XFile.readFileToByteArray(font));
                    // 将每个MD5拼接到字符串中
                    allHashes.append(md5);
                } catch (Exception e) {
                    putFailedInfo("a80");
                }
            }

            // 生成最终的MD5
            if (allHashes.length() > 0) {
                String finalMd5 = MD5Util.md5(allHashes.toString().getBytes());
                putInfo("a80", finalMd5);
            }else {
                putFailedInfo("a80");
            }
        } else {
            putFailedInfo("a80");
        }

    }
    /**
     * 采集系统服务列表信息（指纹字段a75）
     */
    private void collectSystemServices() {
        try {
            List<Map<String, Object>> servicesList = new ArrayList<>();

            // 方法1：优先尝试通过ServiceManager反射获取
            List<String> services = getSystemServicesViaReflection();

            // 方法2：如果反射失败，尝试通过service list命令获取
            if (services.isEmpty()) {
                services = getSystemServicesViaCommand();
            }

            // 方法3：如果前两种方法都失败，尝试通过dumpsys获取
            if (services.isEmpty()) {
                services = getSystemServicesViaDumpsys();
            }

            // 转换为结构化数据
            for (String serviceName : services) {
                Map<String, Object> serviceInfo = new LinkedHashMap<>();
                serviceInfo.put("n", serviceName);  // 服务名称

                // 尝试获取服务实例并检查是否可用
                try {
                    Object service = context.getSystemService(serviceName);
                    serviceInfo.put("a", service != null);  // 是否可访问
                } catch (Exception e) {
                    serviceInfo.put("a", false);
                }

                servicesList.add(serviceInfo);
            }

            if (!servicesList.isEmpty()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("c", services.size());  // 服务总数
                result.put("l", servicesList);     // 服务列表详情

                // 添加额外信息：关键服务是否存在
                Map<String, Boolean> criticalServices = new LinkedHashMap<>();
                criticalServices.put("window", services.contains("window"));
                criticalServices.put("power", services.contains("power"));
                criticalServices.put("activity", services.contains("activity"));
                criticalServices.put("package", services.contains("package"));
                result.put("k", criticalServices);

                putInfo("a75", result);
            } else {
                putFailedInfo("a75");
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect system services: " + e.getMessage());
            putFailedInfo("a75");
        }
    }

    /**
     * 通过反射ServiceManager获取系统服务列表
     */
    private List<String> getSystemServicesViaReflection() {
        List<String> services = new ArrayList<>();
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method listServicesMethod = serviceManagerClass.getMethod("listServices");
            String[] serviceNames = (String[]) listServicesMethod.invoke(null);
            if (serviceNames != null) {
                services.addAll(Arrays.asList(serviceNames));
            }
        } catch (Exception e) {
            XLog.d(TAG, "Reflection method failed, fallback to next method");
        }
        return services;
    }

    /**
     * 执行service list命令获取系统服务列表
     */
    private List<String> getSystemServicesViaCommand() {
        List<String> services = new ArrayList<>();
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("service list");
            if (result.isSuccess()) {
                String[] lines = result.getSuccessMsg().split("\n");
                for (String line : lines) {
                    if (line.contains(": [")) {
                        String serviceName = line.split(": \\[")[0].trim();
                        services.add(serviceName);
                    }
                }
            }
        } catch (Exception e) {
            XLog.d(TAG, "Service command failed, fallback to next method");
        }
        return services;
    }

    /**
     * dumpsys命令获取系统服务列表
     */
    private List<String> getSystemServicesViaDumpsys() {
        List<String> services = new ArrayList<>();
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("dumpsys -l");
            if (result.isSuccess()) {
                String[] lines = result.getSuccessMsg().split("\n");
                for (String line : lines) {
                    if (!line.startsWith(" ") && !line.isEmpty()) {
                        services.add(line.trim());
                    }
                }
            }
        } catch (Exception e) {
            XLog.d(TAG, "Dumpsys method failed");
        }
        return services;
    }
}
