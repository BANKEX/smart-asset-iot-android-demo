package com.bkx.lab.model.rest;

import com.bkx.lab.utils.Const;
import com.devicehive.rest.ApiClient;

public class RestHelper {
    private ApiClient apiClient;

    private RestHelper() {
    }

    private static class InstanceHolder {
        static final RestHelper INSTANCE = new RestHelper();
    }

    public static RestHelper getInstance() {
        return RestHelper.InstanceHolder.INSTANCE;
    }

    public ApiClient getApiClient() {
        if (apiClient == null) {
            apiClient = new ApiClient(Const.URL);
        }
        return apiClient;
    }

}
