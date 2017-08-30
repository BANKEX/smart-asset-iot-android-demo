package com.demo.bankexdh.presenter.impl;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.RegistrationView;

import timber.log.Timber;

public class RegistrationPresenter extends AbstractPresenter<RegistrationView> {

    private static final String ASSET_ID_QUERY_PARAMETER = "id";

    public void onScanCancelled() {

    }

    public void onScanCompleted(@NonNull String contents) {
        String assetId = parseAssetId(contents);
        if (assetId != null) {
            view.showAssetId(assetId);
        } else {
            view.showGetAssetIdError();
        }
    }

    private String parseAssetId(String contents) {
        try {
            return Uri.parse(contents).getQueryParameter(ASSET_ID_QUERY_PARAMETER);
        } catch (Exception e) {
            Timber.e(e, "Failed to parse scanned content: %s", contents);
        }
        return null;
    }

    @Override
    protected void onDestroyed() {

    }
}
