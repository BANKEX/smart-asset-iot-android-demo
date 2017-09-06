package com.bkx.lab.presenter.base;

public interface NotificationView extends MvpView {
    void onShakeNotificationSent();

    void onLocationNotificationSent();

    void onError();

    void onUnregistered();

}