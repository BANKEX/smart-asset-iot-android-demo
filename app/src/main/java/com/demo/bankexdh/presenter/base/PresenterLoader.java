package com.demo.bankexdh.presenter.base;

import android.content.Context;
import android.support.v4.content.Loader;

public class PresenterLoader<T extends AbstractPresenter> extends Loader<T> {

    private final PresenterFactory<T> factory;

    private T presenter;

    public PresenterLoader(Context context, PresenterFactory<T> factory) {
        super(context);
        this.factory = factory;
    }


    @Override
    protected void onStartLoading() {

        // If we already own an instance, simply deliver it.
        if (presenter != null) {
            deliverResult(presenter);
            return;
        }

        // Otherwise, force a load
        forceLoad();
    }

    @Override
    protected void onForceLoad() {
        // Create the AbstractPresenter using the Factory
        presenter = factory.create();

        // Deliver the result
        deliverResult(presenter);
    }

    @Override
    protected void onReset() {
        presenter.onDestroyed();
        presenter = null;
    }
}