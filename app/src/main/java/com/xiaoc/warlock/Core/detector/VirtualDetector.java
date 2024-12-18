package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VirtualDetector extends BaseDetector {
    private String TAG = "VirtualDetector";
    public VirtualDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    @Override
    public void detect() {
        checkEmulator();
        checkEmulatorMounts();
        checkSensorSize();
        checkEmulatorProps();
    }
    private String getSystemProperty(String prop) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method method = cls.getMethod("get", String.class);
            return (String) method.invoke(null, prop);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 检测设备的摄像头个数
     */
    private void checkCameraSize() {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) return;

            String[] cameraIds = manager.getCameraIdList();


            if (cameraIds.length < BuildConfig.CAMERA_MINIMUM_QUANTITY_LIMIT) {
                InfoItem warning = new WarningBuilder("checkCameraSize", null)
                        .addDetail("cameraSize", String.valueOf(cameraIds.length))
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
            }
        } catch (Exception e) {
        }
    }
    /**
     * 检查模拟器特征
     */
    private void checkEmulator() {
        try {
            List<String> abnormalDetails = new ArrayList<>();

            // 检查CPU架构
            String arch = System.getProperty("os.arch");
            if (arch != null && (arch.contains("x86_64") || arch.contains("x86"))) {
                abnormalDetails.add(arch);
            }

            // 检查特征文件
            List<String> foundFiles = new ArrayList<>();
            for (String path : BuildConfig.EMULATOR_FILES) {
                if (XFile.exists(path)) {
                    foundFiles.add(path);
                }
            }

            // 如果特征文件数量大于2个，添加到异常列表
            if (foundFiles.size() > 2) {
                abnormalDetails.addAll(foundFiles);
            }

            if (!abnormalDetails.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String detail : abnormalDetails) {
                    details.append(detail).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkEmulator", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "检测到模拟器特征: " + details);
            }
        } catch (Exception e) {
            XLog.e(TAG, "检查模拟器状态失败", e);
        }
    }
    /**
     * 检查模拟器挂载点
     */
    private void checkEmulatorMounts() {
        try {
            List<String> abnormalDetails = new ArrayList<>();

            XCommandUtil.CommandResult result = XCommandUtil.execute("cat /proc/mounts");
            if (result.isSuccess()) {
                String mounts = result.getSuccessMsg();

                for (String path : BuildConfig.EMULATOR_MOUNT_PATHS) {
                    if (mounts.contains(path)) {
                        abnormalDetails.add(path);
                    }
                }
            }

            // 如果发现异常，创建警告
            if (!abnormalDetails.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String detail : abnormalDetails) {
                    details.append(detail).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkEmulatorMount", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }
        }catch (Exception e){

        }

    }
    /**
     * 检查传感器数量
     */
    private void checkSensorSize(){
        try {
            //3,检测传感器类型,支持的全部类型传感器
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensorlist = sm.getSensorList(Sensor.TYPE_ALL);

            ArrayList<Integer> sensorTypeS = new ArrayList<>();
            for (Sensor sensor : sensorlist) {
                //获取传感器类型
                int type = sensor.getType();
                if (!sensorTypeS.contains(type)) {
                    //发现一种类型则添加一种类型
                    sensorTypeS.add(type);
                }
            }

            //我们认为传感器少于20个则认为是风险设备
            if (sensorlist.size() < BuildConfig.SENSOR_MINIMUM_QUANTITY_LIMIT) {
                InfoItem warning = new WarningBuilder("checkSensorSize", null)
                        .addDetail("sensorSize", String.valueOf(sensorlist.size()))
                        .addDetail("level", "high")
                        .build();
                reportAbnormal(warning);

            }
        }catch (Exception e){

        }
    }

    /**
     * 检测设备上的prop是否有模拟器的属性
     */
    private void checkEmulatorProps() {
        try {


            List<String> foundProps = new ArrayList<>();
            for (String prop : BuildConfig.QEMU_PROPS) {
                String value = getSystemProperty(prop);
                if (value != null && !value.isEmpty()) {
                    foundProps.add(prop + "=" + value);
                }
            }

            if (!foundProps.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String prop : foundProps) {
                    details.append(prop).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkEmulatorProps", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }
        } catch (Exception e) {
        }
    }
}
