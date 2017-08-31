package com.demo.bankexdh.view.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.demo.bankexdh.R;
import com.demo.bankexdh.model.prefs.PreferencesRepository;
import com.demo.bankexdh.presenter.base.BasePresenterActivity;
import com.demo.bankexdh.presenter.base.PresenterFactory;
import com.demo.bankexdh.presenter.base.RegistrationView;
import com.demo.bankexdh.presenter.impl.RegistrationPresenter;
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
    @BindView(R.id.asset_id_edit)
    EditText assetIdEdit;
    private RegistrationPresenter presenter;

    private boolean scannedContentReceived;
    private String scannedContent;

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
}
