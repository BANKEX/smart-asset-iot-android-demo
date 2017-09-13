package com.bkx.lab.model.rest;

import com.bkx.lab.utils.Const;
import com.devicehive.rest.ApiClient;

public class RestHelper {
    private ApiClient apiClient;
    private ApiClient registrationApiClient;

    private RestHelper() {
        if (Const.URL.length() <= 0) {
            throw new NullPointerException("Server URL cannot be null or empty");
        }
        apiClient = new ApiClient(Const.URL);
        registrationApiClient = new ApiClient(Const.REGISTER_URL);
    }

    private static class InstanceHolder {
        static final RestHelper INSTANCE = new RestHelper();
    }

    public static RestHelper getInstance() {
        return RestHelper.InstanceHolder.INSTANCE;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public ApiClient getRegistrationApiClient() {
        return registrationApiClient;
    }

}
