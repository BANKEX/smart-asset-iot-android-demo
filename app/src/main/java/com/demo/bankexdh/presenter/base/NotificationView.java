package com.demo.bankexdh.presenter.base;

public interface NotificationView extends MvpView {
    void onNotificationSent();

    void onError();
}