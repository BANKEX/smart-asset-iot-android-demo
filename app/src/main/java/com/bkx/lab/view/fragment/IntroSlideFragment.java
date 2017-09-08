package com.bkx.lab.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bkx.lab.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IntroSlideFragment extends Fragment {

    protected static final String ARG_TITLE = "title";
    protected static final String ARG_DESCRIPTION = "description";

    @BindView(R.id.title_text)
    TextView titleText;
    @BindView(R.id.description_text)
    TextView descriptionText;

    private String title;
    private String description;


    public static IntroSlideFragment newInstance(String title) {
        return newInstance(title, null);
    }

    public static IntroSlideFragment newInstance(String title, String description) {
        IntroSlideFragment fragment = new IntroSlideFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && !getArguments().isEmpty()) {
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESCRIPTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_slide, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleText.setText(title);

        if (!TextUtils.isEmpty(description)) {
            descriptionText.setText(description);
        } else {
            descriptionText.setVisibility(View.GONE);
        }
    }
}
