package com.xiaoc.warlock.Util;

import android.os.Build;

import java.lang.reflect.Method;

public class MiscUtil {
    public static String getSystemProperty(String prop) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 使用反射获取 SystemProperties
                Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                Method getMethod = systemProperties.getMethod("get", String.class);
                Object value = getMethod.invoke(null, prop);
                return value != null ? value.toString() : "";
            } else {
                // Android P 以下版本直接使用 SystemProperties
                Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                Method getMethod = systemProperties.getMethod("get", String.class);
                Object value = getMethod.invoke(null, prop);
                return value != null ? value.toString() : "";
            }
        } catch (Exception e) {
            XLog.e("MiscUtil", "Failed to get property " + prop + ": " + e.getMessage());
            return "";
        }
    }
}
