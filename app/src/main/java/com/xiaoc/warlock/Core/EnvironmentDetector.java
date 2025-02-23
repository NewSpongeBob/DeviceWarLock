package com.xiaoc.warlock.Core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.xiaoc.warlock.Core.detector.CloudPhoneDetector;
import com.xiaoc.warlock.Core.detector.HookDetector;
import com.xiaoc.warlock.Core.detector.MiscDetector;
import com.xiaoc.warlock.Core.detector.RootDetector;
import com.xiaoc.warlock.Core.detector.SandboxDetector;
import com.xiaoc.warlock.Core.detector.SignatureDetector;
import com.xiaoc.warlock.Core.detector.VirtualDetector;
import com.xiaoc.warlock.Core.detector.XposedDetector;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnvironmentDetector  implements BaseDetector.EnvironmentCallback{
    private String TAG = "EnvironmentDetector";
    private static EnvironmentDetector instance;
    private final Context context;
    private final Handler handler;
    private final List<EnvironmentCallback> callbacks = new ArrayList<>();
    private boolean isDetecting = false;
    private final List<BaseDetector> detectors;
    private final List<InfoItem> pendingItems = new ArrayList<>(); // 添加待处理项列表

    @Override
    public void onAbnormalDetected(InfoItem item) {
        XLog.d(TAG, "Received abnormal detection: " + item.getTitle());
        if (callbacks.isEmpty()) {
            XLog.d(TAG, "No callbacks registered, queuing item");
            pendingItems.add(item);
        } else {
            notifyEnvironmentChange(item);
        }
    }

    public interface EnvironmentCallback {
        void onEnvironmentChanged(InfoItem newItem);
    }

    private EnvironmentDetector(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.detectors = initDetectors();
    }

    private List<BaseDetector> initDetectors() {
        return Arrays.asList(
                new RootDetector(context, this),
                new VirtualDetector(context,this),
                new MiscDetector(context,this),
                new XposedDetector(context,this),
                new SandboxDetector(context,this),
                new CloudPhoneDetector(context,this),
                new SignatureDetector(context,this),
                new HookDetector(context,this)
                // 添加更多检测器...
        );
    }

    public static synchronized EnvironmentDetector getInstance(Context context) {
        if (instance == null) {
            instance = new EnvironmentDetector(context);
        }
        return instance;
    }

    public void registerCallback(EnvironmentCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
            XLog.d(TAG, "Registered callback: " + callback.getClass().getSimpleName());

            // 发送所有待处理的警告
            if (!pendingItems.isEmpty()) {
                XLog.d(TAG, "Processing " + pendingItems.size() + " pending items");
                for (InfoItem item : pendingItems) {
                    notifyEnvironmentChange(item);
                }
                pendingItems.clear();
            }
        }
    }

    public void unregisterCallback(EnvironmentCallback callback) {
        callbacks.remove(callback);
    }

    public void startDetection() {
        if (isDetecting) return;
        isDetecting = true;
        for (BaseDetector detector : detectors) {
            detector.detect();
        }
    }

    public void stopDetection() {
        handler.removeCallbacksAndMessages(null);
    }


    private void notifyEnvironmentChange(InfoItem item) {
        handler.post(() -> {
            XLog.d(TAG, "Notifying " + callbacks.size() + " callbacks about environment change");  // 添加日志
            for (EnvironmentCallback callback : callbacks) {
                if (callback != null) {  // 添加空检查
                    callback.onEnvironmentChanged(item);
                    XLog.d(TAG, "Notified callback: " + callback.getClass().getSimpleName());  // 添加日志
                }
            }
        });
    }
}