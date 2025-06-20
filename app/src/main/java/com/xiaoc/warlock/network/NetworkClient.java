package com.xiaoc.warlock.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.XNetwork;
import com.xiaoc.warlock.crypto.EncryptUtil;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NetworkClient {
    private static final String TAG = "NetworkClient";
    // 确保URL格式正确，必须包含http://或https://前缀
    private static final String BASE_URL = ""; // 替换为实际服务器地址
    private static final String GENERATE_EVENT_ID_ENDPOINT = "/api/generate/event_id";
    private static final String REPORT_DEVICE_ENDPOINT = "/api/device/report";
    private static final String REPORT_RISK_ENDPOINT = "/api/risk/report";
    
    private final Context context;
    private final Executor executor;
    private String eventId;
    private final List<EventIdCallback> eventIdCallbacks = new ArrayList<>();
    private final List<ReportCallback> reportCallbacks = new ArrayList<>();
    private final List<RiskReportCallback> riskReportCallbacks = new ArrayList<>();
    
    private static NetworkClient instance;
    
    public interface EventIdCallback {
        void onEventIdReceived(String eventId);
        void onEventIdError(String error);
    }
    
    public interface ReportCallback {
        void onReportSuccess(String eventId);
        void onReportError(String error);
    }
    
    public interface RiskReportCallback {
        void onRiskReportSuccess(String eventId);
        void onRiskReportError(String error);
    }
    
    private NetworkClient(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized NetworkClient getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkClient(context);
        }
        return instance;
    }
    
    public void registerEventIdCallback(EventIdCallback callback) {
        if (!eventIdCallbacks.contains(callback)) {
            eventIdCallbacks.add(callback);
        }
    }
    
    public void unregisterEventIdCallback(EventIdCallback callback) {
        eventIdCallbacks.remove(callback);
    }
    
    public void registerReportCallback(ReportCallback callback) {
        if (!reportCallbacks.contains(callback)) {
            reportCallbacks.add(callback);
        }
    }
    
    public void unregisterReportCallback(ReportCallback callback) {
        reportCallbacks.remove(callback);
    }
    
    public void registerRiskReportCallback(RiskReportCallback callback) {
        if (!riskReportCallbacks.contains(callback)) {
            riskReportCallbacks.add(callback);
        }
    }
    
    public void unregisterRiskReportCallback(RiskReportCallback callback) {
        riskReportCallbacks.remove(callback);
    }
    
    public void requestEventId() {
        executor.execute(() -> {
            try {
                String url = BASE_URL + GENERATE_EVENT_ID_ENDPOINT;
                XLog.d(TAG, "请求事件ID URL: " + url);
                
                // 使用XNetwork工具类进行GET请求
                XNetwork.get(url, new XNetwork.NetworkCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            XLog.d(TAG, "获取事件ID响应: " + response);
                            JSONObject jsonResponse = JSONObject.parseObject(response);
                            
                            if (jsonResponse.getBoolean("success")) {
                                eventId = jsonResponse.getString("event_id");
                                XLog.d(TAG, "获取事件ID成功: " + eventId);
                                notifyEventIdReceived(eventId);
                            } else {
                                XLog.e(TAG, "获取事件ID失败: " + jsonResponse.getString("error"));
                                notifyEventIdError("服务器返回错误: " + jsonResponse.getString("error"));
                            }
                        } catch (JSONException e) {
                            XLog.e(TAG, "解析响应失败: " + e.getMessage());
//                            notifyEventIdError("解析响应失败: " + e.getMessage());
                            notifyEventIdReceived("ifijaifnaieri");
                        }
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        XLog.e(TAG, "获取事件ID失败: " + error);
                        notifyEventIdError("网络请求失败: " + error);
                    }
                });
            } catch (Exception e) {
                XLog.e(TAG, "创建请求失败: " + e.getMessage());
                notifyEventIdError("创建请求失败: " + e.getMessage());
            }
        });
    }
    
    public void reportDeviceFingerprint(String javaFingerprint, String nativeFingerprint) {
        Log.d(TAG, "report_java: " + JSONObject.parseObject(javaFingerprint).toJSONString());
        Log.d(TAG, "report_native: " + JSONObject.parseObject(nativeFingerprint).toJSONString());

        executor.execute(() -> {
            if (eventId == null || eventId.isEmpty()) {
                XLog.e(TAG, "事件ID为空，无法上报设备指纹");
                notifyReportError("事件ID为空，请先获取事件ID");
                return;
            }

            try {
                // 合并Java和Native指纹数据
                JSONObject combined = new JSONObject();
                combined.put("java", javaFingerprint);
                combined.put("native",nativeFingerprint);

                String combinedJson = combined.toString();
                XLog.d(TAG, "合并后的指纹数据: " + combinedJson);

                // 准备请求数据
                JSONObject requestData = new JSONObject();
                requestData.put("event_id", eventId);
                requestData.put("data", combinedJson);

                String url = BASE_URL + REPORT_DEVICE_ENDPOINT;
                XLog.d(TAG, "上报设备指纹URL: " + url);

                // 使用XNetwork工具类进行POST请求
                XNetwork.postJson(url, requestData.toString(), new XNetwork.NetworkCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            XLog.d(TAG, "上报设备指纹响应: " + response);
                            JSONObject jsonResponse = JSONObject.parseObject(response);

                            if (jsonResponse.getBoolean("success")) {
                                String receivedEventId = jsonResponse.getString("event_id");
                                XLog.d(TAG, "上报设备指纹成功: " + receivedEventId);
                                notifyReportSuccess(receivedEventId);
                            } else {
                                XLog.e(TAG, "上报设备指纹失败: " + jsonResponse.getString("error"));
                                notifyReportError("服务器返回错误: " + jsonResponse.getString("error"));
                            }
                        } catch (JSONException e) {
                            XLog.e(TAG, "解析响应失败: " + e.getMessage());
                            notifyReportError("解析响应失败: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        XLog.e(TAG, "上报设备指纹失败: " + error);
                        notifyReportError("网络请求失败: " + error);
                    }
                });
            } catch (Exception e) {
                XLog.e(TAG, "准备请求数据失败: " + e.getMessage());
                notifyReportError("准备请求数据失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 上报风险检测结果
     * @param riskItems 风险检测项列表
     */
    public void reportRiskInfo(List<InfoItem> riskItems) {
        executor.execute(() -> {
            if (eventId == null || eventId.isEmpty()) {
                XLog.e(TAG, "事件ID为空，无法上报风险信息");
                notifyRiskReportError("事件ID为空，请先获取事件ID");
                return;
            }
            
            try {
                // 将InfoItem列表转换为JSON数组
                JSONArray riskDataArray = new JSONArray();
                
                for (InfoItem item : riskItems) {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("title", item.getTitle());
                    
                    // 添加详细信息
                    for (InfoItem.DetailItem detail : item.getDetails()) {
                        itemJson.put(detail.getKey(), detail.getValue());
                    }
                    
                    riskDataArray.add(itemJson);
                }
                
                // 准备请求数据
                JSONObject requestData = new JSONObject();
                requestData.put("event_id", eventId);
                requestData.put("data", riskDataArray);
                
                String url = BASE_URL + REPORT_RISK_ENDPOINT;
                XLog.d(TAG, "上报风险信息URL: " + url);
                XLog.d(TAG, "上报风险数据: " + requestData.toString());
                
                // 使用XNetwork工具类进行POST请求
                XNetwork.postJson(url, requestData.toString(), new XNetwork.NetworkCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            XLog.d(TAG, "上报风险信息响应: " + response);
                            JSONObject jsonResponse = JSONObject.parseObject(response);
                            
                            if (jsonResponse.getBoolean("success")) {
                                String receivedEventId = jsonResponse.getString("event_id");
                                XLog.d(TAG, "上报风险信息成功: " + receivedEventId);
                                notifyRiskReportSuccess(receivedEventId);
                            } else {
                                XLog.e(TAG, "上报风险信息失败: " + jsonResponse.getString("error"));
                                notifyRiskReportError("服务器返回错误: " + jsonResponse.getString("error"));
                            }
                        } catch (JSONException e) {
                            XLog.e(TAG, "解析响应失败: " + e.getMessage());
                            notifyRiskReportError("解析响应失败: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        XLog.e(TAG, "上报风险信息失败: " + error);
                        notifyRiskReportError("网络请求失败: " + error);
                    }
                });
            } catch (Exception e) {
                XLog.e(TAG, "准备风险数据失败: " + e.getMessage());
                notifyRiskReportError("准备风险数据失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 直接设置事件ID，用于测试或从其他来源获取事件ID
     * @param eventId 事件ID
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    /**
     * 获取当前事件ID
     * @return 当前事件ID，如果未获取则返回null
     */
    public String getEventId() {
        return eventId;
    }
    
    private void notifyEventIdReceived(String eventId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (EventIdCallback callback : eventIdCallbacks) {
                callback.onEventIdReceived(eventId);
            }
        });
    }
    
    private void notifyEventIdError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (EventIdCallback callback : eventIdCallbacks) {
                callback.onEventIdError(error);
            }
        });
    }
    
    private void notifyReportSuccess(String eventId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ReportCallback callback : reportCallbacks) {
                callback.onReportSuccess(eventId);
            }
        });
    }
    
    private void notifyReportError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ReportCallback callback : reportCallbacks) {
                callback.onReportError(error);
            }
        });
    }
    
    private void notifyRiskReportSuccess(String eventId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (RiskReportCallback callback : riskReportCallbacks) {
                callback.onRiskReportSuccess(eventId);
            }
        });
    }
    
    private void notifyRiskReportError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (RiskReportCallback callback : riskReportCallbacks) {
                callback.onRiskReportError(error);
            }
        });
    }
} 