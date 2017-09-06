package com.bkx.lab;

import android.content.Context;

import com.bkx.lab.model.store.RealmContract;
import com.google.firebase.FirebaseApp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import timber.log.Timber;

@Data
@EqualsAndHashCode(callSuper = false)
public class DemoApplication extends android.app.Application {

    public static DemoApplication get(Context context) {
        return (DemoApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        RealmContract.configureRealm(this);
        FirebaseApp.initializeApp(this);
    }
}
