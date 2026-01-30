package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class BookingDto {
    public String id;
    public String client_id;
    public String provider_id;

    public String start_time;     // timestamptz ISO
    public String end_time;       // timestamptz ISO
    public String created_at;

    @Nullable public String provider_name;
    @Nullable public String current_photo_url;
    @Nullable public String inspo_photo_url;

    @Nullable public String status;

    @Nullable public Integer duration_mins;

    // jsonb can come back as Map/LinkedTreeMap; Object is fine for now
    @Nullable public Object details_json;
}

