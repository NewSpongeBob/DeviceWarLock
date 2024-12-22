package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class SignatureDetector extends BaseDetector {
    private static final String TAG = "SignatureDetector";

    public SignatureDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    @Override
    public void detect() {
        checkPackageInfoCreator();
    }

    /**
     * 检测PackageInfo.CREATOR的ClassLoader是否被替换
     */
    private void checkPackageInfoCreator() {
        try {
            // 获取PackageInfo.CREATOR字段
            Field creatorField = PackageInfo.class.getDeclaredField("CREATOR");
            creatorField.setAccessible(true);
            Object creator = creatorField.get(null);

            if (creator != null) {
                // 获取CREATOR的ClassLoader
                ClassLoader creatorClassLoader = creator.getClass().getClassLoader();
                // 获取系统ClassLoader
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

                if (creatorClassLoader == null || systemClassLoader == null) {
                    XLog.w(TAG, "ClassLoader is null");
                    return;
                }

                String creatorLoaderName = creatorClassLoader.getClass().getName();
                String systemLoaderName = systemClassLoader.getClass().getName();

                // 检查ClassLoader类型
                if (systemLoaderName.equals(creatorLoaderName)) {
                    // 系统的应该是BootClassLoader，而用户的是PathClassLoader
                    // 如果相等说明被替换了
                    Map<String, String> details = new LinkedHashMap<>();
                    details.put("creatorLoader", creatorLoaderName);
                    details.put("systemLoader", systemLoaderName);
                    details.put("creator", creator.getClass().getName());

                    InfoItem warning = new WarningBuilder("checkPackageInfoCreator", null)
                            .addDetail("check", details.toString())
                            .addDetail("level", "high")
                            .build();

                    reportAbnormal(warning);
                } else {

                }

                // 额外检查CREATOR的实现类
                checkCreatorImplementation(creator);
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to check PackageInfo.CREATOR", e);
        }
    }

    /**
     * 检查CREATOR的具体实现
     */
    private void checkCreatorImplementation(Object creator) {
        try {
            // 检查CREATOR是否是预期的Parcelable.Creator实现
            if (!creator.getClass().getName().contains("android.content")) {
                Map<String, String> details = new LinkedHashMap<>();
                details.put("creatorClass", creator.getClass().getName());
                details.put("expectedPackage", "android.content");

                InfoItem warning = new WarningBuilder("checkCreatorImplementation", null)
                        .addDetail("check", details.toString())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }

            // 检查CREATOR的方法实现
            Method[] methods = creator.getClass().getDeclaredMethods();
            boolean hasCreateFromParcel = false;
            boolean hasNewArray = false;

            for (Method method : methods) {
                if (method.getName().equals("createFromParcel")) {
                    hasCreateFromParcel = true;
                } else if (method.getName().equals("newArray")) {
                    hasNewArray = true;
                }
            }

            if (!hasCreateFromParcel || !hasNewArray) {
                InfoItem warning = new WarningBuilder("checkCreatorMethods", null)
                        .addDetail("check", "CREATOR方法实现异常")
                        .addDetail("level", "high")
                        .addDetail("description", "PackageInfo.CREATOR缺少必要的方法实现")
                        .addDetail("hasCreateFromParcel", String.valueOf(hasCreateFromParcel))
                        .addDetail("hasNewArray", String.valueOf(hasNewArray))
                        .build();

                reportAbnormal(warning);
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to check CREATOR implementation", e);
        }
    }
}
