package com.xiaoc.warlock.ui;


import android.os.Build;
import android.view.View;

import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.ui.adapter.ViewPagerAdapter;
import com.xiaoc.warlock.ui.dialog.DialogManager;

public class MainUI {
    private final FragmentActivity activity;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter pagerAdapter;
    private final DialogManager dialogManager;

    public MainUI(FragmentActivity activity) {
        this.activity = activity;
        this.dialogManager = new DialogManager(activity);

        initUI();
        setupStatusBar();
    }

    private void initUI() {
        // 设置布局
        activity.setContentView(R.layout.activity_main);
        // 设置 Toolbar
        ImageButton menuButton = activity.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> showBottomSheetMenu());

        // 初始化视图
        viewPager = activity.findViewById(R.id.viewPager);
        tabLayout = activity.findViewById(R.id.tabLayout);

        // 设置适配器
        pagerAdapter = new ViewPagerAdapter(activity);
        viewPager.setAdapter(pagerAdapter);

        // 设置离屏页面限制
        viewPager.setOffscreenPageLimit(1);

        // 添加页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 在这里处理页面切换事件
            }
        });

        // 设置TabLayout和ViewPager2关联
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String[] titles = new String[]{"设备指纹", "环境检测"};
            tab.setText(titles[position]);
        }).attach();
    }

    private void setupStatusBar() {
        // 设置状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(activity.getResources().getColor(R.color.primary_color));

            // 设置状态栏文字颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }


    public void showBottomSheetMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_menu_sheet, null);

        view.findViewById(R.id.btn_about).setOnClickListener(v -> {
            dialog.dismiss();
            dialogManager.showAboutDialog();
        });

//        view.findViewById(R.id.btn_explain).setOnClickListener(v -> {
//            dialog.dismiss();
//            dialogManager.showexplainDialog();
//        });

        view.findViewById(R.id.btn_feedback).setOnClickListener(v -> {
            dialog.dismiss();
            dialogManager.showFeedbackDialog();
        });

        view.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            dialog.dismiss();
            dialogManager.showSettingsDialog();
        });

        dialog.setContentView(view);
        dialog.show();
    }

}