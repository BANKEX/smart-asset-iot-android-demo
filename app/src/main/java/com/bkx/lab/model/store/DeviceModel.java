package com.bkx.lab.model.store;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Data;

@Data
public class DeviceModel extends RealmObject {
    public static final long DEFAULT_ID = 1L;

    public static final String ID = "id";
    public static final String DEVICE_ID = "deviceId";
    public static final String NAME = "name";
    public static final String DATA = "data";
    public static final String NETWORK_ID = "networkId";
    public static final String IS_BLOCKED = "isBlocked";


    @PrimaryKey
    private Long id;
    private String deviceId;
    private String name;
    private String deviceName;
    private String data;
    private Long networkId;
    private Boolean isBlocked;


}
