package com.demo.bankexdh.presenter.impl;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.demo.bankexdh.model.prefs.PreferencesRepository;
import com.demo.bankexdh.model.rest.RegisterBody;
import com.demo.bankexdh.model.rest.RegisterData;
import com.demo.bankexdh.model.rest.RestHelper;
import com.demo.bankexdh.model.rest.api.Register;
import com.demo.bankexdh.model.store.DataBaseHelper;
import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.RegistrationView;
import com.demo.bankexdh.utils.Const;
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
