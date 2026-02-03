package com.example.selahbookingsystem.network.service;

import androidx.annotation.Nullable;

import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.dto.ProfileRoleDto;
import com.example.selahbookingsystem.data.model.BookingCreate;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseRestService {

    // =========================
    // PROFILES
    // =========================

    // GET /rest/v1/profiles?id=eq.<uid>&select=...
    @Headers({"Accept: application/json"})
    @GET("rest/v1/profiles")
    Call<List<ProfileDto>> getProfile(
            @Query("id") String idEq,           // "eq.<auth.uid>"
            @Query("select") String select      // "id,full_name,email,phone,dob,role,created_at"
    );

    // PATCH /rest/v1/profiles?id=eq.<uid>  { "full_name": "New Name" }
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/profiles")
    Call<List<ProfileDto>> updateProfileName(
            @Query("id") String idEq,           // "eq.<auth.uid>"
            @Body ProfileUpdateBody body
    );

    // GET /rest/v1/profiles?role=eq.provider&select=id,full_name
    @Headers({"Accept: application/json"})
    @GET("rest/v1/profiles")
    Call<List<ProviderDto>> listProviders(
            @Query("role") String roleEq,          // "eq.provider"
            @Query("select") String selectFields   // "id,full_name"
    );

    // POST /rest/v1/profiles  (simple insert)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/profiles")
    Call<List<ProfileDto>> insertProfile(@Body ProfileDto body);


    // POST /rest/v1/bookings
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/bookings")
    Call<List<BookingDto>> createBooking(@Body BookingDto body);

    // GET upcoming bookings for a client
    // /rest/v1/bookings?client_id=eq.<uid>&start_time=gte.<iso>&order=start_time.asc&select=...
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> listUpcomingBookingsForClient(
            @Query("client_id") String clientIdEq, // "eq.<auth.uid>"
            @Query("start_time") String startGte,  // "gte.2026-01-28T00:00:00Z"
            @Query("order") String orderBy,        // "start_time.asc"
            @Query("select") String select,        // columns
            @Query("limit") int limit
    );

    // Same as above but limited (Home page preview)
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> listUpcomingBookingsForClientLimited(
            @Query("client_id") String clientIdEq,
            @Query("start_time") String startGte,
            @Query("order") String orderBy,
            @Query("select") String select,
            @Query("limit") int limit
    );


    // =========================
    // DTOs
    // =========================

    // Provider list rows from profiles
    class ProviderDto {
        public String id;
        public String full_name;
    }

    // Full profile row from public.profiles
    class ProfileDto {
        public String id;
        public String email;
        public String phone;
        public String role;
        public String full_name;      // customers + providers
        public String dob;            // customers
        public String business_name;  // providers (optional)
        public String eircode;        // optional
    }

    // Body for updating only the name
    class ProfileUpdateBody {
        public String full_name;

        public ProfileUpdateBody(String full_name) {
            this.full_name = full_name;
        }
    }

    @GET("rest/v1/bookings")
    Call<List<BookingDto>> getBookingById(
            @Query("select") String select,
            @Query("id") String idFilter
    );

    @GET("rest/v1/profiles")
    Call<List<ProfileRoleDto>> getUserRole(
            @Query("id") String idFilter,
            @Query("select") String select
    );

}