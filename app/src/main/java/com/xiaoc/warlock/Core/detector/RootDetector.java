package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.content.pm.PackageManager;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.XCommandUtil;
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
    public RootDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    @Override
    public void detect() {
        checkRootPackages();
        checkRootPath();
        checkRootFiles();
        checkSeLinux();
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

}
