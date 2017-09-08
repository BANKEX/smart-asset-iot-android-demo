package com.bkx.lab.presenter.base;

public interface PresenterFactory<T extends AbstractPresenter> {


    T create();
}