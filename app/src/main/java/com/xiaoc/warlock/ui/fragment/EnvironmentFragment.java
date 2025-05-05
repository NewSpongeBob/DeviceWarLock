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
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentFragment extends Fragment implements EnvironmentDetector.EnvironmentCallback {
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
                    }
                    pendingWarnings.clear();

                    // 如果没有警告，也要隐藏加载状态
                    if (adapter.getItemCount() == 0) {
                        showLoading(false);
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
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loadingHandler != null) {
            loadingHandler.removeCallbacksAndMessages(null);
        }
        if (requireActivity().isFinishing()) {
            if (detector != null) {
                detector.unregisterCallback(this);
                detector.stopDetection();
            }
            NativeEngine.stopDetect();
            showLoading(false);
        }

        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        adapter = null;
    }

    @Override
    public void onEnvironmentChanged(InfoItem newItem) {
        XLog.d(TAG, "Received environment change in fragment: " + newItem.getTitle());
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;

            if (!isDelayComplete) {
                pendingWarnings.add(newItem);
            } else {
                showLoading(false);
                adapter.addItem(newItem);
            }
            XLog.d(TAG, "Processed warning: " + newItem.getTitle());
        });
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