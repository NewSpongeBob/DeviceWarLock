package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.xiaoc.warlock.App;
import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.AppChecker;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class MiscDetector extends BaseDetector {
    private final TelephonyManager telephonyManager;
    private String TAG = "MiscDetector";

    public MiscDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

    }

    /**
     * 检测当前设备是否能够正常调用反射
     */
    private void checkReflectionAvailable(){
        try {
            boolean supported =AppChecker.isReflectionSupported();
                if (supported){{
                    InfoItem warning = new WarningBuilder("checkHideApi", null)
                            .addDetail("check", String.valueOf(supported))
                            .addDetail("level", "medium")
                            .build();

                    reportAbnormal(warning);
                }}

        } catch (Exception e) {
        }
    }
    /**
     * 检测App启动时是否会有异常堆栈
     */
    private void checkException(){
        try {
            boolean StackTraceBbnormal =AppChecker.isStackTraceBbnormal();
            if (StackTraceBbnormal){{
                StringBuilder details = new StringBuilder();
                for (String line : AppChecker.getStackTraceBbnormal()) {
                    details.append(line).append("\n");
                }
                InfoItem warning = new WarningBuilder("checkStackTrace", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }}

        } catch (Exception e) {
        }
    }

    /**
     * 检测当前手机是否有手机卡
     */
    private void checkSimCard() {
        try {
            if (telephonyManager == null) {
                XLog.e(TAG, "TelephonyManager is null");
                return;
            }

            boolean hasActiveSim = false;

            // 对于Android Q及以上版本，检查所有SIM卡槽
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int phoneCount = telephonyManager.getPhoneCount();
                for (int i = 0; i < phoneCount; i++) {
                    if (telephonyManager.getSimState(i) == TelephonyManager.SIM_STATE_READY) {
                        hasActiveSim = true;
                        break;
                    }
                }
            } else {
                // 对于低版本Android，只检查主卡槽
                hasActiveSim = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
            }

            // 如果没有可用的SIM卡，报告警告
            if (!hasActiveSim) {
                InfoItem warning = new WarningBuilder("checkSimCard", null)
                        .addDetail("check", "Not Found Sim")
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "未检测到可用的SIM卡");
            }

        } catch (SecurityException e) {
            XLog.e(TAG, "缺少必要权限", e);
        } catch (Exception e) {
            XLog.e(TAG, "检查SIM卡状态失败", e);
        }
    }

    /**
     * 检测当前设备是否存在shizuku
     */
    private void checkShizukuFiles() {
        try {
            List<String> foundFiles = new ArrayList<>();

            for (String path : BuildConfig.SHIZUKU_FILES) {
                if (XFile.exists(path)) {
                    foundFiles.add(path);
                    XLog.d(TAG, "发现Shizuku文件: " + path);
                }
            }

            if (!foundFiles.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String file : foundFiles) {
                    details.append(file).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkShizuku", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "low")
                        .build();

                reportAbnormal(warning);
            }
        } catch (Exception e) {
            XLog.e(TAG, "检查Shizuku文件失败", e);
        }
    }
    @Override
    public void detect() {
        checkReflectionAvailable();
        checkException();
        checkSimCard();
        checkShizukuFiles();
    }
}
