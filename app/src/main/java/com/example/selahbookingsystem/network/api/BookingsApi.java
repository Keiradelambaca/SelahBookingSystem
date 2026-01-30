package com.example.selahbookingsystem.network.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BookingsApi {

    // Inserts into: rest/v1/bookings
    // Prefer return=representation makes Supabase return the inserted row(s)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/bookings")
    Call<List<Map<String, Object>>> createBooking(
            @Body Map<String, Object> body,
            @Query("select") String select
    );

    @GET("rest/v1/bookings")
    Call<List<com.example.selahbookingsystem.data.model.BookingItem>> listMyBookings(
            @Query("client_id") String clientFilter,   // "eq.<uuid>"
            @Query("order") String order,              // "created_at.desc"
            @Query("select") String select
    );

}

