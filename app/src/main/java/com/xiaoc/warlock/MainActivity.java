package com.xiaoc.warlock;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.xiaoc.warlock.Core.CollectCallback;
import com.xiaoc.warlock.Core.Warlock;

import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;
import com.xiaoc.warlock.ui.MainUI;

import org.json.JSONObject;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements CollectCallback {
    private static final String TAG = "MainActivity";
    private Context context;
    private MainUI mainUI;
    private boolean javaCollectComplete = false;
    private boolean nativeCollectComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initEnvironment();
        startCollect();
    }

    private void initEnvironment() {
        context = this;
        mainUI = new MainUI(this);


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
    private void saveCombinedResults() {
        try {
            JSONObject combined = new JSONObject();
            combined.put("java_fingerprints", new JSONObject(Xson.getMapString(true)));
            combined.put("native_fingerprints", new JSONObject(NativeEngine.getCollectedInfo()));

            String combinedJson = combined.toString(4);  // 使用4空格缩进
            XLog.d(TAG, "Combined fingerprint result: " + combinedJson);
            saveToFile("combined_fingerprint.txt", combinedJson);
        } catch (Exception e) {
            XLog.e(TAG, "Error combining results: " + e.getMessage());
        }
    }
    private void saveJavaResults() {
        String javaJson = Xson.getMapString(true);
        XLog.d(TAG, "Java fingerprint result: " + javaJson);
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
        // 可以添加其他清理工作
    }
}