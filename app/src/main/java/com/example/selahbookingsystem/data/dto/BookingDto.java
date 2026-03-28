package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class BookingDto {
    public String id;
    public String client_id;
    public String provider_id;
    public String start_time;
    public String end_time;
    public String created_at;

    @Nullable public String provider_name;
    @Nullable public String current_photo_url;
    @Nullable public String inspo_photo_url;
    @Nullable public String status;
    @Nullable public Integer duration_mins;
    @Nullable public Object details_json;

    @Nullable public Integer total_price_cents;
    @Nullable public Boolean deposit_required;
    @Nullable public Integer deposit_percent;
    @Nullable public Integer deposit_amount_cents;

    @Nullable public String payment_status; // not_required, requires_payment, paid etc
    @Nullable public String payment_provider; // stripe
    @Nullable public String payment_ref; //checkout_session_id
    @Nullable public String client_name;
    @Nullable public String service_name;

}

