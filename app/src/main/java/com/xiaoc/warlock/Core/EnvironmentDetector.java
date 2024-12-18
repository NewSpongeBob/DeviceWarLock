package com.xiaoc.warlock.Core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.xiaoc.warlock.Core.detector.MiscDetector;
import com.xiaoc.warlock.Core.detector.RootDetector;
import com.xiaoc.warlock.Core.detector.SandboxDetector;
import com.xiaoc.warlock.Core.detector.VirtualDetector;
import com.xiaoc.warlock.Core.detector.XposedDetector;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnvironmentDetector  implements BaseDetector.EnvironmentCallback{
    private static EnvironmentDetector instance;
    private final Context context;
    private final Handler handler;
    private final List<EnvironmentCallback> callbacks = new ArrayList<>();
    private boolean isDetecting = false;
    private final List<BaseDetector> detectors;

    @Override
    public void onAbnormalDetected(InfoItem item) {
        notifyEnvironmentChange(item);
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
                new SandboxDetector(context,this)

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
        }
    }

    public void unregisterCallback(EnvironmentCallback callback) {
        callbacks.remove(callback);
    }

    public void startDetection() {
        if (isDetecting) return;
        isDetecting = true;
        startDetectionLoop();
    }

    public void stopDetection() {
        handler.removeCallbacksAndMessages(null);
    }

    private void startDetectionLoop() {
        // 执行所有检测器
        for (BaseDetector detector : detectors) {
            detector.detect();
        }
    }

    private void notifyEnvironmentChange(InfoItem item) {
        handler.post(() -> {
            for (EnvironmentCallback callback : callbacks) {
                callback.onEnvironmentChanged(item);
            }
        });
    }
}