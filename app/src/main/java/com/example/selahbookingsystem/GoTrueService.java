package com.example.selahbookingsystem;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GoTrueService {
    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    Call<SignUpRes> signUp(@Body SignUpReq body);

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token")
    Call<Session> signIn(@Query("grant_type") String grantType, @Body SignInReq body);
}