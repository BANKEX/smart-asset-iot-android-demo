package com.demo.bankexdh.presenter.impl;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.demo.bankexdh.model.ImageManager;
import com.demo.bankexdh.model.event.DeviceIdUpdateEvent;
import com.demo.bankexdh.model.rest.ImageNotificationData;
import com.demo.bankexdh.model.rest.RegisterBody;
import com.demo.bankexdh.model.rest.RegisterData;
import com.demo.bankexdh.model.rest.RestHelper;
import com.demo.bankexdh.model.rest.ShakeNotificationData;
import com.demo.bankexdh.model.rest.api.Register;
import com.demo.bankexdh.model.store.DataBaseHelper;
import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.NotificationView;
import com.demo.bankexdh.utils.Const;
import com.demo.bankexdh.utils.ImageUtils;
import com.demo.bankexdh.utils.ShakeDetector;
import com.devicehive.rest.ApiClient;
import com.devicehive.rest.api.DeviceNotificationApi;
import com.devicehive.rest.auth.ApiKeyAuth;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.InsertNotification;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.demo.bankexdh.utils.Const.DEVICE_NAME;

public class MainPresenter extends AbstractPresenter<NotificationView> implements ShakeDetector.Listener {
    private final ApiClient client;

    private DeviceNotificationApi deviceNotificationApi;

    private boolean enabled;
    private boolean executed = true;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

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
            register();
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

    private void register() {
        Register registerCall = client.createService(Register.class);
        RegisterBody body = getRegisterBody();
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
                    createServices();

                    EventBus.getDefault().post(DeviceIdUpdateEvent.newInstance());
                }
            }

            @Override
            public void onFailure(Call<RegisterData> call, Throwable t) {
                Timber.d(t.getMessage());
            }
        });
    }

    private void createServices() {
        deviceNotificationApi = client.createService(DeviceNotificationApi.class);
    }

    private RegisterBody getRegisterBody() {
        String uiid = UUID.randomUUID().toString();
        RegisterBody body = new RegisterBody();
        body.setId(uiid);
        String firstPart = uiid.substring(0, uiid.indexOf("-"));
        body.setName(String.format(DEVICE_NAME, firstPart));
        return body;
    }


    public void uploadFile(Context context) {
        Observable.just(context)
                .observeOn(Schedulers.newThread())
                .map((ctx) -> {
                    ImageUtils.getInstance().setOrientation();
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

    public boolean isEnabled() {
        return dbHelper.isEnabled();
    }

    public String getDeviceId() {
        return dbHelper.getDeviceId();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        dbHelper.setEnabled(enabled);
    }

    public File createImageFile(Context context) throws IOException {
        return ImageUtils.getInstance().createImageFile(context);
    }

    @Override
    public void hearShake(String timestamp) {
        if (enabled && executed) {
            executed = false;
            if (!TextUtils.isEmpty(dbHelper.getDeviceId())) {
                sendNotification(ShakeNotificationData.getNotification("Shaked"), new Callback<InsertNotification>() {
                    @Override
                    public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                        Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                        if (response.isSuccessful()) {
                            if (!isViewNull()) {
                                view.onShakeNotificationSent();
                            }
                            isRegistrationInProgress.set(false);
                        } else {
                            register(response.code());
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
    }

    private void register(int code) {
        if (!isViewNull()) {
            view.onError();
        }
        if (code == 401) {
            if (!isRegistrationInProgress.get()) {
                isRegistrationInProgress.set(true);
                register();
            }
        }
    }

    public void onLocationChanged(Location location) {
        this.location = location;
        sendLocationNotification(location, link);
    }

    private void sendLocationNotification(Location location, String link) {
        if (location != null && !TextUtils.isEmpty(link)) {
            DeviceNotificationWrapper wrapper = ImageNotificationData.getNotification(link, location.getLatitude(), location.getLongitude());
            sendNotification(wrapper, new Callback<InsertNotification>() {
                @Override
                public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                    Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                    if (response.isSuccessful()) {
                        if (!isViewNull()) {
                            view.onLocationNotificationSent();
                        }
                        clearLocationNotificationData();
                        isRegistrationInProgress.set(false);
                    } else {
                        register(response.code());
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


}


