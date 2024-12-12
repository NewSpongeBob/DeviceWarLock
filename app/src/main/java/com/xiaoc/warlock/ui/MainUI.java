package com.xiaoc.warlock.ui;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.xiaoc.warlock.R;
import com.xiaoc.warlock.ui.adapter.ViewPagerAdapter;

public class MainUI {
    private final FragmentActivity activity;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter pagerAdapter;
    public MainUI(FragmentActivity activity) {
        this.activity = activity;
        initUI();
        setupStatusBar();
    }

    private void initUI() {
        // 设置布局
        activity.setContentView(R.layout.activity_main);

        // 初始化视图
        viewPager = activity.findViewById(R.id.viewPager);
        tabLayout = activity.findViewById(R.id.tabLayout);

        // 设置适配器
        pagerAdapter = new ViewPagerAdapter(activity);
        viewPager.setAdapter(pagerAdapter);

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
            // 使用字符串资源
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
    // 获取当前页面索引
    public int getCurrentPage() {
        return viewPager.getCurrentItem();
    }

    // 切换到指定页面
    public void setCurrentPage(int page) {
        viewPager.setCurrentItem(page);
    }

    // 添加新的控制方法
    public void enableUserSwipe(boolean enable) {
        viewPager.setUserInputEnabled(enable);
    }

    public void setPageTransformer(ViewPager2.PageTransformer transformer) {
        viewPager.setPageTransformer(transformer);
    }

}