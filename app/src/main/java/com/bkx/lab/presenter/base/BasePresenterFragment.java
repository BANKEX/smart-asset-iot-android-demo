package com.bkx.lab.presenter.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

public abstract class BasePresenterFragment<P extends AbstractPresenter<V>, V extends MvpView> extends Fragment {

    private static final int LOADER_ID = 101;

    private AbstractPresenter<V> presenter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // LoaderCallbacks as an object, so no hint regarding loader will be leak to the subclasses.
        getLoaderManager().initLoader(loaderId(), null, new LoaderManager.LoaderCallbacks<P>() {
            @Override
            public final Loader<P> onCreateLoader(int id, Bundle args) {
                return new PresenterLoader<>(getContext(), getPresenterFactory());
            }

            @Override
            public final void onLoadFinished(Loader<P> loader, P presenter) {
                BasePresenterFragment.this.presenter = presenter;
                onPresenterPrepared(presenter);
            }

            @Override
            public final void onLoaderReset(Loader<P> loader) {
                BasePresenterFragment.this.presenter = null;
                onPresenterDestroyed();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        presenter.onViewAttached(getPresenterView());
    }

    @Override
    public void onStop() {
        presenter.onViewDetached();

        super.onStop();
    }

    /**
     * Instance of {@link PresenterFactory} use to create a AbstractPresenter when needed. This instance should
     * not contain {@link android.app.Activity} context reference since it will be keep on rotations.
     */
    @NonNull
    protected abstract PresenterFactory<P> getPresenterFactory();

    /**
     * Hook for subclasses that deliver the {@link AbstractPresenter} before its View is attached.
     * Can be use to initialize the AbstractPresenter or simple hold a reference to it.
     */
    protected abstract void onPresenterPrepared(@NonNull P presenter);

    /**
     * Hook for subclasses before the screen gets destroyed.
     */
    protected void onPresenterDestroyed() {
    }

    /**
     * Override in case of fragment not implementing AbstractPresenter<View> interface
     */
    @NonNull
    protected V getPresenterView() {
        return (V) this;
    }

    /**
     * Use this method in case you want to specify a specific ID for the {@link PresenterLoader}.
     * By default its value would be {@link #LOADER_ID}.
     */
    protected int loaderId() {
        return LOADER_ID;
    }
}
