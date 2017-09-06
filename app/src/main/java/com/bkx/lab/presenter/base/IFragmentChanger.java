package com.bkx.lab.presenter.base;

import android.support.v4.app.Fragment;

public interface IFragmentChanger {

    void addFragment(Fragment f, String tag, boolean isAddToBackStack);

    void replaceFragment(Fragment f, String tag);

    boolean isFragmentAdded(String tag);
}