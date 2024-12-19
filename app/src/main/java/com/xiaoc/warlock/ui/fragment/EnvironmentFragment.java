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
import java.util.ArrayList;

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
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detector = EnvironmentDetector.getInstance(requireContext());
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();

        // 确保在视图创建后就注册回调
        XLog.d(TAG, "Registering callback in onViewCreated");
        detector.registerCallback(this);
        detector.startDetection();
    }
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
        // 使用LinearLayoutManager并设置堆栈方向
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // 从底部开始堆叠
        recyclerView.setLayoutManager(layoutManager);
        // 初始化适配器
        adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);

    }



    @Override
    public void onPause() {
        super.onPause();
        detector.unregisterCallback(this);
        detector.stopDetection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.unregisterCallback(this);
            detector.stopDetection();
        }
        // 清理资源
        recyclerView.setAdapter(null);
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