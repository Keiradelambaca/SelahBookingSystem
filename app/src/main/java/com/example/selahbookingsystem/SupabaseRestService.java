package com.example.selahbookingsystem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseRestService {

    // ---- PROFILES (single profile by id) ----
    // GET /rest/v1/profiles?id=eq.<uid>&select=id,full_name,email,phone,dob,role,created_at
    @Headers({"Accept: application/json"})
    @GET("rest/v1/profiles")
    Call<List<ProfileDto>> getProfile(
            @Query("id") String idEq,           // "eq.<auth.uid>"
            @Query("select") String select      // "id,full_name,email,phone,dob,role,created_at"
    );

    // ---- PROFILES (update name) ----
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

    // ---- PROFILES (providers list) ----
    // GET /rest/v1/profiles?role=eq.provider&select=id,full_name
    @Headers({"Accept: application/json"})
    @GET("rest/v1/profiles")
    Call<List<ProviderDto>> listProviders(
            @Query("role") String roleEq,          // "eq.provider"
            @Query("select") String selectFields   // "id,full_name"
    );

    // ---- BOOKINGS (customer’s upcoming) ----
    // GET /rest/v1/bookings?client_id=eq.<uid>&order=start_time.asc
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> listBookingsForClient(
            @Query("client_id") String clientIdEq, // "eq.<auth.uid>"
            @Query("order") String orderBy         // "start_time.asc"
    );

    // ---- BOOKINGS (create) ----
    // POST /rest/v1/bookings (JSON body)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/bookings")
    Call<List<BookingDto>> createBooking(@Body BookingCreate body);

    @Headers({
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates"
    })
    @POST("rest/v1/profiles")
    Call<Void> insertProfile(@Body ProfileDto body);



    // ---------- DTOs ----------

    // For provider list dropdowns etc.
    class ProviderDto {
        public String id;
        public String full_name; // profiles(id, full_name)
    }

    // Booking row from public.bookings
    class BookingDto {
        public String id;
        public String client_id;
        public String service_id;
        public String technician_id;
        public String salon_id;
        public String start_time; // ISO8601 timestamptz
        public String end_time;
        public String status;
        public String created_at;
    }

    // Payload for creating a booking
    class BookingCreate {
        public String salon_id;
        public String client_id;
        public String service_id;     // optional for now
        public String technician_id;  // provider chosen
        public String start_time;     // e.g. "2025-11-10T14:00:00Z"
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

    // Full profile row from public.profiles
    class ProfileDto {
        public String id;
        public String full_name;
        public String email;
        public String role;
        public String created_at;

        public String phone;   // new column
        public String dob;     // new column (Supabase date -> JSON string)
    }

    // Body for updating only the name
    class ProfileUpdateBody {
        public String full_name;

        public ProfileUpdateBody(String full_name) {
            this.full_name = full_name;
        }
    }
}
