package com.bkx.lab.view.activity;

import android.Manifest;
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
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.airbnb.lottie.LottieAnimationView;
import com.bkx.lab.BuildConfig;
import com.bkx.lab.R;
import com.bkx.lab.model.prefs.PreferencesRepository;
import com.bkx.lab.model.store.DataBaseHelper;
import com.bkx.lab.presenter.base.BasePresenterActivity;
import com.bkx.lab.presenter.base.NotificationView;
import com.bkx.lab.presenter.base.PresenterFactory;
import com.bkx.lab.presenter.impl.MainPresenter;
import com.bkx.lab.utils.ClientUtils;
import com.bkx.lab.utils.ShakeDetector;
import com.bkx.lab.utils.UIUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

@RuntimePermissions
public class MainActivity extends BasePresenterActivity<MainPresenter, NotificationView> implements NotificationView {

    private MainPresenter presenter;
    private SensorManager mSensorManager;
    private ShakeDetector sd;
    @BindView(R.id.buttonContainer)
    View buttonContainer;
    @BindView(R.id.shake_button)
    View shakeButton;
    @BindView(R.id.photo_button)
    View photoButton;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.asset_id_edit_container)
    TextInputLayout assetIdEditContainer;
    @BindView(R.id.asset_id_edit)
    EditText assetIdEdit;
    @BindView(R.id.progress_bar_view)
    ProgressBar progressBar;
    @BindView(R.id.photoProgressBar)
    ProgressBar photoProgressBar;
    @BindView(R.id.shakeProgressBar)
    ProgressBar shakeProgressBar;
    @BindView(R.id.submit)
    View submit;
    @BindView(R.id.scanCode)
    View scanButton;

    private boolean scannedContentReceived;
    private String scannedContent;

    private static final int TAKE_PHOTO = 2;

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            assetIdEditContainer.setError(null);
            assetIdEditContainer.setErrorEnabled(false);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            assetIdEditContainer.setError(null);
            assetIdEditContainer.setErrorEnabled(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        assetIdEdit.addTextChangedListener(watcher);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                AboutActivity.start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (sd != null) {
            sd.start(mSensorManager);
        }
        showLoading(DataBaseHelper.getInstance().isDeviceRegistered() && presenter.isRegistration());
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    Uri deepLink = null;
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.getLink();
                    }
                    if (deepLink != null) {
                        presenter.onScanCompleted(deepLink.toString());
                    }
                })
                .addOnFailureListener(this, e -> Log.w("TAG", "getDynamicLink:onFailure", e));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sd != null) {
            sd.stop();
        }
    }

    @Override
    protected void setupActivityComponent() {

    }

    @NonNull
    @Override
    protected PresenterFactory<MainPresenter> getPresenterFactory() {
        return () -> new MainPresenter(new PreferencesRepository(MainActivity.this));
    }

    @Override
    protected void onPresenterPrepared(@NonNull MainPresenter presenter) {
        this.presenter = presenter;
        presenter.prepare();
        sd = new ShakeDetector(presenter);
    }

    @OnClick(R.id.shake_button)
    void onShakeButtonClick() {
        presenter.onShake();
    }

    @Override
    public void onShakeNotificationSent() {
        vibrate(500);
    }

    @Override
    public void onLocationNotificationSent() {
        photoProgressBar.setVisibility(View.INVISIBLE);
        vibrate(500);

    }

    @Override
    public void onError() {
        progressBar.setVisibility(View.INVISIBLE);
        photoProgressBar.setVisibility(View.INVISIBLE);
        shakeProgressBar.setVisibility(View.INVISIBLE);
        vibrate(500);
    }

    @Override
    public void onUnregistered() {

    }

    private void playAnimation(LottieAnimationView animationView) {
        animationView.playAnimation();
        vibrate(500);
    }

    @OnClick(R.id.photo_button)
    void takeAPhotoCheck() {
        MainActivityPermissionsDispatcher.takeAPhotoWithCheck(MainActivity.this);
    }

    @OnClick(R.id.scanCode)
    void initiateScan() {
        MainActivityPermissionsDispatcher.startScanWithCheck(this);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    public void startScan() {
        new IntentIntegrator(this)
                .setPrompt(getString(R.string.scan_barcode_prompt))
                .initiateScan();
    }

    @OnClick(R.id.submit)
    void submit() {
        UIUtils.hideKeyboard(this);
        if (!ClientUtils.isNetworkConnected(this)) {
            UIUtils.showInternetConnectionAlertDialog(this);
            return;
        }
        String value = assetIdEdit.getText().toString();
        if (TextUtils.isEmpty(value)) {
            assetIdEditContainer.setErrorEnabled(true);
            assetIdEditContainer.setError(getString(R.string.asset_id_empty));
            return;
        }
        if (presenter.validate(value)) {
            presenter.register(value);
        } else {
            Snackbar snackbar = Snackbar.make(buttonContainer, R.string.error_incorrect_asset_id, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

    }


    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void takeAPhoto() {
        if (isLocationDisabled()) {
            Snackbar snackbar = Snackbar.make(buttonContainer, R.string.location_disabled_message,
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
            Observable.just(this)
                    .observeOn(Schedulers.newThread())
                    .map((ctx) -> presenter.createImageFile(this))
                    .subscribe(photoFile -> {
                        Timber.d(Thread.currentThread().getName());
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID
                                    + ".provider", presenter.createImageFile(this));
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(takePictureIntent, TAKE_PHOTO);
                        }
                    }, t -> onError());
        }
    }

    public void getLastLocation() {
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        presenter.onLocationChanged(location);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }


    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void showNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                }).setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss()).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                if (!ClientUtils.isNetworkConnected(this)) {
                    UIUtils.showInternetConnectionAlertDialog(this);
                    return;
                }
                photoProgressBar.setVisibility(View.VISIBLE);
                presenter.uploadFile(this);
            } catch (Exception e) {
                e.printStackTrace();
                onPhotoUploadFail("");
            }
            return;
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (result != null) {
                scannedContentReceived = true;
                scannedContent = result.getContents();
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
    public void onStart() {
        super.onStart();
        if (DataBaseHelper.getInstance().isDeviceRegistered()) {
            showAssetId(presenter.getDeviceId());
            assetIdEdit.clearFocus();
        }
        if (!scannedContentReceived) {
            return;
        }
        scannedContentReceived = false;

        if (scannedContent != null) {
            presenter.onScanCompleted(scannedContent);
        } else {
            presenter.onScanCancelled();
        }
        scannedContent = null;
    }

    @Override
    public void showIntro() {
        IntroActivity.start(this);
    }

    @Override
    public void onRegistered() {
        showLoading(false);
    }

    @Override
    public void onRegistrationError() {
        showLoading(false);
        Snackbar.make(buttonContainer, R.string.something_went_wrong, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void showAssetId(String assetId) {
        assetIdEdit.setText(assetId);
        assetIdEdit.setSelection(assetId.length());
    }

    @Override
    public void showGetAssetIdError() {
        Snackbar.make(buttonContainer, R.string.error_get_asset_id, Snackbar.LENGTH_SHORT)
                .show();
    }

    void showLoading(boolean show) {
        enableViews(!show);
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    void enableViews(boolean enable) {
        assetIdEdit.setEnabled(enable);
        submit.setEnabled(enable);
        scanButton.setEnabled(enable);
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

    public void onPhotoUploadFail(@Nullable String message) {
        Snackbar snackbar = Snackbar.make(buttonContainer, "Image uploading failed", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getString(android.R.string.ok),
                v -> snackbar.dismiss());

        snackbar.show();
    }
}
