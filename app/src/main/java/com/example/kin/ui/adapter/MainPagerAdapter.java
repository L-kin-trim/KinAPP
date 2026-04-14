package com.example.kin.ui.adapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class MainPagerAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragments;

    public MainPagerAdapter(@NonNull AppCompatActivity activity, List<Fragment> fragments) {
        super(activity);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
