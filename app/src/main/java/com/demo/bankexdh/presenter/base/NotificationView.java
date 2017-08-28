package com.demo.bankexdh.presenter.base;

public interface NotificationView extends MvpView {
    void onShakeNotificationSent();

    void onLocationNotificationSent();

    void onError();

    void showIntro();
}