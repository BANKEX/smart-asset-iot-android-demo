package com.bkx.lab.model.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesRepository {

    private static final String TAG = "PreferencesRepository";

    private static final String KEY_FIRST_RUN = TAG + "key_first_run";

    private final SharedPreferences sharedPreferences;

    public PreferencesRepository(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isFirstRun() {
        return sharedPreferences.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, firstRun).apply();
    }
}
