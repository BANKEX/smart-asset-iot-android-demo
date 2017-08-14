package com.demo.bankexdh.presenter.base;

public interface PresenterFactory<T extends AbstractPresenter> {


    T create();
}