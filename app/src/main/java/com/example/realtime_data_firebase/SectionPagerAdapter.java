package com.example.realtime_data_firebase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SectionPagerAdapter extends FragmentStateAdapter {

    public SectionPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position) {
            case 0:
                return new GroupFragment();
            case 1:
                return new ProjectFragment();
            case 2:
                return new JobFragment();
            case 3:
                return new CollabFragment();
            case 4:
                return new EventFragment();
            case 5:
                return new ProfileFragment();
            case 6:
                return new LearningFragment();
        }

        return new GroupFragment();
    }

    @Override
    public int getItemCount() {
        return 7;
    }
}
