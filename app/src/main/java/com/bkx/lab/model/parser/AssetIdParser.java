package com.bkx.lab.model.parser;

public interface AssetIdParser {
    String LINK_PARAM = "link";
    String ASSET_ID_QUERY_PARAMETER = "id";
    String EXCEPTION_FORMAT = "Failed to parse scanned content: %s";

    String parse(String contents);
}
