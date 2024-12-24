package com.xiaoc.warlock.Util;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class API {
    @SuppressLint({"PrivateApi"})
    public static void setHideShowWarning () {
        try {
            Class.forName("android.content.pm.PackageParser$Package").getDeclaredConstructor(String.class).setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread", new Class[0]);
            declaredMethod.setAccessible(true);
            Object invoke = declaredMethod.invoke(null, new Object[0]);
            Field declaredField = cls.getDeclaredField("mHiddenApiWarningShown");
            declaredField.setAccessible(true);
            declaredField.setBoolean(invoke, true);
            XLog.d("setHideShowWarning Sueecss");
        } catch (Exception e2) {
            e2.printStackTrace();
        }

    }
}
