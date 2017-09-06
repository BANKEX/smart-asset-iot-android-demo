package com.bkx.lab.presenter.base;

public abstract class AbstractPresenter<V extends MvpView> {

    public V view;

    public void onViewAttached(V view) {
        this.view = view;
    }

    public void onViewDetached() {
        this.view = null;
    }

    protected abstract void onDestroyed();

    public boolean isViewNull() {
        return view == null;
    }
}