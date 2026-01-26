package com.example.selahbookingsystem.network.service;

import com.example.selahbookingsystem.data.dto.LikeDto;
import com.example.selahbookingsystem.data.dto.LookDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseService {

    @GET("rest/v1/looks")
    Call<List<LookDto>> getLooks(
            @Query("select") String select,
            @Query("order") String order,
            @Query("limit") Integer limit,
            @Query("offset") Integer offset
    );

    @GET("rest/v1/look_likes")
    Call<List<LikeDto>> getLikesForUser(
            @Query("select") String select,
            @Query("user_id") String userIdEq,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=minimal"
    })
    @POST("rest/v1/look_likes")
    Call<Void> like(@Body Map<String, Object> body);

    @DELETE("rest/v1/look_likes")
    Call<Void> unlike(
            @Query("user_id") String userIdEq,
            @Query("look_id") String lookIdEq
    );

    @GET("rest/v1/looks")
    Call<List<LookDto>> getLookById(
            @Query("select") String select,
            @Query("id") String idEq
    );

}