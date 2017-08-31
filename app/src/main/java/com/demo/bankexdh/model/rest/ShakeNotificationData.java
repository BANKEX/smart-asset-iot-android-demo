package com.demo.bankexdh.model.rest;

import com.demo.bankexdh.utils.Const;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import lombok.Data;

@Data
public class ShakeNotificationData {
    private static final String NOTIFICATION_TITLE = "shake";

    @SerializedName("deviceName")
    private String deviceName;
    @SerializedName("shake")
    private String shake;
    @SerializedName("shakenAt")
    private String shakenAt;

    public static DeviceNotificationWrapper getNotification(String shakeMessage, String deviceName, String assetId) {
        DeviceNotificationWrapper wrapper = new DeviceNotificationWrapper();
        JsonStringWrapper jsonStringWrapper = new JsonStringWrapper();
        ShakeNotificationData shake = new ShakeNotificationData();
        shake.setShake(String.valueOf(shakeMessage));
        shake.setDeviceName(deviceName);
        shake.setShakenAt(DateTime.now().toString());
        jsonStringWrapper.setJsonString(new Gson().toJson(shake));
        wrapper.setParameters(jsonStringWrapper);
        wrapper.setNotification(String.format(Const.NOTIFICATION_NAME_FORMAT, assetId, NOTIFICATION_TITLE));
        return wrapper;
    }
}
