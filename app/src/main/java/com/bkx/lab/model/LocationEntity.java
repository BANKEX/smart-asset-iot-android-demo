package com.bkx.lab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class LocationEntity {

    private static final double DEFAULT_LATITUDE = 40.710652;
    private static final double DEFAULT_LONGITUDE = -74.015577;

    private double longitude = DEFAULT_LONGITUDE;
    private double latitude = DEFAULT_LATITUDE;

}
