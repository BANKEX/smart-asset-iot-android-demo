package com.demo.bankexdh.presenter.base;

public interface RegistrationView extends MvpView {
    void showAssetId(String assetId);

    void showGetAssetIdError();

    void showIntro();
}
