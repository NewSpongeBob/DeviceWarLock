package com.xiaoc.warlock.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xiaoc.warlock.Core.DetectCallback;
import com.xiaoc.warlock.Core.EnvironmentDetector;
import com.xiaoc.warlock.R;
import java.util.ArrayList;

import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.network.NetworkClient;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentFragment extends Fragment implements EnvironmentDetector.EnvironmentCallback, NetworkClient.RiskReportCallback {
    private RecyclerView recyclerView;
    private InfoAdapter adapter;
    private TextView loadingText;
    private EnvironmentDetector detector;
    private String TAG = "EnvironmentFragment";
    private boolean isDetectionRunning = false;
    private Handler loadingHandler;
    private int dotCount = 0;
    private static final int UPDATE_LOADING_TEXT = 1;
    private final List<InfoItem> pendingWarnings = new ArrayList<>(); // 用于存储延迟期间收到的警告
    private boolean isDelayComplete = false;
    private NetworkClient networkClient;
    private boolean isRiskReportScheduled = false;
    private final List<InfoItem> riskItems = new ArrayList<>(); // 存储风险项
    private Handler riskReportHandler = new Handler(Looper.getMainLooper());
    private static final long RISK_REPORT_DELAY = 10000; // 10秒延迟
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detector = EnvironmentDetector.getInstance(requireContext());
        loadingHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == UPDATE_LOADING_TEXT && loadingText != null) {
                    updateLoadingText();
                    sendEmptyMessageDelayed(UPDATE_LOADING_TEXT, 500); // 每500ms更新一次
                }
            }
        };
        
        // 初始化网络客户端
        networkClient = NetworkClient.getInstance(requireContext());
        networkClient.registerRiskReportCallback(this);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_environment, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingText = view.findViewById(R.id.loadingText);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
    }

    private void startDetection() {
        if (isDetectionRunning) return; // 防止重复启动

        isDelayComplete = false;
        pendingWarnings.clear();
        riskItems.clear();
        isRiskReportScheduled = false;

        // 启动延时线程
        new Thread(() -> {
            try {
                Thread.sleep(10);
                isDelayComplete = true;

                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;


                    // 显示所有积累的警告
                    for (InfoItem warning : pendingWarnings) {
                        adapter.addItem(warning);
                        
                        // 检查是否为风险项（含有level字段）
                        if (hasLevelField(warning)) {
                            riskItems.add(warning);
                        }
                    }
                    pendingWarnings.clear();

                    // 如果没有警告，也要隐藏加载状态
                    if (adapter.getItemCount() == 0) {
                        showLoading(false);
                    }
                    
                    // 安排10秒后的风险上报（仅一次）
                    if (!riskItems.isEmpty() && !isRiskReportScheduled) {
                        scheduleRiskReport();
                    }
                });

                // 启动检测
                XLog.d(TAG, "Starting Java detection");
                detector.registerCallback(this);
                detector.startDetection();

                XLog.d(TAG, "Starting Native detection");
                NativeEngine.startDetect(nativeCallback);

                isDetectionRunning = true;

            } catch (InterruptedException e) {
                XLog.e(TAG, "Delay interrupted: " + e.getMessage());
            }
        }).start();
    }

    // 检查InfoItem是否包含level字段（风险项）
    private boolean hasLevelField(InfoItem item) {
        for (InfoItem.DetailItem detail : item.getDetails()) {
            if ("level".equals(detail.getKey())) {
                return true;
            }
        }
        return false;
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InfoAdapter(true);
        recyclerView.setAdapter(adapter);

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = 8;
            }
        });
    }

    private final DetectCallback nativeCallback = new DetectCallback() {
        @Override
        public Context getContext() {
            return requireContext().getApplicationContext();
        }

        @Override
        public void onDetectWarning(String type, String level, String detail) {
            if (!isAdded()) return;

            InfoItem warning = new WarningBuilder(type, null)
                    .addDetail("check", detail)
                    .addDetail("level", level)
                    .build();

            onEnvironmentChanged(warning);
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        if (!isDetectionRunning) {
            showLoading(true);
            startDetection();
        } else if (!riskItems.isEmpty() && !isRiskReportScheduled) {
            // 如果有风险项但没有安排上报，重新安排上报
            XLog.d(TAG, "Fragment恢复，重新安排风险上报");
            scheduleRiskReport();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 取消加载动画相关的消息
        if (loadingHandler != null) {
            loadingHandler.removeMessages(UPDATE_LOADING_TEXT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理Handler资源
        if (loadingHandler != null) {
            loadingHandler.removeCallbacksAndMessages(null);
            loadingHandler = null;
        }
        if (riskReportHandler != null) {
            riskReportHandler.removeCallbacksAndMessages(null);
            riskReportHandler = null;
        }
        
        // 停止检测
        if (detector != null) {
            detector.unregisterCallback(this);
            detector.stopDetection();
        }
        NativeEngine.stopDetect();
        showLoading(false);
        
        // 注销回调
        if (networkClient != null) {
            networkClient.unregisterRiskReportCallback(this);
        }
        
        // 清理列表资源
        synchronized (riskItems) {
            riskItems.clear();
        }
        synchronized (pendingWarnings) {
            pendingWarnings.clear();
        }

        // 清理RecyclerView资源
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        adapter = null;
    }

    @Override
    public void onEnvironmentChanged(InfoItem newItem) {
        XLog.d(TAG, "Received environment change in fragment: " + newItem.getTitle());
        
        // 检查是否为风险项（含有level字段）
        boolean isRiskItem = hasLevelField(newItem);
        if (isRiskItem) {
            // 无论Fragment是否活跃，都将风险项添加到列表中
            synchronized (riskItems) {
                riskItems.add(newItem);
            }
            XLog.d(TAG, "添加风险项: " + newItem.getTitle());
        }
        
        // 只有在Fragment活跃时才更新UI
        if (isAdded() && !isDetached() && getActivity() != null) {
            try {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
    
                    if (!isDelayComplete) {
                        pendingWarnings.add(newItem);
                    } else {
                        showLoading(false);
                        adapter.addItem(newItem);
                    }
                    XLog.d(TAG, "Processed warning in UI: " + newItem.getTitle());
                });
            } catch (Exception e) {
                XLog.e(TAG, "更新UI失败: " + e.getMessage());
            }
        } else {
            XLog.d(TAG, "Fragment不活跃，跳过UI更新");
            // 如果Fragment不活跃，但延迟已完成，仍然将警告添加到pendingWarnings
            if (isDelayComplete && !isRiskItem) {
                synchronized (pendingWarnings) {
                    pendingWarnings.add(newItem);
                }
            }
        }
    }

    // 风险上报任务（延迟10秒）
    private void scheduleRiskReport() {
        if (!isRiskReportScheduled) {
            isRiskReportScheduled = true;
            XLog.d(TAG, "风险上报任务，将在10秒后执行");
            
            // 安排新任务，延迟10秒后执行一次性上报
            riskReportHandler.postDelayed(this::reportRiskItems, RISK_REPORT_DELAY);
        }
    }
    
    // 上报风险项
    private void reportRiskItems() {
        List<InfoItem> itemsToReport;
        
        synchronized (riskItems) {
            if (riskItems.isEmpty()) {
                XLog.d(TAG, "没有风险项需要上报");
                return;
            }
            
            // 创建一个副本，避免并发修改问题
            itemsToReport = new ArrayList<>(riskItems);
        }
        
        // 检查是否有事件ID
        String eventId = networkClient.getEventId();
        if (eventId == null || eventId.isEmpty()) {
            XLog.d(TAG, "没有事件ID，不进行上报");
            return;
        }
        
        XLog.d(TAG, "开始上报风险信息，共 " + itemsToReport.size() + " 项");
        // 上报风险信息（只上报一次）
        networkClient.reportRiskInfo(itemsToReport);
    }

    // NetworkClient.RiskReportCallback 接口实现
    @Override
    public void onRiskReportSuccess(String eventId) {
        XLog.d(TAG, "风险信息上报成功: " + eventId);
    }

    @Override
    public void onRiskReportError(String error) {
        XLog.e(TAG, "风险信息上报失败: " + error);
        isRiskReportScheduled = false;
    }

    private void updateLoadingText() {
        if (!isAdded() || loadingText == null) return;

        dotCount = (dotCount + 1) % 4;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("check env ing");

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

}