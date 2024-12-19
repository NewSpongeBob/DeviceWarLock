package com.xiaoc.warlock.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import com.xiaoc.warlock.IServerCallback;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.service.WarLockServer;

public class ServerManager {
    private static final String TAG = "ServerManager";
    private static final long HEARTBEAT_INTERVAL = 3000; // 心跳间隔3秒
    private static final int MAX_FAILED_PINGS = 3; // 连续失败3次才报警

    private static ServerManager instance;
    private Context context;
    private IServerCallback sandboxCallback;
    private SandboxCallback callback;
    private ServiceStateCallback stateCallback;
    private boolean isServiceBound = false;

    // 心跳相关字段
    private Handler heartbeatHandler;
    private boolean isHeartbeatRunning = false;
    private int failedPingCount = 0;

    public interface SandboxCallback {
        void onSandboxDetected(String details);
    }

    public interface ServiceStateCallback {
        void onServiceDied(String reason);
    }

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isServiceBound || sandboxCallback == null) {
                stopHeartbeat();
                return;
            }

            if (!checkServiceAlive()) {
                handleHeartbeatFailure();
            } else {
                failedPingCount = 0; // 重置失败计数
            }

            // 安排下一次心跳
            if (isHeartbeatRunning) {
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sandboxCallback = IServerCallback.Stub.asInterface(service);
            XLog.d(TAG, "Service connected, checking service alive");

            if (!checkServiceAlive()) {
                handleServiceFailure("Service not responding after binding");
                return;
            }

            isServiceBound = true;
            XLog.d(TAG, "Service verified and ready");
            startHeartbeat();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stopHeartbeat();
            sandboxCallback = null;
            isServiceBound = false;
            handleServiceFailure("Service disconnected unexpectedly");
        }
    };
    private void handleServiceFailure(String reason) {
        XLog.e(TAG, "Service failure: " + reason);

        // 通知服务状态回调
        if (stateCallback != null) {
            stateCallback.onServiceDied(reason);
        }

        // 通知沙箱检测回调
        if (callback != null) {
            callback.onSandboxDetected("Service failure detected: " + reason);
        }

        // 清理状态
        sandboxCallback = null;
        isServiceBound = false;

        try {
            context.unbindService(connection);
        } catch (Exception e) {
            XLog.e(TAG, "Error unbinding service", e);
        }
    }


    private ServerManager(Context context) {
        this.context = context.getApplicationContext();
        heartbeatHandler = new Handler(Looper.getMainLooper());
    }

    public static ServerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ServerManager.class) {
                if (instance == null) {
                    instance = new ServerManager(context);
                }
            }
        }
        return instance;
    }

    private void handleHeartbeatFailure() {
        failedPingCount++;
        XLog.e(TAG, "Heartbeat failed, count: " + failedPingCount);

        if (failedPingCount >= MAX_FAILED_PINGS) {
            XLog.e(TAG, "Heartbeat failed " + MAX_FAILED_PINGS + " times");
            handleServiceFailure("Service not responding to heartbeat");
            stopHeartbeat();
            rebindService();
        }
    }

    private void startHeartbeat() {
        if (!isHeartbeatRunning) {
            isHeartbeatRunning = true;
            failedPingCount = 0;
            XLog.d(TAG, "Starting heartbeat check");
            heartbeatHandler.post(heartbeatRunnable);
        }
    }

    private void stopHeartbeat() {
        if (isHeartbeatRunning) {
            XLog.d(TAG, "Stopping heartbeat check");
            isHeartbeatRunning = false;
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    private void rebindService() {
        try {
            context.unbindService(connection);
        } catch (Exception e) {
            XLog.e(TAG, "Error unbinding service during rebind", e);
        }

        sandboxCallback = null;
        isServiceBound = false;

        // 延迟一秒后重新绑定
        heartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bindService();
            }
        }, 1000);
    }

    public void setServiceStateCallback(ServiceStateCallback callback) {
        XLog.d(TAG, "Setting service state callback: " + (callback != null));
        this.stateCallback = callback;
    }

    public void init(SandboxCallback callback) {
        this.callback = callback;
        bindService();
    }

    public boolean isServiceBound() {
        return isServiceBound;
    }

    private boolean checkServiceAlive() {
        if (sandboxCallback == null) {
            XLog.e(TAG, "sandboxCallback is null");
            return false;
        }

        try {
            XLog.d(TAG, "Attempting to ping service");
            sandboxCallback.ping();
            XLog.d(TAG, "Service ping successful");
            return true;
        } catch (RemoteException e) {
            XLog.e(TAG, "Failed to ping service", e);
            return false;
        } catch (Exception e) {
            XLog.e(TAG, "Unexpected error during ping", e);
            return false;
        }
    }

    private void bindService() {
        Intent intent = new Intent(context, WarLockServer.class);
        boolean bindResult = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!bindResult) {
            handleServiceFailure("Failed to start isolated service");
            XLog.e(TAG, "Failed to bind to isolated service");
        }
    }
    public void destroy() {
        stopHeartbeat();
        try {
            context.unbindService(connection);
        } catch (Exception e) {
            XLog.e(TAG, "Error unbinding service", e);
        }
        callback = null;
        stateCallback = null;

        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacksAndMessages(null);
        }
    }
}