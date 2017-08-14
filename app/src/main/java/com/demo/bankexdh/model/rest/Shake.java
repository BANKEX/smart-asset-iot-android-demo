package com.demo.bankexdh.model.rest;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

@Data
public class Shake {
    @SerializedName("shake")
    private String shake;
    @SerializedName("shakenAt")
    private String shakenAt;
    @SerializedName("fakeData")
    private List<Long> fakeData;
}
