package com.example.selahbookingsystem.network.service;

import androidx.annotation.Nullable;

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


    // =========================
    // BOOKINGS (matches your table)
    // Table columns (per your screenshot):
    // id, client_id, provider_id, start_time, end_time, created_at,
    // provider_name, current_photo_url, inspo_photo_url, staus, duration_mins, details_json
    // =========================

    // POST /rest/v1/bookings
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/bookings")
    Call<List<BookingDto>> createBooking(@Body BookingCreate body);

    // GET upcoming bookings for a client
    // /rest/v1/bookings?client_id=eq.<uid>&start_time=gte.<iso>&order=start_time.asc&select=...
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> listUpcomingBookingsForClient(
            @Query("client_id") String clientIdEq, // "eq.<auth.uid>"
            @Query("start_time") String startGte,  // "gte.2026-01-28T00:00:00Z"
            @Query("order") String orderBy,        // "start_time.asc"
            @Query("select") String select         // columns
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

    // Booking row from public.bookings (MATCHES YOUR COLUMN NAMES)
    class BookingDto {
        public String id;
        public String client_id;
        public String provider_id;

        public String start_time;     // timestamptz ISO string
        public String end_time;       // timestamptz ISO string
        public String created_at;

        @Nullable public String provider_name;
        @Nullable public String current_photo_url;
        @Nullable public String inspo_photo_url;

        // NOTE: your table shows "staus" (typo). If you rename the column to "status",
        // rename this field too.
        @Nullable public String staus;

        @Nullable public Integer duration_mins;

        // jsonb – Retrofit/Gson can serialize/deserialize Maps fine
        @Nullable public Object details_json;
    }

    // Payload for creating a booking
    class BookingCreate {
        public String client_id;
        public String provider_id;

        public String start_time;
        public String end_time;

        @Nullable public String provider_name;
        @Nullable public String current_photo_url;
        @Nullable public String inspo_photo_url;

        @Nullable public String staus;         // change to "status" if you fix DB column name
        @Nullable public Integer duration_mins;

        @Nullable public Object details_json;  // Map<String,String> recommended

        public BookingCreate(String client_id,
                             String provider_id,
                             String start_time,
                             String end_time,
                             @Nullable String provider_name,
                             @Nullable String current_photo_url,
                             @Nullable String inspo_photo_url,
                             @Nullable String staus,
                             @Nullable Integer duration_mins,
                             @Nullable Object details_json) {

            this.client_id = client_id;
            this.provider_id = provider_id;
            this.start_time = start_time;
            this.end_time = end_time;
            this.provider_name = provider_name;
            this.current_photo_url = current_photo_url;
            this.inspo_photo_url = inspo_photo_url;
            this.staus = staus;
            this.duration_mins = duration_mins;
            this.details_json = details_json;
        }

        // Convenience factory if you want to pass Map directly
        public static BookingCreate of(String clientId,
                                       String providerId,
                                       String startIso,
                                       String endIso,
                                       String providerName,
                                       int durationMins,
                                       @Nullable Map<String, String> detailsMap,
                                       @Nullable String currentPhotoUrl,
                                       @Nullable String inspoPhotoUrl) {
            return new BookingCreate(
                    clientId,
                    providerId,
                    startIso,
                    endIso,
                    providerName,
                    currentPhotoUrl,
                    inspoPhotoUrl,
                    "confirmed",
                    durationMins,
                    detailsMap
            );
        }
    }
}