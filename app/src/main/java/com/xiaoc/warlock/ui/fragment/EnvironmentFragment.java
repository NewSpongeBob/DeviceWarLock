package com.xiaoc.warlock.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    private EnvironmentDetector detector;
    private String TAG = "EnvironmentFragment";
    private boolean isDetectionRunning = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detector = EnvironmentDetector.getInstance(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 只在这里初始化一次RecyclerView
        initRecyclerView();

        if (!isDetectionRunning) {
            // 启动Java检测
            XLog.d(TAG, "Starting Java detection");
            detector.registerCallback(this);
            detector.startDetection();

            // 启动Native检测
            XLog.d(TAG, "Starting Native detection");
            NativeEngine.startDetect(nativeCallback);

            isDetectionRunning = true;
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_environment, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        return view;
    }
    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);
    }


    // 定义Native检测回调
    private final DetectCallback nativeCallback = new DetectCallback() {
        @Override
        public void onDetectWarning(String type, String level, String detail) {
            InfoItem warning = new WarningBuilder(type, null)
                    .addDetail("check", detail)
                    .addDetail("level", level)
                    .build();

            onEnvironmentChanged(warning);
        }
    };

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 只在Fragment真正销毁时停止检测
        if (requireActivity().isFinishing()) {
            if (detector != null) {
                detector.unregisterCallback(this);
                detector.stopDetection();
            }
            NativeEngine.stopDetect();
        }

        // 清理UI相关资源
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        adapter = null;
    }

    @Override
    public void onEnvironmentChanged(InfoItem newItem) {
        XLog.d(TAG, "Received environment change in fragment: " + newItem.getTitle());
        requireActivity().runOnUiThread(() -> {
            if (adapter != null && isAdded()) {
                adapter.addItem(newItem);
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                XLog.d(TAG, "Added item to adapter: " + newItem.getTitle());
            }
        });
    }
}