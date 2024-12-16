package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.xiaoc.warlock.App;
import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.AppChecker;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
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
    /**
     * 检测系统代理设置
     * 检查是否设置了HTTP代理，这可能表明设备正在被用于抓包分析
     */
    private void checkProxy() {
        try {
            String host = System.getProperty("http.proxyHost");
            String port = System.getProperty("http.proxyPort");

            if (host != null && !host.isEmpty()) {
                String proxyInfo = host + (port != null ? ":" + port : "");

                InfoItem warning = new WarningBuilder("checkProxy", null)
                        .addDetail("check", proxyInfo)
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "检测到代理设置: " + proxyInfo);
            }
        } catch (Exception e) {
            XLog.e(TAG, "检查代理设置失败", e);
        }
    }
    /**
     * 检测USB调试状态
     * 检查ADB调试是否启用，这是一个潜在的安全风险
     */
    private void checkAdbDebug() {
        try {
            int adbEnabled = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.ADB_ENABLED,
                    0);

            if (adbEnabled == 1) {
                InfoItem warning = new WarningBuilder("checkAdbDebug", null)
                        .addDetail("check", String.valueOf(true))
                        .addDetail("level", "low")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "USB调试已启用");
            }
        } catch (Exception e) {
            XLog.e(TAG, "检查USB调试状态失败", e);
        }
    }

    /**
     * 检测开发者选项状态
     * 检查开发者选项是否启用，这可能表明设备处于开发测试状态
     */
    private void checkDevelopmentSettings() {
        try {
            int developmentSettingsEnabled = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0);

            if (developmentSettingsEnabled == 1) {
                InfoItem warning = new WarningBuilder("checkDevelopmentSettings", null)
                        .addDetail("check", String.valueOf(true))
                        .addDetail("level", "low")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "开发者选项已启用");
            }
        } catch (Exception e) {
            XLog.e(TAG, "检查开发者选项状态失败", e);
        }
    }
    /**
     * 检测VPN连接状态
     * 检查设备是否正在使用VPN连接，这可能表明流量正在被重定向或监控
     */
    private void checkVpnConnection() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                XLog.e(TAG, "ConnectivityManager is null");
                return;
            }

            // 方法1：通过网络接口检测
            boolean vpnInUse = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        vpnInUse = true;
                    }
                }
            }

            // 方法2：通过检查VPN接口
            try {
                List<NetworkInterface> networks = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface network : networks) {
                    if (!vpnInUse && network.getName().startsWith("tun") || network.getName().startsWith("ppp")) {
                        vpnInUse = true;
                        break;
                    }
                }
            } catch (Exception e) {
                XLog.e(TAG, "检查网络接口失败", e);
            }

            if (vpnInUse) {
                InfoItem warning = new WarningBuilder("checkVpn", null)
                        .addDetail("check", String.valueOf(true))
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
                XLog.d(TAG, "检测到VPN连接");
            }

        } catch (Exception e) {
            XLog.e(TAG, "检查VPN状态失败", e);
        }
    }
    @Override
    public void detect() {
        checkReflectionAvailable();
        checkException();
        checkSimCard();
        checkShizukuFiles();
        checkProxy();
        checkAdbDebug();
        checkDevelopmentSettings();
        checkVpnConnection();
    }
}
