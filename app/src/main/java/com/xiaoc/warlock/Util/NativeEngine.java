package com.xiaoc.warlock.Util;

import com.xiaoc.warlock.Core.CollectCallback;
import com.xiaoc.warlock.Core.DetectCallback;

import java.util.Map;

public class NativeEngine {

//    static {
//        // 加载本地库
//        System.loadLibrary("warlockCore");
//    }

    // 声明本地方法
    public static native String popen(String command);

    public native int open(String path, int flags);
    public static native void startCollect(CollectCallback callback);
    public static native String getCollectedInfo();
    public static native void startDetect(DetectCallback callback);
    public static native void stopDetect();
}
