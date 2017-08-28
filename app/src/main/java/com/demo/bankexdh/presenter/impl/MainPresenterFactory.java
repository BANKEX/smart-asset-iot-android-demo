package com.demo.bankexdh.presenter.impl;

import com.demo.bankexdh.model.prefs.PreferencesRepository;
import com.demo.bankexdh.presenter.base.PresenterFactory;

public class MainPresenterFactory implements PresenterFactory<MainPresenter> {

    private final PreferencesRepository preferencesRepository;

    public MainPresenterFactory(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    @Override
    public MainPresenter create() {
        return new MainPresenter(preferencesRepository);
    }

}
