package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VirtualDetector extends BaseDetector {
    public VirtualDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    @Override
    public void detect() {
        checkEmulator();
        checkEmulatorMounts();
        checkSensorSize();
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
              InfoItem warning = new WarningBuilder("checkEmulator", null)
                      .addDetail("check", arch)
                      .addDetail("level", "high")
                      .build();
              reportAbnormal(warning);
          }

          // 检查特征文件
          for (String path : BuildConfig.EMULATOR_FILES) {
              File file = new File(path);
              if (file.exists()) {
                  abnormalDetails.add(path);
              }
          }

          // 当特征文件数量大于3个时创建警告
          if (abnormalDetails.size() > 3) {
              StringBuilder details = new StringBuilder();
              for (String detail : abnormalDetails) {
                  details.append(detail).append("\n");
              }

              InfoItem warning = new WarningBuilder("checkEmulator", null)
                      .addDetail("check", details.toString().trim())
                      .addDetail("level", "high")
                      .build();

              reportAbnormal(warning);
          }
       }catch (Exception e){{

       }}
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
}
