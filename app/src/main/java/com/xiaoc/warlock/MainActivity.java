package com.xiaoc.warlock;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.xiaoc.warlock.Core.CollectCallback;
import com.xiaoc.warlock.Core.Warlock;
import com.xiaoc.warlock.Util.API;
import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;
import com.xiaoc.warlock.ui.MainUI;
import org.json.JSONObject;
import com.xiaoc.warlock.crypto.EncryptUtil;

import java.io.BufferedReader;
import java.io.FileReader;


public class MainActivity extends AppCompatActivity implements CollectCallback {
    private static final String TAG = "MainActivity";
    private Context context;
    private MainUI mainUI;
    private boolean javaCollectComplete = false;
    private boolean nativeCollectComplete = false;
    
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
        mainUI = new MainUI(this);
        API.setHideShowWarning();
        XLog.init(this, XLog.DEBUG, true);
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
                checkAndPrintResult();
            });
        }).start();
    }

    @Override
    public void onNativeCollectComplete() {
        runOnUiThread(() -> {
            nativeCollectComplete = true;
            checkAndPrintResult();
            XLog.d(TAG, "Native collection completed");
        });
    }

    private void checkAndPrintResult() {
        XLog.d(TAG, "Check result - Java: " + javaCollectComplete + ", Native: " + nativeCollectComplete);
        if (!javaCollectComplete || !nativeCollectComplete) {
            return;
        }

        // 设置完成标志
        sBothCollectComplete = true;

        try {
            // 保存单独的结果
            saveJavaResults();
            saveNativeResults();

            // 如果需要，还可以保存合并的结果
            saveCombinedResults();
        } catch (Exception e) {
            XLog.e(TAG, "Error processing results: " + e.getMessage());
        }
    }
    
    /**
     * 检查指纹收集是否完成
     * @return 如果Java和Native层都完成，返回true；否则返回false
     */
    public static boolean isCollectionComplete() {
        return sBothCollectComplete;
    }
    
    private void saveCombinedResults() {
        try {
            JSONObject combined = new JSONObject();
            combined.put("java_fingerprints", new JSONObject(Xson.getMapString(true)));
            combined.put("native_fingerprints", new JSONObject(NativeEngine.getCollectedInfo()));

            String combinedJson = combined.toString(4);  // 使用4空格缩进
            //XLog.d(TAG, "Combined fingerprint result: " + combinedJson);
            saveToFile("combined_fingerprint.txt", combinedJson);
        } catch (Exception e) {
            XLog.e(TAG, "Error combining results: " + e.getMessage());
        }
    }
    private void saveJavaResults() {
        String javaJson = Xson.getMapString(true);
       // XLog.d(TAG, "Java fingerprint result: " + javaJson);
        EncryptUtil encryptUtil = new EncryptUtil(javaJson);
        saveToFile("java_fingerprint.txt", javaJson);
    }

    private void saveNativeResults() {
        String nativeJson = NativeEngine.getCollectedInfo();
        if (nativeJson == null || nativeJson.isEmpty()) {
            XLog.w(TAG, "No native info collected");
            return;
        }
        XLog.d(TAG, "Native fingerprint result: " + nativeJson);
        saveToFile("native_fingerprint.txt", nativeJson);
    }

    private void saveToFile(String filename, String content) {
        if (XFile.writeExternalFile(this, filename, content, true)) {
            XLog.d(TAG, "Results saved to external storage: " + filename);
        } else if (XFile.writePrivateFile(this, filename, content, true)) {
            XLog.d(TAG, "Results saved to private storage: " + filename);
        } else {
            XLog.e(TAG, "Failed to save results: " + filename);
        }
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