package com.example.selahbookingsystem.network.service;

import com.example.selahbookingsystem.data.dto.AiBookingAssessmentResponse;
import com.example.selahbookingsystem.data.dto.AppServiceDto;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.dto.ProfileRoleDto;
import com.example.selahbookingsystem.data.dto.ProviderServicePricingDto;
import com.example.selahbookingsystem.data.dto.ServiceDto;
import com.example.selahbookingsystem.data.dto.ServiceModifierDto;
import com.example.selahbookingsystem.data.dto.EmailTemplateDto;
import com.example.selahbookingsystem.data.dto.ProviderSettingsDto;
import com.example.selahbookingsystem.data.model.AiBookingAssessmentRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
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
            "Accept: application/json",
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


    class BookingUpdateBody {
        public String status;
        public String start_time;
        public String end_time;

        public BookingUpdateBody(String status, String start_time, String end_time) {
            this.status = status;
            this.start_time = start_time;
            this.end_time = end_time;
        }
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

    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingDto>> getBookingById(
            @Query("select") String select,
            @Query("id") String idEq
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


    //BookingDto
    // A lightweight profile nested object for joins
    class ProfileMiniDto {
        public String id;
        public String full_name;
        public String email;
        public String phone;
    }

    class BookingWithClientDto {
        public String id;
        public String client_id;
        public String provider_id;
        public String status;
        public String start_time;
        public String end_time;
        public String created_at;

        public Integer duration_mins;
        public String inspo_photo_url;
        public String provider_name;

        public ProfileMiniDto client;
    }

    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    retrofit2.Call<java.util.List<BookingWithClientDto>> listConfirmedBookingsWithClient(
            @Query("provider_id") String providerIdEq,   // "eq.<providerId>"
            @Query("status") String statusEq,            // "eq.confirmed"
            @Query("select") String select,              // include join
            @Query("order") String order                 // "start_time.desc"
    );

    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<BookingWithClientDto>> listConfirmedBookingsWithClientForRange(
            @Query("provider_id") String providerIdEq,      // "eq.<providerId>"
            @Query("status") String statusEq,               // "eq.confirmed"
            @Query("start_time") List<String> startFilters, // ["gte.<ISO>", "lt.<ISO>"]
            @Query("select") String select,
            @Query("order") String order
    );

// SP WEEKLY AVAILABILITY
    @Headers({"Accept: application/json"})
    @GET("rest/v1/sp_weekly_availability")
    Call<List<com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto>> listWeeklyAvailability(
            @Query("provider_id") String providerEq, // "eq.<uid>"
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/sp_weekly_availability")
    Call<List<com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto>> insertWeeklyAvailability(
            @Body com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto body
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/sp_weekly_availability")
    Call<List<com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto>> updateWeeklyAvailability(
            @Query("id") String idEq, // "eq.<id>"
            @Body com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto body
    );

    @Headers({"Prefer: return=minimal"})
    @DELETE("rest/v1/sp_weekly_availability")
    Call<Void> deleteWeeklyAvailability(
            @Query("id") String idEq // "eq.<id>"
    );


// SP DAILY AVAILABILITY
    @Headers({"Accept: application/json"})
    @GET("rest/v1/sp_daily_availability")
    Call<List<com.example.selahbookingsystem.data.dto.DailyAvailabilityDto>> listDailyAvailabilityForDate(
            @Query("provider_id") String providerEq, // "eq.<uid>"
            @Query("date") String dateEq,            // "eq.2026-02-16"
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/sp_daily_availability")
    Call<List<com.example.selahbookingsystem.data.dto.DailyAvailabilityDto>> insertDailyAvailability(
            @Body com.example.selahbookingsystem.data.dto.DailyAvailabilityDto body
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/sp_daily_availability")
    Call<List<com.example.selahbookingsystem.data.dto.DailyAvailabilityDto>> updateDailyAvailability(
            @Query("id") String idEq,
            @Body com.example.selahbookingsystem.data.dto.DailyAvailabilityDto body
    );

    @Headers({"Prefer: return=minimal"})
    @DELETE("rest/v1/sp_daily_availability")
    Call<Void> deleteDailyAvailability(
            @Query("id") String idEq
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/sp_weekly_availability")
    Call<List<com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto>> updateWeeklyAvailabilityByProviderDay(
            @Query("provider_id") String providerEq,   // "eq.<uid>"
            @Query("day_of_week") String dayEq,        // "eq.3"
            @Body com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto body
    );

// BOOKINGS: get provider bookings within day
    @Headers({"Accept: application/json"})
    @GET("rest/v1/bookings")
    Call<List<com.example.selahbookingsystem.data.dto.BookingDto>> listBookingsForProviderBetween(
            @Query("provider_id") String providerEq,
            @Query("start_time") List<String> startFilters, // ["gte.<ISO>", "lt.<ISO>"]
            @Query("select") String select,
            @Query("order") String order
    );


// SERVICES

    // GET /rest/v1/services?provider_id=eq.<uuid>&is_active=eq.true&order=created_at.asc&select=...
    @Headers({
            "Accept: application/json"
    })
    @GET("rest/v1/services")
    Call<List<ServiceDto>> listServicesForProvider(
            @Query("provider_id") String providerIdEq,   // "eq.<uuid>"
            @Query("is_active") String isActiveEq,       // "eq.true"
            @Query("order") String order,                // "created_at.asc"
            @Query("select") String select               // "id,provider_id,name,base_price_cents,base_duration_mins,is_active"
    );

    // Create service
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/services")
    Call<List<ServiceDto>> createService(@Body Map<String, Object> body);

    // Update service
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/services")
    Call<List<ServiceDto>> updateService(
            @Query("id") String idEq, // "eq.<serviceId>"
            @Body Map<String, Object> body
    );

// SERVICE MODIFIERS (bubbles)

    @Headers({"Accept: application/json"})
    @GET("rest/v1/service_modifiers")
    Call<List<ServiceModifierDto>> listModifiersForService(
            @Query("service_id") String serviceIdEq,
            @Query("is_active") String isActiveEq,
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/service_modifiers")
    Call<List<ServiceModifierDto>> createModifier(@Body Map<String, Object> body);

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/service_modifiers")
    Call<List<ServiceModifierDto>> updateModifier(
            @Query("id") String idEq, // "eq.<modifierId>"
            @Body Map<String, Object> body
    );

// AUTO EMAIL TEMPLATES

    // GET /rest/v1/email_templates?provider_id=eq.<uid>&select=...
    @Headers({"Accept: application/json"})
    @GET("rest/v1/email_templates")
    Call<List<EmailTemplateDto>> listEmailTemplatesForProvider(
            @Query("provider_id") String providerIdEq,   // "eq.<uid>"
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates, return=representation"
    })
    @POST("rest/v1/email_templates")
    Call<List<EmailTemplateDto>> upsertEmailTemplates(
            @Query("on_conflict") String onConflict,   // "provider_id,type"
            @Body List<EmailTemplateDto> body
    );


// BOOKINGS: update (cancel/reschedule/status)

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/bookings")
    Call<List<BookingDto>> updateBooking(
            @Query("id") String idEq,     // "eq.<bookingId>"
            @Body BookingUpdateBody body
    );


// EDGE FUNCTIONS: booking emails

    @Headers({"Content-Type: application/json"})
    @POST("functions/v1/booking-confirmed-email")
    Call<Map<String, Object>> fnBookingConfirmedEmail(@Body Map<String, Object> body);

    @Headers({"Content-Type: application/json"})
    @POST("functions/v1/booking-cancelled-email")
    Call<Map<String, Object>> fnBookingCancelledEmail(@Body Map<String, Object> body);

    @Headers({"Content-Type: application/json"})
    @POST("functions/v1/booking-rescheduled-email")
    Call<Map<String, Object>> fnBookingRescheduledEmail(@Body Map<String, Object> body);

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @POST("functions/v1/create-deposit-checkout")
    Call<Map<String, Object>> fnCreateDepositCheckout(@Body Map<String, Object> body);

// PROVIDER SETTINGS

    @Headers({"Accept: application/json"})
    @GET("rest/v1/provider_settings")
    Call<List<ProviderSettingsDto>> getProviderSettings(
            @Query("provider_id") String providerIdEq,
            @Query("select") String select
    );

    @Headers({
            "Accept: application/json",
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates,return=representation"
    })
    @POST("rest/v1/provider_settings")
    Call<List<ProviderSettingsDto>> upsertProviderSettings(@Body Map<String, Object> body);


    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @POST("functions/v1/analyse-nail-booking")
    Call<AiBookingAssessmentResponse> analyseNailBooking(
            @Body AiBookingAssessmentRequest body
    );

    @Headers({"Accept: application/json"})
    @GET("rest/v1/app_services")
    Call<List<AppServiceDto>> listAppServices(
            @Query("is_active") String isActiveEq,
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({"Accept: application/json"})
    @GET("rest/v1/provider_service_pricing_view")
    Call<List<ProviderServicePricingDto>> getProviderServicePricing(
            @Query("provider_id") String providerIdEq,
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates, return=representation"
    })
    @POST("rest/v1/provider_service_pricing")
    Call<List<ProviderServicePricingDto>> upsertProviderServicePricing(
            @Query("on_conflict") String onConflict,
            @Body List<Map<String, Object>> body
    );


}
