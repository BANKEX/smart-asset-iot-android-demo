package com.bkx.lab.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.bkx.lab.R;
import com.bkx.lab.view.fragment.IntroSlideFragment;
import com.bkx.lab.view.fragment.IntroSlideFragmentLink;
import com.github.paolorotolo.appintro.AppIntro;

import butterknife.BindString;
import butterknife.ButterKnife;

public class IntroActivity extends AppIntro {

    @BindString(R.string.intro_first_slide_title)
    String firstTitle;
    @BindString(R.string.intro_first_slide_description)
    String firstDescription;
    @BindString(R.string.intro_second_slide_title)
    String secondTitle;
    @BindString(R.string.intro_second_slide_description)
    String secondDescription;


    public static void start(Context context) {
        Intent intent = new Intent(context, IntroActivity.class);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.NoActionBar);
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        addSlide(IntroSlideFragmentLink.newInstance());
        addSlide(IntroSlideFragment.newInstance(secondTitle, secondDescription));
        setSkipText(getString(R.string.skip));
        setDoneText(getString(R.string.done));
        setFadeAnimation();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        finish();
    }
}
