package com.demo.bankexdh.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.demo.bankexdh.R;
import com.demo.bankexdh.presenter.base.BasePresenterFragment;
import com.demo.bankexdh.presenter.base.PresenterFactory;
import com.demo.bankexdh.presenter.base.RegistrationView;
import com.demo.bankexdh.presenter.impl.RegistrationPresenter;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegistrationFragment
        extends BasePresenterFragment<RegistrationPresenter, RegistrationView>
        implements RegistrationView {

    @BindView(R.id.asset_id_edit)
    EditText assetIdEdit;
    @BindView(R.id.scan_button)
    View scanButton;

    private RegistrationPresenter presenter;

    private boolean scannedContentReceived;
    private String scannedContent;

    @NonNull
    @Override
    protected PresenterFactory<RegistrationPresenter> getPresenterFactory() {
        return RegistrationPresenter::new;
    }

    @Override
    protected void onPresenterPrepared(@NonNull RegistrationPresenter presenter) {
        this.presenter = presenter;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scanButton.setOnClickListener(button ->
                IntentIntegrator.forSupportFragment(this).initiateScan());
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
    public void showAssetId(String assetId) {
        assetIdEdit.setText(assetId);
    }

    @Override
    public void showGetAssetIdError() {
        Toast.makeText(getActivity(), R.string.error_get_asset_id, Toast.LENGTH_SHORT).show();
    }
}
