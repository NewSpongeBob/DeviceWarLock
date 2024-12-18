package com.xiaoc.warlock.Core.detector;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.manager.ServerManager;
import com.xiaoc.warlock.ui.adapter.InfoItem;

public class SandboxDetector extends BaseDetector {
    private static final String TAG = "SandboxDetector";
    private ServerManager serverManager;
    private boolean isServiceDied = false;
    private Handler handler;
    private static final int INIT_CHECK_DELAY = 1000; // 1秒
    public SandboxDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
        serverManager = ServerManager.getInstance(context);
        handler = new Handler(Looper.getMainLooper());
        initCallbacks();
        checkInitialServiceState();
    }
    private void checkInitialServiceState() {
        // 延迟一秒检查服务是否成功启动
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!serverManager.isServiceBound()) {
                    handleServiceDied("Isolated service failed to start");
                }
                // 检查完成后释放 handler
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }
        }, INIT_CHECK_DELAY);
    }

    private void initCallbacks() {
        // 先设置状态回调
        serverManager.setServiceStateCallback(new ServerManager.ServiceStateCallback() {
            @Override
            public void onServiceDied(String reason) {
                XLog.e(TAG, "Service died callback received: " + reason);  // 添加日志
                handleServiceDied("Service died: " + reason);
            }
        });

        // 再初始化服务
        serverManager.init(new ServerManager.SandboxCallback() {
            @Override
            public void onSandboxDetected(String details) {
                handleSandboxDetection(details);
            }
        });
    }


    private void handleServiceDied(String reason) {
        XLog.e(TAG, "Handling service died: " + reason);  // 添加日志
        if (!isServiceDied) {
            isServiceDied = true;
            InfoItem warning = new WarningBuilder("serviceStatus", null)
                    .addDetail("check", "Isolated WarLock Server: " + reason)
                    .addDetail("level", "high")
                    .build();

            XLog.e(TAG, "Created warning item: " + warning.toString());  // 添加日志
            reportAbnormal(warning);
        }
    }


    private void handleSandboxDetection(String details) {
        InfoItem warning = new WarningBuilder("checkSandbox", null)
                .addDetail("check", details)
                .addDetail("level", "high")
                .build();

        reportAbnormal(warning);
    }
    @Override
    protected void reportAbnormal(InfoItem item) {
        XLog.e(TAG, "Reporting abnormal item: " + item.toString());
        if (callback != null) {
            callback.onAbnormalDetected(item);
            XLog.e(TAG, "Abnormal reported to callback");
        } else {
            XLog.e(TAG, "Warning: callback is null in reportAbnormal!");
        }
    }
    @Override
    public void detect() {
        XLog.d(TAG, "Starting sandbox detection...");
    }

    @Override
    public void release() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (serverManager != null) {
            serverManager.destroy();
            serverManager = null;
        }
        super.release();
    }
}