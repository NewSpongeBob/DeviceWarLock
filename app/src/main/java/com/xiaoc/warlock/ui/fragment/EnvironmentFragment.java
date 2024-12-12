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
import com.xiaoc.warlock.R;
import java.util.ArrayList;
import com.xiaoc.warlock.ui.adapter.InfoAdapter;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentFragment extends Fragment {
    private RecyclerView recyclerView;
    private InfoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_environment, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        initRecyclerView();
        return view;
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);

        // 添加环境检测数据
        List<InfoItem> items = new ArrayList<>();
        items.add(new InfoItem("Root状态", "未检测到Root"));
        items.add(new InfoItem("模拟器检测", "真实设备"));
        items.add(new InfoItem("调试状态", "未开启调试"));
        items.add(new InfoItem("VPN状态", "未使用VPN"));
        items.add(new InfoItem("系统完整性", "系统完整"));
        // ... 添加更多环境检测信息
        adapter.setItems(items);
    }
}