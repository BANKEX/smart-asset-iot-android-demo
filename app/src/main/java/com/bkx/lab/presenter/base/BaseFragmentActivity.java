package com.bkx.lab.presenter.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.bkx.lab.R;


public abstract class BaseFragmentActivity extends AppCompatActivity implements IFragmentChanger {
    protected static final String CHROME_PACKAGE_NAME = "com.android.chrome";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActivityComponent();

    }

    @Override
    public void addFragment(Fragment f, String tag, boolean isAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, f);
        if (isAddToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }

        fragmentTransaction.commit();
    }

    protected void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        final int backStackEntryCount = fm.getBackStackEntryCount();
        for (int i = 0; i < backStackEntryCount; ++i) {
            fm.popBackStack();
        }
    }

    @Override
    public void replaceFragment(Fragment f, String tag) {
        replaceFragment(f, tag, false);
    }

    public void replaceFragment(Fragment f, String tag, boolean executeSynchronously) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f, tag)
                .commit();
        if (executeSynchronously) getSupportFragmentManager().executePendingTransactions();
    }


    public void replaceFragment(Fragment f, String tag, boolean executeSynchronously, boolean isAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, f);
        if (isAddToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.commit();
        if (executeSynchronously) getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public boolean isFragmentAdded(String tag) {
        return getSupportFragmentManager().findFragmentByTag(tag) != null;
    }


    protected abstract void setupActivityComponent();
}
