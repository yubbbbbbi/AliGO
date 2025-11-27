package com.example.aligo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StorePagerAdapter extends FragmentStateAdapter {

    public StorePagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new OliveyoungFragment();
        else return new DaisoFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
