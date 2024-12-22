package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.content.pm.PackageManager;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;
import com.xiaoc.warlock.Util.WarningBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RootDetector extends BaseDetector {
    private Method getPropMethod;
    private String TAG = "RootDetector";
    public RootDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
        initSystemProperties();

    }
    private void initSystemProperties() {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            getPropMethod = cls.getMethod("get", String.class);
        } catch (Exception e) {
            XLog.e("BootloaderDetector", "反射获取SystemProperties失败", e);
            getPropMethod = null;
        }
    }
    @Override
    public void detect() {
        checkRootPackages();
        checkRootPath();
        checkRootFiles();
        checkSeLinux();
        checkUnLock();
        checkMountFile();
        checkMapsFile();
    }

    /**
     * 检查Root相关应用包名
     * 遍历预定义的包名列表，检查是否有已安装的Root应用
     */
    private void checkRootPackages() {
        try {
            PackageManager pm = context.getPackageManager();
            List<String> foundPackages = new ArrayList<>();

            for (String packageName : BuildConfig.ROOT_PACKAGES) {
                try {
                    pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                    foundPackages.add(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
            }

            if (!foundPackages.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String pkg : foundPackages) {
                    details.append(pkg).append("\n");
                }
                InfoItem warning = new WarningBuilder("checkRootApp", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "low")
                        .build();

                reportAbnormal(warning);
            }
        }catch (Exception e){{

        }}

    }
    /**
     * 检查Root相关文件
     * 遍历预定义的文件路径列表，检查是否存在Root相关文件
     */
    private void checkRootFiles() {
        try {
            List<String> foundFiles = new ArrayList<>();

            // 遍历检查每个文件路径
            for (String path : BuildConfig.ROOT_FILES) {
                File file = new File(path);
                if (file.exists()) {
                    foundFiles.add(path);
                }
            }

            if (!foundFiles.isEmpty()) {

                StringBuilder details = new StringBuilder();
                for (String path : foundFiles) {
                    details.append(path).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkRootFile", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
            }
        }catch (Exception e){

        }

    }
    /**
     * 检查PATH环境变量中是否包含su
     */
    private void checkRootPath() {
        try {
            String path = System.getenv("PATH");
            if (path != null) {
                String[] pathDirs = path.split(":");
                List<String> suPaths = new ArrayList<>();

                for (String dir : pathDirs) {
                    File suFile = new File(dir, "su");
                    if (suFile.exists()) {
                        suPaths.add(suFile.getAbsolutePath());
                    }
                }

                if (!suPaths.isEmpty()) {
                    StringBuilder details = new StringBuilder();
                    for (String suPath : suPaths) {
                        details.append(suPath).append("\n");
                    }

                    InfoItem warning = new WarningBuilder("checkRootPath", null)
                            .addDetail("check", details.toString().trim())
                            .addDetail("level", "medium")
                            .build();

                    reportAbnormal(warning);
                }
            }
        }catch (Exception e){

        }

    }
    /**
     * 检查seLinux安全上下文和状态
     */
    private void checkSeLinux() {
        try {
            List<String> abnormalDetails = new ArrayList<>();

            // 检查seLinux状态
            XCommandUtil.CommandResult seLinuxResult = XCommandUtil.execute("getenforce");
            if (seLinuxResult.isSuccess()) {
                String seLinuxStatus = seLinuxResult.getSuccessMsg();
                if (!seLinuxStatus.trim().equalsIgnoreCase("Enforcing")) {
                    abnormalDetails.add(seLinuxStatus.trim());
                }
            }

            // 检查seLinux上下文
            int pid = android.os.Process.myPid();
            String seLinuxPath = String.format("/proc/%d/attr/prev", pid);

            try {
                BufferedReader reader = new BufferedReader(new FileReader(seLinuxPath));
                String context = reader.readLine();
                reader.close();

                if (context != null && context.equals("u:r:zygote:s0")) {
                    abnormalDetails.add(context);
                }
            } catch (IOException e) {
            }

            if (!abnormalDetails.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String detail : abnormalDetails) {
                    details.append(detail).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkSeLinux", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
            }
        }catch (Exception e){

        }
        }

    /**
     * 使用反射的方法获取系统属性
     * @param prop
     * @return
     */
    private String getProperty(String prop) {
        // 先尝试使用反射方式
        if (getPropMethod != null) {
            try {
                Object value = getPropMethod.invoke(null, prop);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception e) {
                XLog.e(TAG, "反射获取属性失败: " + prop, e);
            }
        }

        // 反射失败则使用命令行方式
        XCommandUtil.CommandResult result = XCommandUtil.execute("getprop " + prop);
        if (result.isSuccess()) {
            return result.getSuccessMsg().trim();
        }
        return "";
    }

    /**
     * 检测设备是否解锁
     */
    private void checkUnLock() {
        try {
            List<String> abnormalProps = new ArrayList<>();

            for (String prop : BuildConfig.BOOTLOADER_PROPS) {
                String value = getProperty(prop).toLowerCase();
                if (value.contains("orange") || value.contains("unlocked")) {
                    abnormalProps.add(prop + ": " + value);
                }
            }
            String oemUnlockAllowed = getProperty("sys.oem_unlock_allowed");
            if ("1".equals(oemUnlockAllowed)) {
                abnormalProps.add("sys.oem_unlock_allowed: "+ oemUnlockAllowed);
            }

            if (!abnormalProps.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String prop : abnormalProps) {
                    details.append(prop).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkUnLock", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "medium")
                        .build();

                reportAbnormal(warning);
            }
        } catch (Exception e) {
            XLog.e(TAG, "checkUnLock失败", e);
        }
    }
    private void checkMountFile() {
        try {
            String mountContent = XFile.readFile("/proc/mounts");
            if (mountContent != null) {
                StringBuilder details = new StringBuilder();
                boolean found = false;

                for (String line : mountContent.split("\n")) {
                    String lowerLine = line.toLowerCase();
                    for (String keyword : BuildConfig.MOUNT_KEYWORDS) {
                        if (lowerLine.contains(keyword)) {
                            details.append("[").append(keyword).append("]: ")
                                    .append(line).append("\n");
                            found = true;
                        }
                    }
                }

                if (found) {
                    InfoItem warning = new WarningBuilder("checkMountFile", null)
                            .addDetail("check", details.toString().trim())
                            .addDetail("level", "medium")
                            .build();

                    reportAbnormal(warning);
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to check mount file", e);
        }
    }

    private void checkMapsFile() {
        try {
            String mapsContent = XFile.readFile("/proc/self/maps");
            if (mapsContent != null) {
                StringBuilder details = new StringBuilder();
                boolean found = false;

                for (String line : mapsContent.split("\n")) {
                    String lowerLine = line.toLowerCase();
                    for (String keyword : BuildConfig.MAPS_KEYWORDS) {
                        if (lowerLine.contains(keyword)) {
                            details.append("[").append(keyword).append("]: ")
                                    .append(line).append("\n");
                            found = true;
                        }
                    }
                }

                if (found) {
                    InfoItem warning = new WarningBuilder("checkMapsFile", null)
                            .addDetail("check", details.toString().trim())
                            .addDetail("level", "high")
                            .build();

                    reportAbnormal(warning);
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to check maps file", e);
        }
    }
}
