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
import com.demo.bankexdh.model.store.DeviceModel;
import com.demo.bankexdh.model.store.UserModel;
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
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainPresenter extends AbstractPresenter<NotificationView> implements ShakeDetector.Listener {

    private static final String ACCELEROMETER = "ACCELEROMETER";

    private final ApiClient client;

    private DeviceNotificationApi deviceNotificationApi;
    private Call<InsertNotification> notificationCallInsert;

    private boolean enabled;
    private boolean executed = true;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private String link;
    private Location location;

    public MainPresenter() {
        client = RestHelper.getInstance().getApiClient();
        enabled = isEnabled();
    }

    public void prepare() {
        if (isDeviceRegistered()) {
            setupPresenter();
        } else {
            register();
        }

    }

    private void setupPresenter() {
        String token = getToken();
        addAuth(token);
        createServices();
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

                    insertUserModel(data);
                    insertDevice(data);
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

    private RegisterBody getRegisterBody() {
        String uiid = UUID.randomUUID().toString();
        RegisterBody body = new RegisterBody();
        body.setKey(null);
        body.setId(uiid);
        body.setName(Const.DEVICE_NAME);
        return body;
    }


    private void createServices() {
        deviceNotificationApi = client.createService(DeviceNotificationApi.class);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            if (model != null) {
                realm.executeTransaction(t -> {
                    model.setIsEnabled(enabled);
                    t.copyToRealmOrUpdate(model);
                });
            }
        }
    }

    public boolean isEnabled() {
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            if (model != null) {
                return model.getIsEnabled();
            }
        }
        return false;
    }

    private void addAuth(String accessToken) {
        client.addAuthorization(ApiClient.AUTH_API_KEY,
                ApiKeyAuth.newInstance(accessToken));
    }

    private void insertUserModel(RegisterData registerData) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(t -> {
                UserModel model = new UserModel();
                model.setId(UserModel.DEFAULT_ID);
                model.setAccessToken(registerData.getToken().getAccessToken());
                model.setRefreshToken(registerData.getToken().getRefreshToken());
                model.setIsEnabled(enabled);
                t.copyToRealmOrUpdate(model);
            });
        }
    }


    private void insertDevice(RegisterData registerData) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(t -> {
                DeviceModel newModel = new DeviceModel();
                newModel.setId(DeviceModel.DEFAULT_ID);
                newModel.setDeviceId(registerData.getDeviceId());
                newModel.setName(ACCELEROMETER);
                t.copyToRealmOrUpdate(newModel);
            });
        }
    }

    private boolean isDeviceRegistered() {
        try (Realm realm = Realm.getDefaultInstance()) {
            DeviceModel model = realm.where(DeviceModel.class)
                    .equalTo(DeviceModel.ID, DeviceModel.DEFAULT_ID).findFirst();
            return model != null;
        }
    }

    private String getToken() {
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            return model.getAccessToken();
        }
    }

    public String getDeviceId() {
        try (Realm realm = Realm.getDefaultInstance()) {
            DeviceModel model = realm.where(DeviceModel.class)
                    .equalTo(DeviceModel.ID,
                            DeviceModel.DEFAULT_ID).findFirst();
            if (model == null) {
                return null;
            } else {
                return model.getDeviceId();
            }
        }
    }
    public void uploadFile(Uri filePath, Context context) {
        Timber.d(filePath + "");
        Observable.just(context)
                .observeOn(Schedulers.io())
                .map((ctx) -> {
                    InputStream imageStream = ctx.getContentResolver().openInputStream(filePath);
                    int imageLength = imageStream.available();
                    String imageUrl = ImageManager.getInstance()
                            .uploadImage(filePath.getLastPathSegment(), imageStream, imageLength);
                    return imageUrl;
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe((s) -> {
                    this.link = s;
                    sendLocationNotification(location, s);
                    Timber.d(s);
                }, Throwable::printStackTrace);

    }

    public File createImageFile(Context context) throws IOException {
        return ImageUtils.getInstance().createImageFile(context);
    }

    public void setOrientation() throws IOException {
        ImageUtils.getInstance().setOrientation();
    }

    public String getCurrentPhotoPath() {
        return ImageUtils.getInstance().getCurrentPhotoPath();
    }

    @Override
    public void hearShake(String timestamp) {
        if (enabled && executed) {
            executed = false;
            if (!TextUtils.isEmpty(getDeviceId())) {
                insertNotification(ShakeNotificationData.getNotification("Shaked"));
            }
        }
    }

    public void onLocationChanged(Location location) {
        this.location = location;
        sendLocationNotification(location, link);
    }

    private void sendLocationNotification(Location location, String imageUrl) {
        if (location != null && !TextUtils.isEmpty(imageUrl)) {
            DeviceNotificationWrapper wrapper = ImageNotificationData.getNotification(link, location.getLatitude(), location.getLongitude());
            insertNotification(wrapper);
        }
    }

    private void insertNotification(DeviceNotificationWrapper notificationWrapper) {
        notificationCallInsert = deviceNotificationApi.insert(getDeviceId(), notificationWrapper);
        notificationCallInsert.enqueue(new Callback<InsertNotification>() {
            @Override
            public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                if (response.isSuccessful()) {
                    if (!isViewNull()) {
                        view.onNotificationSent();
                    }
                    link = null;
                    isRegistrationInProgress.set(false);
                } else {
                    if (!isViewNull()) view.onError();
                    if (response.code() == 401) {
                        if (!isRegistrationInProgress.get()) {
                            isRegistrationInProgress.set(true);
                            register();
                        }
                    }
                }
                executed = true;
            }

            @Override
            public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable t) {
                Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                if (!isViewNull()) view.onError();
                executed = true;
            }
        });
    }

    @Override
    protected void onDestroyed() {
    }
}
