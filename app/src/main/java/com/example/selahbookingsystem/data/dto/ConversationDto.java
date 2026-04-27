package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class ConversationDto {
    public String id;
    public String client_id;
    public String provider_id;
    @Nullable public String booking_id;
    @Nullable public String status;
    @Nullable public String created_at;
    @Nullable public String updated_at;
}