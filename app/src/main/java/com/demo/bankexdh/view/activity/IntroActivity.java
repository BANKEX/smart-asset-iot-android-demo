package com.demo.bankexdh.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.demo.bankexdh.R;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;

public class IntroActivity extends AppIntro2 {

    @BindColor(R.color.introSlideBackground) int bgColor;
    @BindString(R.string.intro_slide_description) String description;
    @BindString(R.string.intro_slide_title) String title;
    int imageRes = R.drawable.slide;

    public static void start(Context context) {
        Intent intent = new Intent(context, IntroActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        SliderPage firstPage = new SliderPage();
        firstPage.setBgColor(bgColor);
        firstPage.setDescription(description);
        firstPage.setTitle(title);
        firstPage.setImageDrawable(imageRes);

        SliderPage secondPage = new SliderPage();
        secondPage.setBgColor(bgColor);
        secondPage.setDescription(description);
        secondPage.setTitle(title);
        secondPage.setImageDrawable(imageRes);

        addSlide(AppIntroFragment.newInstance(firstPage));
        addSlide(AppIntroFragment.newInstance(secondPage));

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
