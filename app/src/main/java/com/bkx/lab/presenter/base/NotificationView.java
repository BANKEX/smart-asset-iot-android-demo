package com.bkx.lab.presenter.base;

import com.bkx.lab.model.LocationEntity;

public interface NotificationView extends MvpView, RegistrationView {
    void onShakeNotificationSent();

    void onLocationNotificationSent(LocationEntity location);

    void onLocationError();

    void onShakeError();

    void onError();

    void onUnregistered();

    void registerOnScan();

}