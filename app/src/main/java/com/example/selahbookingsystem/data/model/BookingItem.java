package com.example.selahbookingsystem.data.model;

import com.google.gson.annotations.SerializedName;

public class BookingItem {
    @SerializedName("id") public String id;
    @SerializedName("provider_id") public String providerId;
    @SerializedName("provider_name") public String providerName;
    @SerializedName("start_time") public String startTime;
    @SerializedName("end_time") public String endTime;
    @SerializedName("status") public String status;
    @SerializedName("inspo_photo_url") public String inspoPhotoUrl;
    @SerializedName("current_photo_url") public String currentPhotoUrl;
    @SerializedName("details_json") public Object detailsJson; // fine for now
    @SerializedName("duration_mins") public Integer durationMins;
}
