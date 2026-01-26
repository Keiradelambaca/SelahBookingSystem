package com.example.selahbookingsystem.network.service;

import com.example.selahbookingsystem.data.model.RefreshReq;
import com.example.selahbookingsystem.data.model.SignInReq;
import com.example.selahbookingsystem.data.model.SignUpReq;
import com.example.selahbookingsystem.data.model.SignUpResponse;
import com.example.selahbookingsystem.data.session.Session;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GoTrueService {

    // existing signIn method (leave as it is)
    @Headers({"Content-Type: application/json"})
    @POST("auth/v1/token")
    Call<Session> signIn(
            @Query("grant_type") String grant,
            @Body SignInReq body
    );

    // NEW: sign-up for creating accounts
    @Headers({"Content-Type: application/json"})
    @POST("auth/v1/signup")
    Call<SignUpResponse> signUp(@Body SignUpReq body);

    @POST("auth/v1/token?grant_type=refresh_token")
    Call<Session> refreshToken(@Body RefreshReq body);

}
