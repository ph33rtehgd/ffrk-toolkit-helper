package com.ffrktoolkit.ffrktoolkithelper.views;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragmentList = new ArrayList();
    private final List<String> mFragmentTitleList = new ArrayList();

    public ViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    public void addFragment(Fragment fragment, String title) {
        this.mFragmentList.add(fragment);
        this.mFragmentTitleList.add(title);
    }

    public int getCount() {
        return this.mFragmentList.size();
    }

    public Fragment getItem(int paramInt) {
        return (Fragment)this.mFragmentList.get(paramInt);
    }

    public CharSequence getPageTitle(int paramInt) {
        return (CharSequence)this.mFragmentTitleList.get(paramInt);
    }

}
