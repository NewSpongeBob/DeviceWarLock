package com.xiaoc.warlock.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xiaoc.warlock.ui.fragment.FingerprintFragment;
import com.xiaoc.warlock.ui.fragment.EnvironmentFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 每次需要时创建新的 Fragment 实例
        return position == 0 ? new FingerprintFragment() : new EnvironmentFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}