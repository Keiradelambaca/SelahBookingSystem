package com.example.selahbookingsystem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface SupabaseRestService {

    // ---- PROFILES (providers) ----
    // GET /rest/v1/profiles?role=eq.provider&select=id,full_name
    @Headers({"Accept: application/json"})
    @GET("rest/v1/profiles")
    Call<List<ProviderDto>> listProviders(
            @Query("role") String roleEq,          // pass "eq.provider"
            @Query("select") String selectFields   // pass "id,full_name"
    );

    // ---- BOOKINGS (customer’s upcoming) ----
    // GET /rest/v1/bookings?client_id=eq.<uid>&order=start_time.asc
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> listBookingsForClient(
            @Query("client_id") String clientIdEq, // pass "eq.<auth.uid>"
            @Query("order") String orderBy         // pass "start_time.asc"
    );

    // ---- BOOKINGS (create) ----
    // POST /rest/v1/bookings (JSON body)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/bookings")
    Call<List<BookingDto>> createBooking(@Body BookingCreate body);

    // DTOs
    class ProviderDto {
        public String id;
        public String full_name; // columns: profiles(id, full_name)
    }

    public static class BookingDto {
        public String id;
        public String client_id;
        public String service_id;
        public String technician_id;
        public String salon_id;
        public String start_time; // ISO8601 timestamptz from Supabase
        public String end_time;
        public String status;
        public String created_at;
    }

    public static class BookingCreate {
        public String salon_id;
        public String client_id;
        public String service_id;     // optional for now
        public String technician_id;  // provider chosen
        public String start_time;     // e.g., "2025-11-10T14:00:00Z"
        public String end_time;

        public BookingCreate(String salon_id, String client_id, String service_id,
                             String technician_id, String start_time, String end_time) {
            this.salon_id = salon_id;
            this.client_id = client_id;
            this.service_id = service_id;
            this.technician_id = technician_id;
            this.start_time = start_time;
            this.end_time = end_time;
        }
    }
}
