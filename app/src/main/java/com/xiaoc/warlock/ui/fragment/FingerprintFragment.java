package com.xiaoc.warlock.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import com.xiaoc.warlock.Core.EnvironmentDetector;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class FingerprintFragment extends Fragment {
    private RecyclerView recyclerView;
    private InfoAdapter adapter;
    private TextView loadingText;
    private Handler loadingHandler;
    private int dotCount = 0;
    private static final int UPDATE_LOADING_TEXT = 1;
    private final List<InfoItem> pendingWarnings = new ArrayList<>();
    private boolean isCollecting = false;
    private static final String TAG = "FingerprintFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadingHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == UPDATE_LOADING_TEXT && loadingText != null) {
                    updateLoadingText();
                    sendEmptyMessageDelayed(UPDATE_LOADING_TEXT, 500);
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
        loadDeviceInfo();
//        if (!isCollecting) {
//            showLoading(true);
//            startCollection();
//        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InfoAdapter(false);
        recyclerView.setAdapter(adapter);
    }


    private void startCollection() {
        if (isCollecting) return;
        isCollecting = true;

        // TODO: 实现实际的设备指纹收集逻辑
        XLog.d(TAG, "Starting fingerprint collection");
    }

    private void updateLoadingText() {
        dotCount = (dotCount + 1) % 4;
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < dotCount; i++) {
            dots.append(" .");
        }
        loadingText.setText("collecting fingerprint ing" + dots);
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

    private void loadDeviceInfo() {
        List<InfoItem> items = new ArrayList<>();

        try {
            // 基本信息
            InfoItem basicInfo = new InfoItem("基本信息", "设备基本信息");
            basicInfo.addDetail("设备型号", Build.MODEL);
            basicInfo.addDetail("Android版本", Build.VERSION.RELEASE);
            basicInfo.addDetail("系统版本", Build.DISPLAY);
            items.add(basicInfo);

            // 硬件信息
            InfoItem hardwareInfo = new InfoItem("硬件信息", "设备硬件信息");
            if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
                hardwareInfo.addDetail("CPU架构", Build.SUPPORTED_ABIS[0]);
            }
            hardwareInfo.addDetail("制造商", Build.MANUFACTURER);
            hardwareInfo.addDetail("品牌", Build.BRAND);
            items.add(hardwareInfo);

            adapter.setItems(items);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 这个方法将来用于接收设备指纹收集结果
    public void onFingerprintCollected(InfoItem newItem) {
        requireActivity().runOnUiThread(() -> {
            if (adapter != null && isAdded()) {
                showLoading(false);
                adapter.addItem(newItem);
                recyclerView.smoothScrollToPosition(0);
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
        isCollecting = false;
    }
}