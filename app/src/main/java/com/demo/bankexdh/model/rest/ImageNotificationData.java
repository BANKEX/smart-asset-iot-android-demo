package com.demo.bankexdh.model.rest;

import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ImageNotificationData {

    private static final String NOTIFICATION_IMAGE_TITLE = "I am here";

    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;


    public static DeviceNotificationWrapper getNotification(String imageUrl, Double latitude, Double longitude) {
        DeviceNotificationWrapper wrapper = new DeviceNotificationWrapper();
        JsonStringWrapper jsonStringWrapper = new JsonStringWrapper();

        ImageNotificationData data = new ImageNotificationData();
        data.setImageUrl(imageUrl);
        data.setLatitude(latitude);
        data.setLongitude(longitude);

        jsonStringWrapper.setJsonString(new Gson().toJson(data));
        wrapper.setParameters(jsonStringWrapper);
        wrapper.setNotification(NOTIFICATION_IMAGE_TITLE);
        return wrapper;
    }
}
