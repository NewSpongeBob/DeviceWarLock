package com.xiaoc.warlock.ui.fragment;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xiaoc.warlock.Core.FingerprintCollector;
import com.xiaoc.warlock.MainActivity;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;


public class FingerprintFragment extends Fragment implements FingerprintCollector.FingerprintCallback {
    private RecyclerView recyclerView;
    private InfoAdapter adapter;
    private TextView loadingText;
    private Handler loadingHandler;
    private int dotCount = 0;
    private static final int UPDATE_LOADING_TEXT = 1;
    private static final int CHECK_COLLECTION_STATUS = 2;
    private boolean isCollecting = false;
    private static final String TAG = "FingerprintFragment";
    private FingerprintCollector collector;
    
    // 检查收集状态的间隔时间（毫秒）
    private static final long CHECK_INTERVAL = 500;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        collector = FingerprintCollector.getInstance(requireContext());
        collector.registerCallback(this);
        loadingHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == UPDATE_LOADING_TEXT && loadingText != null) {
                    updateLoadingText();
                    sendEmptyMessageDelayed(UPDATE_LOADING_TEXT, 500);
                } else if (msg.what == CHECK_COLLECTION_STATUS) {
                    checkCollectionStatus();
                }
            }
        };
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fingerprint, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingText = view.findViewById(R.id.loadingText);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        if (!isCollecting) {
            showLoading(true);
            // 检查是否已经收集完成
            if (MainActivity.isCollectionComplete()) {
                // 直接开始展示
                startCollection();
            } else {
                // 开始等待收集完成
                waitForCollection();
            }
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InfoAdapter(false);
        recyclerView.setAdapter(adapter);
    }
    
    private void waitForCollection() {
        // 开始周期性检查收集状态
        loadingHandler.sendEmptyMessage(CHECK_COLLECTION_STATUS);
        // 确保加载提示开始显示
        loadingHandler.sendEmptyMessage(UPDATE_LOADING_TEXT);
    }
    
    private void checkCollectionStatus() {
        // 使用MainActivity的静态变量检查收集状态
        if (MainActivity.isCollectionComplete()) {
            XLog.d(TAG, "Collection complete, starting to display fingerprints");
            startCollection();
        } else {
            XLog.d(TAG, "Waiting for fingerprint collection to complete");
            // 继续等待
            loadingHandler.sendEmptyMessageDelayed(CHECK_COLLECTION_STATUS, CHECK_INTERVAL);
        }
    }

    private void startCollection() {
        if (isCollecting) return;
        isCollecting = true;

        XLog.d(TAG, "Starting fingerprint display");
        
        // 使用FingerprintCollector来解析和展示指纹信息
        collector.collectFingerprint();
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

    @Override
    public void onFingerprintCollected(InfoItem newItem) {
        requireActivity().runOnUiThread(() -> {
            if (adapter != null && isAdded()) {
                showLoading(false);
                adapter.addItem(newItem);
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                XLog.d(TAG, "Added fingerprint item: " + newItem.getTitle());
            }
        });
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loadingHandler != null) {
            loadingHandler.removeCallbacksAndMessages(null);
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        adapter = null;
        if (collector != null) {
            collector.unregisterCallback(this);
        }
        isCollecting = false;
    }
}