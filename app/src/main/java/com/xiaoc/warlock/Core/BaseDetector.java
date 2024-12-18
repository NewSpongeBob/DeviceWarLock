package com.xiaoc.warlock.Core;

import android.content.Context;
import com.xiaoc.warlock.ui.adapter.InfoItem;

public abstract class BaseDetector {
    protected Context context;
    protected EnvironmentCallback callback;

    public interface EnvironmentCallback {
        void onAbnormalDetected(InfoItem item);
    }

    public BaseDetector(Context context, EnvironmentCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public abstract void detect();

    protected void reportAbnormal(InfoItem item) {
        if (callback != null) {
            callback.onAbnormalDetected(item);
        }
    }
    // 添加 release 方法
    public void release() {
        // 基类的清理工作
        context = null;
        callback = null;
    }
}