package com.example.selahbookingsystem.network.service;

import androidx.annotation.Nullable;

import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.dto.ConversationDto;
import com.example.selahbookingsystem.data.dto.ConversationPreviewDto;
import com.example.selahbookingsystem.data.dto.CreateConversationBody;
import com.example.selahbookingsystem.data.dto.CreateMessageBody;
import com.example.selahbookingsystem.data.dto.MessageDto;
import com.example.selahbookingsystem.data.dto.ProfileRoleDto;
import com.example.selahbookingsystem.data.dto.ReadAtUpdateBody;
import com.example.selahbookingsystem.data.dto.UpdateConversationStatusBody;
import com.example.selahbookingsystem.data.model.BookingCreate;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
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
    public static class ProviderDto {
        public String id;
        public String full_name;
    }

    // Full profile row from public.profiles
    public static class ProfileDto {
        public String id;
        public String email;
        public String phone;
        public String role;
        public String full_name;      // customers + providers
        public String dob;            // customers
        public String business_name;  // providers
        public String eircode;        // optional
        public String address;        // providers
        public String banner_url;     // providers
        public Double lat;            // providers
        public Double lng;            // providers


    }

    // Body for updating only the name
    public static class ProfileUpdateBody {
        public String full_name;

        public ProfileUpdateBody(String full_name) {
            this.full_name = full_name;
        }
    }

    public static class ProviderLocationUpdateBody {
        public String eircode;
        public String address;
        public Double lat;
        public Double lng;

        public ProviderLocationUpdateBody(String eircode, String address, Double lat, Double lng) {
            this.eircode = eircode;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }
    }


    public static class ProviderBannerUpdateBody {
        public String banner_url;
        public ProviderBannerUpdateBody(String bannerUrl) { this.banner_url = bannerUrl; }
    }

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/profiles")
    Call<List<ProfileDto>> updateProviderBanner(
            @Query("id") String idFilter,
            @Body ProviderBannerUpdateBody body
    );

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

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/profiles")
    Call<List<ProfileDto>> updateProviderLocation(
            @Query("id") String idFilter,
            @Body ProviderLocationUpdateBody body
    );

    //Messaging DTO

    @GET("rest/v1/conversation_previews")
    Call<List<ConversationPreviewDto>> listCustomerConversationPreviews(
            @Query("client_id") String clientId,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/conversation_previews")
    Call<List<ConversationPreviewDto>> listProviderConversationPreviews(
            @Query("provider_id") String providerId,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/conversations")
    Call<List<ConversationDto>> findConversationByClientAndProvider(
            @Query("client_id") String clientId,
            @Query("provider_id") String providerId,
            @Query("select") String select,
            @Query("limit") int limit
    );

    @GET("rest/v1/conversations")
    Call<List<ConversationDto>> findConversationByClientProviderAndBooking(
            @Query("client_id") String clientId,
            @Query("provider_id") String providerId,
            @Query("booking_id") String bookingId,
            @Query("select") String select,
            @Query("limit") int limit
    );

    @POST("rest/v1/conversations")
    Call<List<ConversationDto>> createConversation(
            @Header("Prefer") String prefer,
            @Body CreateConversationBody body
    );

    @PATCH("rest/v1/conversations")
    Call<List<ConversationDto>> updateConversationStatus(
            @Header("Prefer") String prefer,
            @Query("id") String conversationId,
            @Body UpdateConversationStatusBody body
    );

    @GET("rest/v1/messages")
    Call<List<MessageDto>> listMessages(
            @Query("conversation_id") String conversationId,
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/messages")
    Call<List<MessageDto>> createMessage(
            @Header("Prefer") String prefer,
            @Body CreateMessageBody body
    );

    @PATCH("rest/v1/messages")
    Call<List<MessageDto>> markMessagesRead(
            @Header("Prefer") String prefer,
            @Query("conversation_id") String conversationId,
            @Query("receiver_id") String receiverId,
            @Query("read_at") String readAtFilter,
            @Body ReadAtUpdateBody body
    );

}
