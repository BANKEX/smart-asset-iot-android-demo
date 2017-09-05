package com.demo.bankexdh.view.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.demo.bankexdh.R;
import com.demo.bankexdh.model.prefs.PreferencesRepository;
import com.demo.bankexdh.model.store.DataBaseHelper;
import com.demo.bankexdh.presenter.base.BasePresenterActivity;
import com.demo.bankexdh.presenter.base.PresenterFactory;
import com.demo.bankexdh.presenter.base.RegistrationView;
import com.demo.bankexdh.presenter.impl.RegistrationPresenter;
import com.demo.bankexdh.utils.ClientUtils;
import com.demo.bankexdh.utils.UIUtils;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class RegistrationActivity extends BasePresenterActivity<RegistrationPresenter, RegistrationView>
        implements RegistrationView {

    @BindView(R.id.parent_container)
    View parentContainer;
    @BindView(R.id.asset_id_edit_container)
    TextInputLayout assetIdEditContainer;
    @BindView(R.id.asset_id_edit)
    EditText assetIdEdit;
    @BindView(R.id.registrationProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.register_button)
    View registrationButton;
    @BindView(R.id.scan_button)
    View scanButton;
    private RegistrationPresenter presenter;

    private boolean scannedContentReceived;
    private String scannedContent;

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

    @NonNull
    @Override
    protected PresenterFactory<RegistrationPresenter> getPresenterFactory() {
        return () -> new RegistrationPresenter(new PreferencesRepository(RegistrationActivity.this));
    }

    @Override
    protected void onPresenterPrepared(@NonNull RegistrationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        ButterKnife.bind(this);
        assetIdEdit.addTextChangedListener(watcher);
    }

    @OnClick(R.id.scan_button)
    void initiateScan() {
        RegistrationActivityPermissionsDispatcher.startScanWithCheck(this);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    public void startScan() {
        new IntentIntegrator(this)
                .setPrompt(getString(R.string.scan_barcode_prompt))
                .initiateScan();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void showNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                }).setNegativeButton(getString(android.R.string.cancel),
                (dialog, which) -> dialog.dismiss()).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RegistrationActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnClick(R.id.register_button)
    void register() {
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
            showLoading(true);
            presenter.register(value);
        } else {
            Snackbar snackbar = Snackbar.make(parentContainer, R.string.error_incorrect_asset_id, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            scannedContentReceived = true;
            scannedContent = result.getContents();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DataBaseHelper.getInstance().isDeviceRegistered()) {
            onRegistered();
            return;
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
    protected void onResume() {
        super.onResume();
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
    public void showIntro() {
        IntroActivity.start(this);
    }

    @Override
    public void onRegistered() {
        showLoading(false);
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void onRegistrationError() {
        showLoading(false);
        Snackbar.make(parentContainer, R.string.something_went_wrong, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void showAssetId(String assetId) {
        assetIdEdit.setText(assetId);
        assetIdEdit.setSelection(assetId.length());
    }

    @Override
    public void showGetAssetIdError() {
        Snackbar.make(parentContainer, R.string.error_get_asset_id, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void setupActivityComponent() {

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
        registrationButton.setEnabled(enable);
        scanButton.setEnabled(enable);
    }
}
