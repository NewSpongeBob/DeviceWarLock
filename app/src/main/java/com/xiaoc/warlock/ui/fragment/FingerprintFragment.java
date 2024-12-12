package com.xiaoc.warlock.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xiaoc.warlock.R;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class FingerprintFragment extends Fragment {
    private RecyclerView recyclerView;

    private InfoAdapter adapter;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fingerprint, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        initRecyclerView();
        return view;
    }
    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);
        List<InfoItem> items = new ArrayList<>();
        items.add(new InfoItem("Root状态", checkRoot()));
        items.add(new InfoItem("模拟器检测", checkEmulator()));
        items.add(new InfoItem("调试状态", checkDebug()));
        // ... 添加更多环境检测信息
        adapter.setItems(items);
    }

    private String checkRoot() {
        // 实现Root检测逻辑
        return "未检测到Root";
    }

    private String checkEmulator() {
        // 实现模拟器检测逻辑
        return "真实设备";
    }

    private String checkDebug() {
        // 实现调试检测逻辑
        return "未开启调试";
    }
}
