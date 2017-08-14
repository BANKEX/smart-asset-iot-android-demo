package com.demo.bankexdh.model.rest.api;

import com.demo.bankexdh.model.rest.RegisterData;
import com.demo.bankexdh.model.rest.RegisterBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface Register {

    @POST
    @Headers("ContentType: application/json")
    public Call<RegisterData> register(@Url String url, @Body RegisterBody body);
}
