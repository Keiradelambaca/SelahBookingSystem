package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class ConversationPreviewDto {
    public String id;
    public String client_id;
    public String provider_id;
    @Nullable public String booking_id;
    @Nullable public String status;
    @Nullable public String created_at;
    @Nullable public String updated_at;
    @Nullable public String client_name;
    @Nullable public String provider_name;
    @Nullable public String last_message;
    @Nullable public String last_message_at;
}