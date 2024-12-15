package com.xiaoc.warlock.Util;

import java.util.ArrayList;
import java.util.List;

public class AppChecker {
    private static boolean isReflectionSupported = false;

    private AppChecker() {}
    private static  List<String> nonMatchingStack = new ArrayList<>();
    private static boolean isStackTraceBbnormal = false;
    public static void checkReflectionSupport() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            activityThreadClass.getDeclaredMethod("currentActivityThread");
            isReflectionSupported = true;
        } catch (Exception e) {
            isReflectionSupported = false;
        }
    }

    public static boolean isReflectionSupported() {
        return isReflectionSupported;
    }
    public static void checkStackTrace(Exception e) {
        // 获取异常堆栈
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        // 遍历堆栈中的每一行
        for (StackTraceElement element : stackTraceElements) {
            String className = element.getClassName();
            XLog.e(className);
            if (!className.contains("android.app") &&
                    !className.contains("android.os") &&
                    !className.contains("android.internal")&&
                    !className.contains("com.xiaoc.warlock")&&
                    !className.contains("java.lang.reflect")) {
                isStackTraceBbnormal = true;
                nonMatchingStack.add(element.toString());
            }
        }
    }
    public static boolean isStackTraceBbnormal() {
        return isStackTraceBbnormal;
    }
    public static List<String> getStackTraceBbnormal(){
        return nonMatchingStack;
    }
}
