package com.xiaoc.warlock.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xiaoc.warlock.Core.FingerprintCollector;
import com.xiaoc.warlock.MainActivity;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;
import com.xiaoc.warlock.crypto.EncryptUtil;
import com.xiaoc.warlock.network.NetworkClient;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FingerprintFragment extends Fragment implements FingerprintCollector.FingerprintCallback, 
        NetworkClient.EventIdCallback, NetworkClient.ReportCallback {
    
    private static final String TAG = "FingerprintFragment";
    private RecyclerView recyclerView;
    private TextView loadingText;
    private InfoAdapter adapter;
    private List<InfoItem> infoItems = new ArrayList<>();
    private NetworkClient networkClient;
    private FingerprintCollector fingerprintCollector;
    private String eventId; // 保存接收到的事件ID
    private boolean isCollectionStarted = false; // 标记是否已经开始收集
    private Handler loadingHandler;
    private int dotCount = 0;
    private static final int UPDATE_LOADING_TEXT = 1;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化Handler用于更新加载动画
        loadingHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == UPDATE_LOADING_TEXT && loadingText != null) {
                    updateLoadingText();
                    sendEmptyMessageDelayed(UPDATE_LOADING_TEXT, 500); // 每500ms更新一次
                }
            }
        };
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fingerprint, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingText = view.findViewById(R.id.loadingText);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InfoAdapter(infoItems);
        recyclerView.setAdapter(adapter);
        
        // 初始化网络客户端
        networkClient = NetworkClient.getInstance(requireContext());
        networkClient.registerEventIdCallback(this);
        networkClient.registerReportCallback(this);
        
        // 初始化指纹采集器
        fingerprintCollector = FingerprintCollector.getInstance(requireContext());
        fingerprintCollector.registerCallback(this);
        
        // 先获取事件ID，再等待指纹收集完成
        showLoading(true);
        requestEventId();
        startCheckingCollection();
    }
    
    private void updateLoadingText() {
        if (!isAdded() || loadingText == null) return;
        
        dotCount = (dotCount + 1) % 4;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("check fingerprint ing");
        
        for (int i = 0; i < dotCount; i++) {
            SpannableString dot = new SpannableString(" .");
            dot.setSpan(new ForegroundColorSpan(Color.parseColor("#2196F3")),
                    0, dot.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(dot);
        }
        
        loadingText.setText(builder);
    }
    
    private void showLoading(boolean show) {
        if (loadingText != null) {
            loadingText.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            
            if (show) {
                dotCount = 0;
                loadingHandler.sendEmptyMessage(UPDATE_LOADING_TEXT);
            } else {
                loadingHandler.removeMessages(UPDATE_LOADING_TEXT);
            }
        }
    }
    
    private void requestEventId() {
        // 请求事件ID
        networkClient.requestEventId();
    }
    
    private void startCheckingCollection() {
        // 开始轮询检查指纹收集状态
        new Thread(() -> {
            while (!MainActivity.isCollectionComplete()) {
                try {
                    XLog.d(TAG, "等待指纹收集完成...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 指纹收集完成后，在主线程展示结果
            requireActivity().runOnUiThread(() -> {
                XLog.d(TAG, "指纹收集完成，开始展示");
                displayFingerprintInfo();
                
                // 如果有事件ID，则上报数据
                if (eventId != null && !eventId.isEmpty()) {
                    reportFingerprints(eventId);
                }
                
                showLoading(false);
            });
        }).start();
    }
    
    private void displayFingerprintInfo() {
        if (isCollectionStarted) {
            return; // 避免重复调用
        }
        
        isCollectionStarted = true;
        // 调用收集指纹的方法
        fingerprintCollector.collectFingerprint();
    }
    
    /**
     * 统一显示事件ID或错误信息
     */
    private void displayEventIdInfo(String id, boolean isSuccess, String errorMsg) {
        // 只在主线程中调用
        if (Looper.myLooper() != Looper.getMainLooper()) {
            requireActivity().runOnUiThread(() -> 
                displayEventIdInfo(id, isSuccess, errorMsg));
            return;
        }
        
        // 只有成功时才显示详细信息，失败时使用简化的错误信息
        fingerprintCollector.setAndDisplayEventId(id, isSuccess, null);
    }
    
    @Override
    public void onEventIdReceived(String eventId) {
        XLog.d(TAG, "收到事件ID: " + eventId);
        this.eventId = eventId;
        
        // 存储事件ID，等待指纹收集完成后上报
        if (MainActivity.isCollectionComplete() && !isCollectionStarted) {
            displayFingerprintInfo();
            reportFingerprints(eventId);
            showLoading(false);
        }
    }
    
    @Override
    public void onEventIdError(String error) {
        XLog.e(TAG, "获取事件ID错误: " + error);
        this.eventId = null;
        
        // 等待指纹收集完成后显示错误
        if (MainActivity.isCollectionComplete() && !isCollectionStarted) {
            displayFingerprintInfo();
            displayEventIdInfo("", false, null);
            showLoading(false);
        } else {
            // 将错误信息保存，待展示指纹时一起显示
            new Thread(() -> {
                // 等待指纹收集完成
                while (!MainActivity.isCollectionComplete()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                requireActivity().runOnUiThread(() -> {
                    displayFingerprintInfo();
                    displayEventIdInfo("", false, null);
                    showLoading(false);
                });
            }).start();
        }
    }
    
    private void reportFingerprints(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            XLog.e(TAG, "事件ID为空，无法上报指纹");
            return;
        }
        
        XLog.d(TAG, "开始上报指纹数据，事件ID: " + eventId);
        String javaFingerprint = Xson.getMapString(true);
//        EncryptUtil encryptUtil = new EncryptUtil(javaFingerprint);
//        javaFingerprint = encryptUtil.result;
        String nativeFingerprint = NativeEngine.getCollectedInfo();
        
        // 上报指纹数据
        networkClient.reportDeviceFingerprint(javaFingerprint, nativeFingerprint);
    }
    
    @Override
    public void onReportSuccess(String eventId) {
        XLog.d(TAG, "设备指纹上报成功，事件ID: " + eventId);
        displayEventIdInfo(eventId, true, null);
    }
    
    @Override
    public void onReportError(String error) {
        XLog.e(TAG, "设备指纹上报失败: " + error);
        displayEventIdInfo("", false, null);
    }
    
    @Override
    public void onFingerprintCollected(InfoItem item) {
        requireActivity().runOnUiThread(() -> {
            infoItems.add(item);
            adapter.notifyDataSetChanged();
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 取消注册回调
        fingerprintCollector.unregisterCallback(this);
        networkClient.unregisterEventIdCallback(this);
        networkClient.unregisterReportCallback(this);
        
        // 清理Handler
        if (loadingHandler != null) {
            loadingHandler.removeCallbacksAndMessages(null);
        }
    }
}