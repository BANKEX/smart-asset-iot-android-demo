package com.demo.bankexdh.model.rest;

import com.demo.bankexdh.utils.Const;
import com.devicehive.rest.ApiClient;

public class RestHelper {

    private RestHelper() {
    }

    private static class InstanceHolder {
        static final RestHelper INSTANCE = new RestHelper();
    }

    public static RestHelper getInstance() {
        return RestHelper.InstanceHolder.INSTANCE;
    }

    public ApiClient getApiClient() {
        return new ApiClient(Const.URL);
    }

}
