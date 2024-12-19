package com.xiaoc.warlock.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.xiaoc.warlock.IServerCallback;
import com.xiaoc.warlock.Util.XLog;


public class WarLockServer extends Service {
    private static final String TAG = "WarLockServer";

    static {
        System.loadLibrary("warlockServer");
    }

    private final RemoteCallbackList<IServerCallback> callbacks = new RemoteCallbackList<>();

    private final IServerCallback.Stub binder = new IServerCallback.Stub() {
        @Override
        public void onSandboxDetected(String details) {
            notifyDetection(details);
        }

        @Override
        public void ping() {
            XLog.d(TAG, "Service received ping request");
            // 即使是空实现，也添加日志
        }
    };


    private native void nativeCheckSandbox();

    @Override
    public IBinder onBind(Intent intent) {
        XLog.d(TAG, "Service onBind called");
        return binder;
    }

    // Native 回调方法
    private void onSandboxDetected(String details) {
        notifyDetection(details);
    }

    private void notifyDetection(String details) {
        // 通知所有注册的回调
        int n = callbacks.beginBroadcast();
        try {
            for (int i = 0; i < n; i++) {
                IServerCallback callback = callbacks.getBroadcastItem(i);
                try {
                    callback.onSandboxDetected(details);
                } catch (Exception e) {
                    XLog.e(TAG, "Failed to notify callback", e);
                }
            }
        } finally {
            callbacks.finishBroadcast();
        }
    }


    public void onCreate() {
        super.onCreate();


        nativeCheckSandbox();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        callbacks.kill();
    }
}