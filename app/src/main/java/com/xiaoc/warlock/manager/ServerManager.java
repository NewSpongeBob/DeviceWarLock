package com.xiaoc.warlock.manager;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.xiaoc.warlock.IServerCallback;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.service.WarLockServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ServerManager {
    private static final String TAG = "ServerManager";
    private static ServerManager instance;
    private Context context;
    private IServerCallback sandboxCallback;
    private SandboxCallback callback;
    private ServiceStateCallback stateCallback;
    private boolean isServiceBound = false;

    public interface SandboxCallback {
        void onSandboxDetected(String details);
    }
    // 添加服务状态回调接口
    public interface ServiceStateCallback {
        void onServiceDied(String reason);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sandboxCallback = IServerCallback.Stub.asInterface(service);
            XLog.d(TAG, "Service connected, checking service alive");

            // 通过 AIDL 调用检查服务是否真的可用
            if (!checkServiceAlive()) {
                XLog.e(TAG, "Service not responding after binding");
                if (stateCallback != null) {
                    stateCallback.onServiceDied("Service not responding after binding");
                }
                return;
            }

            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sandboxCallback = null;
            isServiceBound = false;

            if (stateCallback != null) {
                stateCallback.onServiceDied("Service disconnected unexpectedly");
            }
        }
    };
    // 设置状态回调
    public void setServiceStateCallback(ServiceStateCallback callback) {
        XLog.d(TAG, "Setting service state callback: " + (callback != null));  // 添加日志
        this.stateCallback = callback;
    }

    private ServerManager(Context context) {
        this.context = context.getApplicationContext();
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

    public void init(SandboxCallback callback) {
        this.callback = callback;
        bindService();
    }

    // 添加公开方法来检查服务绑定状态
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
        }
    }
    private void bindService() {
        Intent intent = new Intent(context, WarLockServer.class);
        boolean bindResult = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!bindResult) {
            // 绑定失败，说明服务无法启动
            if (stateCallback != null) {
                stateCallback.onServiceDied("Failed to start isolated service");
            }
            XLog.e(TAG, "Failed to bind to isolated service");
        }
    }

    public void destroy() {
        try {
            context.unbindService(connection);
        } catch (Exception e) {
            XLog.e(TAG, "Error unbinding service", e);
        }
        callback = null;
    }
}
