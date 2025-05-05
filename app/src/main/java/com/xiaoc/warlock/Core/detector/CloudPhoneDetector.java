package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.BatteryManager;
import android.os.Build;

import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CloudPhoneDetector extends BaseDetector {
    private static final String TAG = "BatteryDetector";
    private final BatteryManager batteryManager;
    private boolean isDetecting = false;

    private String DEV_PATH = "/dev";
    public CloudPhoneDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
        batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    @Override
    public void detect() {
        if (isDetecting) return;
        isDetecting = true;
        checkBatteryStatus();
        checkAOSPSensors();
        checkDevDirectory();
        checkOMXNames();
    }

    private void checkBatteryStatus() {
        try {
            List<String> abnormalDetails = new ArrayList<>();

            // 方法1: 检查基本电池信息
            Intent batteryStatus = context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (batteryStatus == null) return;

            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

            // 方法2: 检查充电功率
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

                if (isCharging(plugged) && voltage != -1 && currentNow != -1) {
                    float voltageInVolts = voltage / 1000f;
                    float currentInAmperes = Math.abs(currentNow / 1000000f);
                    float chargingPower = voltageInVolts * currentInAmperes;

                    // 检查是否存在异常的充电功率（可能是云手机特征）
                    if (chargingPower > 300) {
                        abnormalDetails.add(String.format("Power: %.2fW", chargingPower));
                    }
                }
            }

            // 方法3: 检查温度异常
            if (temperature != -1) {
                float temp = temperature / 10f;
                // 检查温度是否在正常范围内
                if (temp < 10 || temp > 50) {
                    abnormalDetails.add(String.format("Temperature: %.1f°C", temp));
                }
            }

            // 方法4: 检查电池健康状态
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN);
            if (health == BatteryManager.BATTERY_HEALTH_UNKNOWN ||
                    health == BatteryManager.BATTERY_HEALTH_DEAD) {
                abnormalDetails.add("Health: " + getBatteryHealthString(health));
            }

            // 如果发现异常情况，生成报告
            if (!abnormalDetails.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String detail : abnormalDetails) {
                    details.append(detail).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkBattery", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }

        } catch (Exception e) {
            XLog.e(TAG, "Battery detection failed", e);
        }
    }

    /**
     * 检查设备是否正在充电
     */
    private boolean isCharging(int plugged) {
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    /**
     * 获取电池健康状态的描述
     */
    private String getBatteryHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Unspecified failure";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }
    private void checkAOSPSensors() {
        try {
            // 需要Android N及以上版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

                // 收集所有AOSP传感器
                List<String> aospSensors = new ArrayList<>();
                for (Sensor sensor : sensorList) {
                    if (sensor.getVendor().contains("AOSP")) {
                        aospSensors.add(String.format("%s (%s)",
                                sensor.getName(),
                                sensor.getVendor()));
                    }
                }

                // 如果AOSP传感器数量过多，可能是模拟器或云手机
                if (aospSensors.size() > 3) {
                    StringBuilder details = new StringBuilder();
                    details.append(String.format("AOSP Sensors: %d/%d\n",
                            aospSensors.size(),
                            sensorList.size()));

                    // 最多显示前3个传感器
                    for (int i = 0; i < Math.min(3, aospSensors.size()); i++) {
                        details.append("Sensor: ").append(aospSensors.get(i)).append("\n");
                    }

                    if (aospSensors.size() > 3) {
                        details.append("... and ").append(aospSensors.size() - 3).append(" more");
                    }

                    InfoItem warning = new WarningBuilder("checkAOSPSensors", null)
                            .addDetail("check", details.toString().trim())
                            .addDetail("level", "high")
                            .build();

                    reportAbnormal(warning);
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "AOSP sensors detection failed", e);
        }
    }

    /**
     * 扫描 /dev 目录，查找可能的 RK3588 控制器节点
     * @return
     */
    private void  checkDevDirectory() {
        List<String> nodes = new ArrayList<>();
        File devDir = new File(DEV_PATH);
        if (devDir.exists() && devDir.isDirectory()) {
            File[] files = devDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    // 查找常见的 RK3588 相关节点，例如 video、rk 等
                    if (name.startsWith("video") && name.startsWith("rk")) {
                        nodes.add(name);
                    }
                }
            }
            if (!nodes.isEmpty()){
                InfoItem warning = new WarningBuilder("checkDev", null)
                        .addDetail("check", nodes.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }
        } else {
            XLog.e(TAG, "Cannot access /dev directory. Permission denied or not available.");
        }
    }

    /**
     * 通过检测MediaCodec API 的名称是否包含OMX以及rk。
     */
    private  void checkOMXNames() {
        List<String> omxNames = new ArrayList<>();
        try {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
                String codecName = codecInfo.getName();
                // OMX 名称通常以 "OMX." 开头
                if (codecName.startsWith("OMX.") && codecName.contains("rk")) {
                    omxNames.add(codecName);
                }
            }
            if (!omxNames.isEmpty()){
                InfoItem warning = new WarningBuilder("checkMediaCodec", null)
                        .addDetail("check", omxNames.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }

        } catch (Exception e) {
            XLog.e(TAG, "Error retrieving OMX names: " + e.getMessage());
        }
    }
}