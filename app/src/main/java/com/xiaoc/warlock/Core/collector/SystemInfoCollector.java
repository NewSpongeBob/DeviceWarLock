package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    }
    /**
     * 收集传感器信息
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

                    if (key.equals("processor")) {
                        if (isNewProcessor) {
                            currentProcessor = new LinkedHashMap<>();
                            isNewProcessor = false;
                        }
                    } else if (key.equals("Features")) {
                        // Features 放在 regular 信息中
                        regularInfo.put(key, value);
                    } else if (key.equals("CPU variant") ||
                            key.equals("CPU part")) {
                        // 处理器特定信息
                        if (currentProcessor != null) {
                            currentProcessor.put(key, value);
                        }
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
}
