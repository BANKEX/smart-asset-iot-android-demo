package com.bkx.lab.presenter.impl;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bkx.lab.model.ImageManager;
import com.bkx.lab.model.parser.ChainParser;
import com.bkx.lab.model.prefs.PreferencesRepository;
import com.bkx.lab.model.rest.ImageNotificationData;
import com.bkx.lab.model.rest.RegisterBody;
import com.bkx.lab.model.rest.RegisterData;
import com.bkx.lab.model.rest.RestHelper;
import com.bkx.lab.model.rest.ShakeNotificationData;
import com.bkx.lab.model.rest.api.Register;
import com.bkx.lab.model.store.DataBaseHelper;
import com.bkx.lab.presenter.base.AbstractPresenter;
import com.bkx.lab.presenter.base.NotificationView;
import com.bkx.lab.utils.Const;
import com.bkx.lab.utils.ImageUtils;
import com.bkx.lab.utils.ShakeDetector;
import com.devicehive.rest.ApiClient;
import com.devicehive.rest.api.DeviceNotificationApi;
import com.devicehive.rest.auth.ApiKeyAuth;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.InsertNotification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainPresenter extends AbstractPresenter<NotificationView> implements ShakeDetector.Listener {
    private DeviceNotificationApi deviceNotificationApi;

    private boolean enabled;
    private boolean canExecute = true;

    private String link;
    private Location location;

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 16777216;
    private final PreferencesRepository preferencesRepository;
    private ApiClient client;

    private DataBaseHelper dbHelper = DataBaseHelper.getInstance();

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    public MainPresenter(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
        isRegistrationInProgress.set(false);
        client = RestHelper.getInstance().getApiClient();
        enabled = dbHelper.isEnabled();
    }

    public void prepare() {
        if (dbHelper.isDeviceRegistered()) {
            enabled = dbHelper.isEnabled();
            setupPresenter();
        } else {
            if (!isViewNull()) {
                view.onUnregistered();
            }
        }

    }

    private void setupPresenter() {
        String token = dbHelper.getToken();
        addAuth(token);
        createServices();
    }

    private void addAuth(String accessToken) {
        client.addAuthorization(ApiClient.AUTH_API_KEY,
                ApiKeyAuth.newInstance(accessToken));
    }

    private void createServices() {
        deviceNotificationApi = client.createService(DeviceNotificationApi.class);
    }

    public void uploadFile(Context context) {
        Observable.just(context)
                .observeOn(Schedulers.newThread())
                .map((ctx) -> {
                    ImageUtils.getInstance().setOrientation();
                    boolean renamed = ImageUtils.getInstance().renameFile(ctx);
                    if (!renamed) {
                        throw new RuntimeException("File not renamed");
                    }
                    Uri filePath = Uri.parse(ImageUtils.getInstance().getCurrentPhotoPath());
                    InputStream imageStream = ctx.getContentResolver().openInputStream(filePath);
                    int imageLength = imageStream.available();
                    return ImageManager.getInstance()
                            .uploadImage(filePath.getLastPathSegment(), imageStream, imageLength);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((link) -> {
                    this.link = link;
                    sendLocationNotification(location, link);
                    Timber.d(link);
                }, t -> {
                    t.printStackTrace();
                    clearLocationNotificationData();
                    if (!isViewNull()) {
                        view.onError();
                    }
                });

    }

    public String getDeviceId() {
        return dbHelper.getDeviceId();
    }

    public File createImageFile(Context context) throws IOException, NoSuchAlgorithmException {
        return ImageUtils.getInstance().createImageFile(context);
    }

    public void onShake() {
        hearShake(null);
    }

    @Override
    public void hearShake(@Nullable String timestamp) {
        if (enabled && canExecute) {
            canExecute = false;
            if (!TextUtils.isEmpty(dbHelper.getDeviceId())) {
                sendNotification(ShakeNotificationData.getNotification("Shaked",
                        dbHelper.getDeviceName(),
                        dbHelper.getDeviceName()), new Callback<InsertNotification>() {
                    @Override
                    public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                        Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                        if (response.isSuccessful()) {
                            if (!isViewNull()) {
                                view.onShakeNotificationSent();
                            }
                        } else {
                            if (!isViewNull()) {
                                if (response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                                    dbHelper.clearDevice();
                                    view.onUnregistered();
                                } else {
                                    view.onShakeError();
                                }
                            }
                        }

                        canExecute = true;
                    }

                    @Override
                    public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable
                            t) {
                        Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                        if (!isViewNull()) {
                            view.onShakeError();
                        }
                        canExecute = true;
                    }
                });
            }
        }

    }

    public void onLocationChanged(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    private void sendLocationNotification(Location location, String link) {
        if (location != null && !TextUtils.isEmpty(link)) {

            DeviceNotificationWrapper wrapper = ImageNotificationData.getNotification(link,
                    dbHelper.getDeviceName(),
                    dbHelper.getDeviceId(),
                    location.getLatitude(), location.getLongitude());
            sendNotification(wrapper, new Callback<InsertNotification>() {
                @Override
                public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                    Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                    if (response.isSuccessful()) {
                        if (!isViewNull()) {
                            view.onLocationNotificationSent();
                        }
                        clearLocationNotificationData();
                    } else {
                        if (!isViewNull()) {
                            if (response.code() == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                                dbHelper.clearDevice();
                                view.onUnregistered();
                            } else {
                                view.onLocationError();
                            }
                        }
                    }
                    canExecute = true;
                }

                @Override
                public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable t) {
                    Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                    if (!isViewNull()) {
                        view.onLocationError();
                    }
                    canExecute = true;
                }
            });
        }
    }

    private void sendNotification(DeviceNotificationWrapper notificationWrapper, Callback<InsertNotification> callback) {
        if (deviceNotificationApi == null) {
            return;
        }
        Call<InsertNotification> notificationCallInsert = deviceNotificationApi.insert(dbHelper.getDeviceId(), notificationWrapper);
        notificationCallInsert.enqueue(callback);
    }

    private void clearLocationNotificationData() {
        link = null;
        location = null;
    }

    public boolean isRegistration() {
        return isRegistrationInProgress.get();
    }


    private RegisterBody getRegisterBody(@NonNull String assetId) {
        RegisterBody body = new RegisterBody();
        body.setId(assetId);
        body.setName(String.format(Const.DEVICE_NAME, assetId));
        return body;
    }

    public void register(@NonNull String assetId) {
        isRegistrationInProgress.set(true);
        Register registerCall = RestHelper
                .getInstance()
                .getRegistrationApiClient()
                .createService(Register.class);

        RegisterBody body = getRegisterBody(assetId);
        registerCall.register(body).enqueue(new Callback<RegisterData>() {
            @Override
            public void onResponse(Call<RegisterData> call, Response<RegisterData> response) {
                Timber.d(response.toString());
                if (response.isSuccessful()) {
                    Timber.d(response.body().toString());
                    RegisterData data = response.body();
                    dbHelper.insertUserModel(data);
                    dbHelper.insertDevice(data);
                    dbHelper.setEnabled(true);
                    enabled = true;
                    canExecute = true;

                    addAuth(data.getToken().getAccessToken());
                    isRegistrationInProgress.set(false);
                    createServices();
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
                if (!isViewNull()) {
                    view.onRegistrationError();
                }
            }
        });
    }

    @Override
    public void onViewAttached(NotificationView view) {
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
            view.registerOnScan();
        } else {
            view.showGetAssetIdError();
        }
    }

    public void onScanCancelled() {

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
        return ChainParser.getInstance().parseAssetId(contents);
    }

    @Override
    protected void onDestroyed() {
        clearLocationNotificationData();
    }

    public void clear() {
        dbHelper.clearUser();
        dbHelper.clearDevice();
    }
}


