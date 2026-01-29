package com.example.selahbookingsystem.network.api;

import com.example.selahbookingsystem.data.model.ServiceItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ServicesApi {

    @GET("rest/v1/services")
    Call<List<ServiceItem>> listServices(
            @Query("select") String select
    );
}