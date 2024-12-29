package com.xiaoc.warlock.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xiaoc.warlock.ui.fragment.FingerprintFragment;
import com.xiaoc.warlock.ui.fragment.EnvironmentFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final Fragment fingerprintFragment;
    private final Fragment environmentFragment;
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fingerprintFragment = new FingerprintFragment();
        environmentFragment = new EnvironmentFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 返回已创建的Fragment实例
        return position == 0 ? fingerprintFragment : environmentFragment;
    }

    @Override
    public long getItemId(int position) {
        // 返回固定的ID，防止重建
        return position;
    }

    @Override
    public boolean containsItem(long itemId) {
        // 确保ViewPager2不会重建Fragment
        return itemId >= 0 && itemId < getItemCount();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}