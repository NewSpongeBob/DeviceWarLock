package com.xiaoc.warlock.Core;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;
import com.xiaoc.warlock.crypto.MD5Util;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FingerprintCollector {
    private final Context context;
    private final List<FingerprintCallback> callbacks = new ArrayList<>();
    private static FingerprintCollector instance;
    private static final String TAG = "FingerprintCollector";
    private String eventId = null;
    
    // 需要进行MD5加密的键列表
    private static final Set<String> MD5_KEYS = new HashSet<>(Arrays.asList(
            "a3","a5","a7","a14", "a21", "a52","a53","a58","a64","a69","a70","a6" ,"n1","n3","n8","n9","n10","n11","n12","n15","n17","n20","n21"
    ));

    public interface FingerprintCallback {
        void onFingerprintCollected(InfoItem item);
    }

    private FingerprintCollector(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized FingerprintCollector getInstance(Context context) {
        if (instance == null) {
            instance = new FingerprintCollector(context);
        }
        return instance;
    }

    public void registerCallback(FingerprintCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void unregisterCallback(FingerprintCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * 设置事件ID并显示
     * @param eventId 要显示的事件ID
     * @param isSuccess 是否获取/上报成功
     * @param errorMsg 错误信息，如果isSuccess为false则使用此信息
     */
    public void setAndDisplayEventId(String eventId, boolean isSuccess, String errorMsg) {
        this.eventId = eventId;
        
        // 创建事件ID展示项
        InfoItem eventIdItem = new InfoItem("Event ID", "事件ID信息");
        if (isSuccess) {
            eventIdItem.addDetail("state", "success");
            eventIdItem.addDetail("event_id", eventId);
        } else {
            eventIdItem.addDetail("state", "error");
            eventIdItem.addDetail("state", "failed");
        }
        
        // 通知回调
        notifyCallbacks(eventIdItem);
    }

    public void collectFingerprint() {
        // 在后台线程中从已收集的数据中获取信息并展示
        new Thread(() -> {
            try {
                // 获取Java层数据
                String javaJson = Xson.getMapString(true);
                if (javaJson != null && !javaJson.isEmpty()) {
                    parseAndDisplayJavaFingerprint(javaJson);
                }

                // 获取Native层数据
                String nativeJson = NativeEngine.getCollectedInfo();
                if (nativeJson != null && !nativeJson.isEmpty()) {
                    parseAndDisplayNativeFingerprint(nativeJson);
                }
            } catch (Exception e) {
                XLog.e(TAG, "Error collecting fingerprints: " + e.getMessage());
            }
        }).start();
    }

    private void notifyCallbacks(InfoItem item) {
        for (FingerprintCallback callback : callbacks) {
            if (callback != null) {
                callback.onFingerprintCollected(item);
            }
        }
    }
    
    /**
     * 解析并展示Java层收集的指纹信息
     */
    private void parseAndDisplayJavaFingerprint(String javaFingerprint) {
        try {
            JSONObject jsonObject = new JSONObject(javaFingerprint);

            // 基本设备信息
            InfoItem basicInfo = new InfoItem("Base Info", "基本设备信息");
            addJsonDataToInfoItem(jsonObject, basicInfo, new String[]{"a8",Build.MODEL,Build.CPU_ABI,Build.VERSION.RELEASE,"a11","a67"},
                    new String[]{"Vendor","Model","CPU_ABI","AndroidVersion","FingerPrint","KernelInfo"});
            notifyCallbacks(basicInfo);
            //Java层设备信息
            InfoItem javaInfo = new InfoItem("Java Fingerprint", "java指纹信息");
            addJsonDataToInfoItem(jsonObject, javaInfo, new String[]{"a3","a4","a5","a6", "a7","a14", "a21","a52","a53","a58","a69" ,"a70","a80","a81"},
                    new String[]{"a3","a4","a5","a6", "a7","a14", "a21","a52","a53","a58","a69" ,"a70","a80","a81"});
            notifyCallbacks(javaInfo);

        } catch (JSONException e) {
            XLog.e(TAG, "Error parsing Java fingerprint: " + e.getMessage());
        }
    }

    /**
     * 解析并展示Native层收集的指纹信息
     */
    private void parseAndDisplayNativeFingerprint(String nativeFingerprint) {
        try {
            JSONObject jsonObject = new JSONObject(nativeFingerprint);

            //Native层设备信息
            InfoItem nativeInfo = new InfoItem("Native Fingerprint", "native指纹信息");
            addJsonDataToInfoItem(jsonObject, nativeInfo, new String[]{"n1","n3","n9","n10","n11","n12","n15"},
                    new String[]{"n1","n3","n9","n10","n11","n12","n15"});
            notifyCallbacks(nativeInfo);
            
        } catch (JSONException e) {
            XLog.e(TAG, "Error parsing Native fingerprint: " + e.getMessage());
        }
    }
    
    /**
     * 辅助方法：从JSON对象中提取数据并添加到InfoItem中
     * 支持处理普通字符串键和以a或n开头后跟1-2位数字的键
     * 对于指定的键，会对值进行MD5加密后再展示
     */
    private void addJsonDataToInfoItem(JSONObject jsonObject, InfoItem infoItem, 
                                     String[] keys, String[] displayNames) {

        for (int i = 0; i < keys.length; i++) {
            try {
                String key = keys[i];
                // 检查是否是以a或n开头后跟1-2位数字的键
                if ((key.startsWith("a") || key.startsWith("n")) && 
                    key.length() >= 2 && key.length() <= 3 && 
                    Character.isDigit(key.charAt(1)) && 
                    (key.length() == 2 || Character.isDigit(key.charAt(2)))) {
                    
                    // 处理a或n开头后跟1-2位数字的键
                    if (jsonObject.has(key)) {
                        JSONObject data = jsonObject.getJSONObject(key);
                        if (data.has("v")) {
                            Object value = data.get("v");
                            String valueStr = value.toString();
                            
                            // 检查是否需要进行MD5加密
                            if (MD5_KEYS.contains(key)) {
                                // 对数据进行MD5加密
                                valueStr = MD5Util.md5(valueStr);
                                XLog.d(TAG, "Applied MD5 encryption to key: " + key);
                            }
                            
                            infoItem.addDetail(displayNames[i], valueStr);
                        }
                    }
                } else {
                    // 对于常规键，直接添加
                    infoItem.addDetail(displayNames[i], key);
                }
            } catch (JSONException e) {
                XLog.e(TAG, "Error getting " + keys[i] + ": " + e.getMessage());
            }
        }
    }
} 