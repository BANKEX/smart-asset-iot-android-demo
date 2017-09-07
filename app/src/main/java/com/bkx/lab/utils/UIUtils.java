package com.bkx.lab.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.bkx.lab.R;


public class UIUtils {


    private UIUtils() {
        throw new UnsupportedOperationException("Do not instantiate");
    }

    public static void showInternetConnectionAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_title));
        builder.setMessage(context.getString(R.string.dialog_message));

        String positiveText = context.getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                (dialog, which) -> {
                    // positive button logic
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();
    }

    public static void openInBrowser(Context context,
                                     @StringRes int urlResId) {
        context.startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(context.getString(urlResId))
        ));
    }
    public static void hideKeyboard(Activity a) {
        hideKeyboard(a.getCurrentFocus());
    }

    public static void hideKeyboard(View v) {
        if (v == null) {
            return;
        }

        View focus = v.findFocus();
        if (focus != null) {
            Context c = v.getContext();
            InputMethodManager imm =
                    (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);

            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

}