package com.bkx.lab.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

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
}