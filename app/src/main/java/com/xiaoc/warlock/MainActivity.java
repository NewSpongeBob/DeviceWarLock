package com.xiaoc.warlock;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.xiaoc.warlock.Core.CollectCallback;
import com.xiaoc.warlock.Core.Warlock;
import com.xiaoc.warlock.Util.API;
import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.network.NetworkClient;
import com.xiaoc.warlock.ui.MainUI;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;


public class MainActivity extends AppCompatActivity implements CollectCallback {
    private static final String TAG = "MainActivity";
    private Context context;
    private boolean javaCollectComplete = false;
    private boolean nativeCollectComplete = false;
    private NetworkClient networkClient;
    
    // 公共静态变量，供Fragment访问
    public static boolean sBothCollectComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //CrashReport.initCrashReport(getApplicationContext(), "56552ffeab", false);
        initEnvironment();
        startCollect();
        XLog.i("boot->"+String.valueOf(getSystemBootTime()));
    }
    public static long getSystemBootTime() {
        try {
            String uptime = new BufferedReader(new FileReader("/proc/uptime"))
                    .readLine().split(" ")[0]; // 读取第一个值（单位：秒）
            return System.currentTimeMillis() - (long)(Double.parseDouble(uptime) * 1000);
        } catch (Exception e) {
            return 0;
        }
    }
    private void initEnvironment() {
        context = this;
        new MainUI(this);
        API.setHideShowWarning();
        XLog.init(this, XLog.DEBUG, true);
        
        // 初始化网络客户端
        networkClient = NetworkClient.getInstance(this);
    }

    private void startCollect() {
        // 启动Java层收集
        startJavaCollect();
        // 启动Native层收集
        NativeEngine.startCollect(this);
    }

    private void startJavaCollect() {
        new Thread(() -> {
            Warlock warlock = Warlock.getInstance(context);
            warlock.collectFingerprint();
            runOnUiThread(() -> {
                javaCollectComplete = true;
                checkAndProcessResult();
            });
        }).start();
    }

    @Override
    public void onNativeCollectComplete() {
        runOnUiThread(() -> {
            nativeCollectComplete = true;
            checkAndProcessResult();
            XLog.d(TAG, "Native collection completed");
        });
    }

    private void checkAndProcessResult() {
        XLog.d(TAG, "Check result - Java: " + javaCollectComplete + ", Native: " + nativeCollectComplete);
        if (!javaCollectComplete || !nativeCollectComplete) {
            return;
        }

        // 设置完成标志
        sBothCollectComplete = true;
        XLog.d(TAG, "Both Java and Native collection completed!");
    }
    
    /**
     * 检查指纹收集是否完成
     * @return 如果Java和Native层都完成，返回true；否则返回false
     */
    public static boolean isCollectionComplete() {
        return sBothCollectComplete;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void cleanup() {
        javaCollectComplete = false;
        nativeCollectComplete = false;
        // 不重置静态变量，因为其他组件可能依赖这些状态
    }
}