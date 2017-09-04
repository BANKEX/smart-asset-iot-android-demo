package com.demo.bankexdh.presenter.impl;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.demo.bankexdh.model.ImageManager;
import com.demo.bankexdh.model.rest.ImageNotificationData;
import com.demo.bankexdh.model.rest.RestHelper;
import com.demo.bankexdh.model.rest.ShakeNotificationData;
import com.demo.bankexdh.model.store.DataBaseHelper;
import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.NotificationView;
import com.demo.bankexdh.utils.ImageUtils;
import com.demo.bankexdh.utils.ShakeDetector;
import com.devicehive.rest.ApiClient;
import com.devicehive.rest.api.DeviceNotificationApi;
import com.devicehive.rest.auth.ApiKeyAuth;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.InsertNotification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainPresenter extends AbstractPresenter<NotificationView> implements ShakeDetector.Listener {
    private final ApiClient client;

    private DeviceNotificationApi deviceNotificationApi;

    private boolean enabled;
    private boolean executed = true;

    private String link;
    private Location location;
    private DataBaseHelper dbHelper = DataBaseHelper.getInstance();

    public MainPresenter() {
        client = RestHelper.getInstance().getApiClient();
        enabled = dbHelper.isEnabled();
    }


    public void prepare() {
        if (dbHelper.isDeviceRegistered()) {
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
        if (enabled && executed) {
            executed = false;
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
                                    view.onError();
                                }
                            }
                        }

                        executed = true;
                    }

                    @Override
                    public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable
                            t) {
                        Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                        if (!isViewNull()) {
                            view.onError();
                        }
                        executed = true;
                    }
                });
            }
        }

    }

    public void onLocationChanged(Location location) {
        this.location = location;
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
                                view.onError();
                            }
                        }
                    }
                    executed = true;
                }

                @Override
                public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable t) {
                    Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                    if (!isViewNull()) {
                        view.onError();
                    }
                    executed = true;
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

    @Override
    protected void onDestroyed() {
        clearLocationNotificationData();
    }

    public void logout() {
        dbHelper.clearDevice();
        dbHelper.clearUser();
    }
}


