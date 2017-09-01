package com.demo.bankexdh.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.demo.bankexdh.R;
import com.demo.bankexdh.utils.UIUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class IntroSlideFragmentLink extends Fragment {

    public static IntroSlideFragmentLink newInstance() {
        return new IntroSlideFragmentLink();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @OnClick(R.id.link)
    void open(){
        UIUtils.openInBrowser(getContext(),R.string.about_info_link);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first_intro_slide, container, false);
        ButterKnife.bind(this, view);
        return view;
    }
}
