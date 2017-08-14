package com.demo.bankexdh.view.activity;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.demo.bankexdh.BuildConfig;
import com.demo.bankexdh.R;
import com.demo.bankexdh.model.event.DeviceIdUpdateEvent;
import com.demo.bankexdh.presenter.base.BasePresenterActivity;
import com.demo.bankexdh.presenter.base.NotificationView;
import com.demo.bankexdh.presenter.base.PresenterFactory;
import com.demo.bankexdh.presenter.impl.MainPresenter;
import com.demo.bankexdh.utils.ClientUtils;
import com.demo.bankexdh.utils.ShakeDetector;
import com.demo.bankexdh.utils.UIUtils;
import com.google.android.gms.location.FusedLocationProviderClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.RuntimePermissions;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

@RuntimePermissions
public class MainActivity extends BasePresenterActivity<MainPresenter, NotificationView> implements NotificationView {

    private MainPresenter presenter;
    private SensorManager mSensorManager;
    private ShakeDetector sd;
    @BindView(R.id.parentView)
    View parentView;
    @BindView(R.id.switcher)
    SwitchCompat switcher;
    @BindView(R.id.uniqueId)
    TextView deviceIdView;
    @BindView(R.id.camera)
    View camera;
    @BindView(R.id.animation_view)
    LottieAnimationView animationView;

    private static final int TAKE_PHOTO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        ButterKnife.bind(this);
        switcher.setOnCheckedChangeListener((compoundButton, b) -> {
            presenter.setEnabled(b);
            enableCameraFab(b);
            if (b) {
                presenter.prepare();
            }
        });
        prepareAnimation();
    }

    void prepareAnimation() {
        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

                animationView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                animationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                animationView.setVisibility(View.GONE);
            }
        });
        animationView.setVisibility(View.GONE);
    }

    @OnClick(R.id.switcher)
    void sendNotification() {
        presenter.setEnabled(switcher.isChecked());

    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sd.start(mSensorManager);
        setDeviceIdView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sd.stop();
    }

    @Override
    protected void setupActivityComponent() {

    }

    @NonNull
    @Override
    protected PresenterFactory<MainPresenter> getPresenterFactory() {
        return MainPresenter::new;
    }

    @Override
    protected void onPresenterPrepared(@NonNull MainPresenter presenter) {
        this.presenter = presenter;
        switcher.setChecked(presenter.isEnabled());
        enableCameraFab(presenter.isEnabled());
        presenter.prepare();
        sd = new ShakeDetector(presenter);
        setDeviceIdView();
    }

    @Override
    public void onNotificationSent() {
        if (animationView.isAnimating()) {
            animationView.cancelAnimation();
        }
        animationView.setAnimation("done_button.json");
        animationView.playAnimation();
        vibrate(500);
    }


    @OnClick(R.id.camera)
    void takeAPhotoCheck() {
        MainActivityPermissionsDispatcher.takeAPhotoWithCheck(MainActivity.this);
    }

    @NeedsPermission({Manifest.permission.CAMERA,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void takeAPhoto() {
        if (isLocationDisabled()) {
            Snackbar snackbar = Snackbar.make(parentView, "Location is disabled",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(getString(android.R.string.ok),
                    v -> snackbar.dismiss());
            snackbar.show();
            onError();
            return;
        }
        getLastLocation();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            Observable.just(this).map((ctx) -> presenter.createImageFile(this))
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(photoFile -> {
                        if (photoFile != null) {
                            try {
                                Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID
                                        + ".provider", presenter.createImageFile(this));
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                startActivityForResult(takePictureIntent, TAKE_PHOTO);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, t -> onError());
        }
    }

    public void getLastLocation() {
        // Get last known recent location using new Google Play Services SDK (v11+)
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    // GPS location can be null if GPS is switched off
                    if (location != null) {
                        presenter.onLocationChanged(location);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }


    @OnNeverAskAgain({Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void showNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_camera_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                }).setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss()).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO) {
                try {
                    if (!ClientUtils.isNetworkConnected(this)) {
                        UIUtils.showInternetConnectionAlertDialog(this);
                        return;
                    }
                    presenter.setOrientation();
                    String imageUri = presenter.getCurrentPhotoPath();
                    presenter.uploadFile(Uri.parse(imageUri), this);
                } catch (Exception e) {
                    e.printStackTrace();
                    onPhotoUploadFail("");
                }
            }
        }
    }

    private boolean isLocationDisabled() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;
        boolean network_enabled;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            return false;
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            return false;
        }

        return !gps_enabled && !network_enabled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onError() {
        if (animationView.isAnimating()) {
            animationView.cancelAnimation();
        }
        animationView.setAnimation("x_pop.json");
        animationView.playAnimation();
        vibrate(500);
    }

    private void vibrate(long millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (v != null) {
                v.vibrate(millis);
            }
        } else {
            VibrationEffect effect =
                    VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE);
            v.vibrate(effect);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateDeviceId(DeviceIdUpdateEvent event) {
        setDeviceIdView();
    }

    private void setDeviceIdView() {
        String deviceId = presenter.getDeviceId();
        deviceIdView.setVisibility(TextUtils.isEmpty(deviceId) ? View.INVISIBLE : View.VISIBLE);
        deviceIdView.setText(String.format("ID: %s", deviceId));
    }

    public void onPhotoUploadFail(@Nullable String message) {
        Snackbar snackbar = Snackbar.make(parentView, "Image uploading failed", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getString(android.R.string.ok),
                v -> snackbar.dismiss());

        snackbar.show();
    }

    private void enableCameraFab(boolean b) {
        camera.setVisibility(b ? View.VISIBLE : View.GONE);
    }

}
