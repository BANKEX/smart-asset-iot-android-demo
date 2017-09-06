package com.bkx.lab.presenter.base;

public interface RegistrationView extends MvpView {
    void showAssetId(String assetId);

    void showGetAssetIdError();

    void showIntro();

    void onRegistered();

    void onRegistrationError();
}
