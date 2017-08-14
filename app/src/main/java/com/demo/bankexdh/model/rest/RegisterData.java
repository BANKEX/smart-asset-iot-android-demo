package com.demo.bankexdh.model.rest;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class RegisterData {


    @SerializedName("token")
    private Token token;
    @SerializedName("deviceId")
    private String deviceId;

    @Override
    public String toString() {
        return "{\"RegisterData\":{"
                + "\"token\":" + token
                + ", \"deviceId\":\"" + deviceId + "\""
                + "}}";
    }
}
