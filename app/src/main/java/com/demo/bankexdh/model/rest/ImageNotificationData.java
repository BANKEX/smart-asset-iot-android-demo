package com.demo.bankexdh.model.rest;

import com.demo.bankexdh.utils.Const;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ImageNotificationData {

    private static final String NOTIFICATION_TITLE = "upload";

    @SerializedName("deviceName")
    private String deviceName;
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;


    public static DeviceNotificationWrapper getNotification(String imageUrl, String deviceName, String assetId, Double latitude, Double longitude) {
        DeviceNotificationWrapper wrapper = new DeviceNotificationWrapper();
        JsonStringWrapper jsonStringWrapper = new JsonStringWrapper();

        ImageNotificationData data = new ImageNotificationData();
        data.setDeviceName(deviceName);
        data.setImageUrl(imageUrl);
        data.setLatitude(latitude);
        data.setLongitude(longitude);

        jsonStringWrapper.setJsonString(new Gson().toJson(data));
        wrapper.setParameters(jsonStringWrapper);
        wrapper.setNotification(String.format(Const.NOTIFICATION_NAME_FORMAT, assetId, NOTIFICATION_TITLE));
        return wrapper;
    }
}
