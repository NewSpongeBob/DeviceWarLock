package com.xiaoc.warlock.Util;

public class NativeEngine {

    static {
        // 加载本地库
        System.loadLibrary("warlockCore");
    }

    // 声明本地方法
    public static native String popen(String command);

    public native int open(String path, int flags);

    // 其他本地方法声明...
}
