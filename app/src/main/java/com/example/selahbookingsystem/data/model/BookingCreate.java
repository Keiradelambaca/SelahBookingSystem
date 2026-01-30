package com.example.selahbookingsystem.data.model;

import androidx.annotation.Nullable;

import java.util.Map;

public class BookingCreate {
    public String client_id;
    public String provider_id;

    public String start_time;
    public String end_time;

    @Nullable public String provider_name;
    @Nullable public String current_photo_url;
    @Nullable public String inspo_photo_url;

    @Nullable public String status;
    @Nullable public Integer duration_mins;

    @Nullable public Object details_json;

    public BookingCreate(String client_id,
                         String provider_id,
                         String start_time,
                         String end_time,
                         @Nullable String provider_name,
                         @Nullable String current_photo_url,
                         @Nullable String inspo_photo_url,
                         @Nullable String status,
                         @Nullable Integer duration_mins,
                         @Nullable Object details_json) {
        this.client_id = client_id;
        this.provider_id = provider_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.provider_name = provider_name;
        this.current_photo_url = current_photo_url;
        this.inspo_photo_url = inspo_photo_url;
        this.status = status;
        this.duration_mins = duration_mins;
        this.details_json = details_json;
    }

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

