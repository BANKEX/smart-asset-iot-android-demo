package com.bkx.lab.model.rest.api;

import com.bkx.lab.model.rest.RegisterBody;
import com.bkx.lab.model.rest.RegisterData;
import com.bkx.lab.utils.Const;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface Register {

    @POST("register")
    @Headers({"ContentType: application/json", "Authorization:" + Const.KEY})
    Call<RegisterData> register(@Body RegisterBody body);
}
