package com.example.selahbookingsystem.network.api;

import com.example.selahbookingsystem.data.model.ServiceItem;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ServicesApi {

    @GET("rest/v1/services")
    Call<List<ServiceItem>> listServices(
            @Query("select") String select
    );

    @Headers({"Accept: application/json"})
    @GET("rest/v1/services")
    Call<List<ServiceItem>> getServiceById(
            @Query("id") String idEq,
            @Query("select") String select
    );
}
