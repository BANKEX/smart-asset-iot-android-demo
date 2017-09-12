package com.bkx.lab.model.parser;

import android.net.Uri;

import timber.log.Timber;

public class PathSegmentParser implements AssetIdParser {

    @Override
    public String parse(String contents) {
        try {
            String assetId = Uri.parse(Uri.parse(contents)
                    .getQueryParameter(LINK_PARAM))
                    .getLastPathSegment().trim();
            if (isNumeric(assetId)) {
                return assetId;
            }
        } catch (Exception e) {
            Timber.e(e, EXCEPTION_FORMAT, contents);
        }
        return null;
    }

    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
