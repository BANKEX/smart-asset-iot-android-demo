package com.demo.bankexdh.presenter.impl;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.demo.bankexdh.model.prefs.PreferencesRepository;
import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.RegistrationView;

import timber.log.Timber;

public class RegistrationPresenter extends AbstractPresenter<RegistrationView> {

    private static final String ASSET_ID_QUERY_PARAMETER = "id";
    private final PreferencesRepository preferencesRepository;


    public RegistrationPresenter(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    public void onScanCancelled() {

    }
    @Override
    public void onViewAttached(RegistrationView view) {
        super.onViewAttached(view);

        if (preferencesRepository.isFirstRun()) {
            view.showIntro();
            preferencesRepository.setFirstRun(false);
        }
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
