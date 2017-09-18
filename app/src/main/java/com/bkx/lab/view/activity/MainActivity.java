package com.bkx.lab.view.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.support.v4.content.ContextCompat;
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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bkx.lab.BuildConfig;
import com.bkx.lab.R;
import com.bkx.lab.model.LocationEntity;
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
import com.google.android.gms.location.LocationServices;
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

@RuntimePermissions
public class MainActivity extends BasePresenterActivity<MainPresenter, NotificationView> implements NotificationView {

    private MainPresenter presenter;
    private SensorManager sensorManager;
    private ShakeDetector sd;
    private FusedLocationProviderClient locationClient;

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
    @BindView(R.id.coordinates)
    TextView coordinates;
    @BindView(R.id.shakeLayout)
    LinearLayout shakeLayout;
    @BindView(R.id.shakeTitleLayout)
    RelativeLayout shakeTitleLayout;
    @BindView(R.id.photoLayout)
    LinearLayout photoLayout;
    @BindView(R.id.photoTitleLayout)
    RelativeLayout photoTitleLayout;
    @BindView(R.id.animationShake)
    ImageView animationShake;
    @BindView(R.id.animationLocation)
    ImageView animationLocation;
    @BindView(R.id.animationShakeError)
    ImageView animationShakeError;
    @BindView(R.id.animationLocationError)
    ImageView animationLocationError;

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
        assetIdEdit.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                handled = true;
            }
            return handled;
        });
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
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
            sd.start(sensorManager);
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
        UIUtils.hideKeyboard(this);
        playAnimation(animationShake);
        showSnackBar(getString(R.string.shake_success));
    }

    @Override
    public void onLocationNotificationSent(LocationEntity location) {
        UIUtils.hideKeyboard(this);
        coordinates.setVisibility(View.VISIBLE);
        coordinates.setText(String.format(getString(R.string.location_format),
                location.getLatitude(),
                location.getLongitude()));
        photoProgressBar.setVisibility(View.INVISIBLE);
        playAnimation(animationLocation);
        showSnackBar(String.format(getString(R.string.photo_success), presenter.getDeviceId()));

    }

    private void showSnackBar(String text) {
        Snackbar snackbar = Snackbar.make(buttonContainer,
                text,
                Snackbar.LENGTH_INDEFINITE);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_color));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.white));
        TextView textView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(10);
        snackbar.setAction(getString(R.string.dismiss), view1 -> snackbar.dismiss());
        snackbar.show();
    }

    @Override
    public void onLocationError() {
        coordinates.setVisibility(View.GONE);
        progressBar.setVisibility(View.INVISIBLE);
        photoProgressBar.setVisibility(View.INVISIBLE);
        playAnimation(animationLocationError);
    }

    @Override
    public void onShakeError() {
        shakeProgressBar.setVisibility(View.INVISIBLE);
        playAnimation(animationShakeError);
    }

    @Override
    public void onError() {
        Snackbar snackbar = Snackbar.make(buttonContainer, R.string.something_went_wrong, Snackbar.LENGTH_SHORT);
        snackbar.show();
        coordinates.setVisibility(View.GONE);
        progressBar.setVisibility(View.INVISIBLE);
        photoProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUnregistered() {
        presenter.clear();
        enableAllViews(false, shakeLayout, shakeTitleLayout, photoLayout, photoTitleLayout);
    }

    @Override
    public void registerOnScan() {
        submit();
    }

    private void playAnimation(View animationView) {
        animationView.clearAnimation();
        animationView.setVisibility(View.VISIBLE);
        animationView.setAlpha(0.0f);
        animationView.animate()
                .alpha(1f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animationView.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        animationView.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
        vibrate(500);
    }

    @OnClick(R.id.photo_button)
    void takeAPhotoCheck() {
        coordinates.setVisibility(View.GONE);
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
            showLoading(true);
            enableAllViews(false, shakeLayout, shakeTitleLayout, photoLayout, photoTitleLayout);
        } else {
            assetIdEditContainer.setError(getString(R.string.error_get_asset_id));
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
                    }, t -> onLocationError());
        }
    }

    public void getLastLocation() {
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
                .addOnFailureListener(error -> Timber.e("Failed to get location"));
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
        } else {
            enableAllViews(false, shakeLayout, shakeTitleLayout, photoLayout, photoTitleLayout);
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
        Snackbar.make(buttonContainer, R.string.register_success, Snackbar.LENGTH_SHORT)
                .show();
        showLoading(false);
        enableAllViews(true, shakeLayout, shakeTitleLayout, photoLayout, photoTitleLayout);
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
                view -> snackbar.dismiss());

        snackbar.show();
    }

    private void enableAllViews(boolean enable, ViewGroup... viewGroups) {
        for (ViewGroup viewGroup : viewGroups) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                child.setEnabled(enable);
            }
        }
    }
}
