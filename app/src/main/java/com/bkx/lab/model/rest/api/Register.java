package com.bkx.lab.model.rest.api;

import com.bkx.lab.model.rest.RegisterBody;
import com.bkx.lab.model.rest.RegisterData;
import com.bkx.lab.utils.Const;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface Register {

    @POST
    @Headers({"ContentType: application/json","Authorization:"+ Const.KEY})
    public Call<RegisterData> register(@Url String url, @Body RegisterBody body);
}
