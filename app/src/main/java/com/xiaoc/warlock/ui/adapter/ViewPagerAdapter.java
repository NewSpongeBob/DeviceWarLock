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
        switch (position) {
            case 0:
                return new FingerprintFragment();
            case 1:
                return new EnvironmentFragment();
            default:
                return new FingerprintFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}