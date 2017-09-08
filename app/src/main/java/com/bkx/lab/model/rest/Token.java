package com.bkx.lab.model.rest;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class Token {
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("refreshToken")
    private String refreshToken;

    @Override
    public String toString() {
        return "{\"Token\":{"
                + "\"accessToken\":\"" + accessToken + "\""
                + ", \"refreshToken\":\"" + refreshToken + "\""
                + "}}";
    }
}
