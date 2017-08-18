package com.demo.bankexdh.model.store;

import android.content.Context;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmContract {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "bankex.realm";

    public static void configureRealm(Context context) {
        Realm.init(context);
        getDefaultRealmConfiguration();
    }

    private static RealmConfiguration getDefaultRealmConfiguration() {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name(DATABASE_NAME)
                .schemaVersion(DATABASE_VERSION)
                .deleteRealmIfMigrationNeeded()
                .build();
        // Use the config
        Realm.setDefaultConfiguration(config);
        return config;
    }
}
