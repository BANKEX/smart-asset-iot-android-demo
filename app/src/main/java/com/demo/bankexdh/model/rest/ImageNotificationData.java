package com.demo.bankexdh.model.rest;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ImageNotificationData {
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;
}
