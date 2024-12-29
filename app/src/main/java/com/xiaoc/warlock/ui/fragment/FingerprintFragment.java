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

import com.xiaoc.warlock.Core.EnvironmentDetector;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class FingerprintFragment extends Fragment {
    private RecyclerView recyclerView;
    private InfoAdapter adapter;
    private boolean isInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fingerprint, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 只初始化一次
        if (!isInitialized) {
            initRecyclerView();
            isInitialized = true;
        }
    }

    private void initRecyclerView() {
        if (getContext() == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);

        loadDeviceInfo();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        adapter = null;
    }
}