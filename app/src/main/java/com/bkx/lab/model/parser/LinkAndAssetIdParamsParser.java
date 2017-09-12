package com.bkx.lab.model.parser;

import android.net.Uri;

import timber.log.Timber;

public class LinkAndAssetIdParamsParser implements AssetIdParser {

    @Override
    public String parse(String contents) {
        try {
            return Uri.parse(Uri.parse(contents)
                    .getQueryParameter(LINK_PARAM))
                    .getQueryParameter(ASSET_ID_QUERY_PARAMETER);
        } catch (Exception e) {
            Timber.e(e, EXCEPTION_FORMAT, contents);
        }
        return null;
    }
}
