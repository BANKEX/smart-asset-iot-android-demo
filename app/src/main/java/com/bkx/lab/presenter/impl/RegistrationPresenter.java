package com.bkx.lab.presenter.impl;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.bkx.lab.model.prefs.PreferencesRepository;
import com.bkx.lab.model.rest.RegisterBody;
import com.bkx.lab.model.rest.RegisterData;
import com.bkx.lab.model.rest.RestHelper;
import com.bkx.lab.model.rest.api.Register;
import com.bkx.lab.model.store.DataBaseHelper;
import com.bkx.lab.presenter.base.AbstractPresenter;
import com.bkx.lab.presenter.base.RegistrationView;
import com.bkx.lab.utils.Const;
import com.devicehive.rest.ApiClient;
import com.devicehive.rest.auth.ApiKeyAuth;

import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class RegistrationPresenter extends AbstractPresenter<RegistrationView> {

    private static final String ASSET_ID_QUERY_PARAMETER = "id";
    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 16777216;
    public static final String LINK_PARAM = "link";
    private final PreferencesRepository preferencesRepository;
    private ApiClient client;

    private DataBaseHelper dbHelper = DataBaseHelper.getInstance();

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    public RegistrationPresenter(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
        isRegistrationInProgress.set(false);
    }

    public void onScanCancelled() {

    }

    public boolean isRegistration() {
        return isRegistrationInProgress.get();
    }

    private void addAuth(String accessToken) {
        client.addAuthorization(ApiClient.AUTH_API_KEY,
                ApiKeyAuth.newInstance(accessToken));
    }

    private RegisterBody getRegisterBody(@NonNull String assetId) {
        RegisterBody body = new RegisterBody();
        body.setId(assetId);
        body.setName(String.format(Const.DEVICE_NAME, assetId));
        return body;
    }

    public void register(@NonNull String assetId) {
        isRegistrationInProgress.set(true);
        Register registerCall = client.createService(Register.class);
        RegisterBody body = getRegisterBody(assetId);
        registerCall.register(Const.REGISTER_URL, body).enqueue(new Callback<RegisterData>() {
            @Override
            public void onResponse(Call<RegisterData> call, Response<RegisterData> response) {
                Timber.d(response.toString());
                if (response.isSuccessful()) {
                    Timber.d(response.body().toString());
                    RegisterData data = response.body();
                    dbHelper.insertUserModel(data);
                    dbHelper.insertDevice(data);

                    addAuth(data.getToken().getAccessToken());
                    isRegistrationInProgress.set(false);
                    if (!isViewNull()) {
                        view.onRegistered();
                    }
                } else {
                    isRegistrationInProgress.set(false);
                    if (!isViewNull()) {
                        view.onRegistrationError();
                    }
                }
            }

            @Override
            public void onFailure(Call<RegisterData> call, Throwable t) {
                Timber.d(t.getMessage());
                isRegistrationInProgress.set(false);
            }
        });
    }

    @Override
    public void onViewAttached(RegistrationView view) {
        client = RestHelper.getInstance().getApiClient();
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

    public boolean validate(@NonNull String assetId) {
        long value;
        try {
            value = Long.valueOf(assetId);
        } catch (NumberFormatException e) {
            return false;
        }

        return (value >= MIN_VALUE && value <= MAX_VALUE);
    }

    private String parseAssetId(String contents) {
        try {
            return Uri.parse(Uri.parse(contents)
                    .getQueryParameter(LINK_PARAM))
                    .getQueryParameter(ASSET_ID_QUERY_PARAMETER);

        } catch (Exception e) {
            Timber.e(e, "Failed to parse scanned content: %s", contents);


        }
        return parseAssetIdOldFormat(contents);
    }

    private String parseAssetIdOldFormat(String contents) {
        try {
            return Uri.parse(contents)
                    .getQueryParameter(ASSET_ID_QUERY_PARAMETER);
        } catch (Exception ex) {
            Timber.e(ex, "Failed to parse scanned content: %s", contents);

        }
        return null;
    }

    @Override
    protected void onDestroyed() {

    }
}
