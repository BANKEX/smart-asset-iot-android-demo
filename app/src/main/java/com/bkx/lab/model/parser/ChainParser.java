package com.bkx.lab.model.parser;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class ChainParser {


    private static class InstanceHolder {
        static final ChainParser INSTANCE = new ChainParser();
    }

    public static ChainParser getInstance() {
        return ChainParser.InstanceHolder.INSTANCE;
    }

    private List<AssetIdParser> parsers = new ArrayList<>();

    private ChainParser() {
        parsers.clear();
        parsers.add( new PathSegmentParser());
        parsers.add( new LinkAndAssetIdParamsParser());
        parsers.add( new AssetIdParamParser());
    }

    public String parseAssetId(String content) {
        String assetId;
        for (int i = 0; i < parsers.size(); i++) {
            assetId = parsers.get(i).parse(content);
            if (!TextUtils.isEmpty(assetId)) {
                return assetId;
            }
        }
        return null;
    }
}
