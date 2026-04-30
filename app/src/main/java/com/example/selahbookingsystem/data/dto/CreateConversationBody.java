package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class CreateConversationBody {
    public String client_id;
    public String provider_id;
    @Nullable public String booking_id;
    public String status;

    public CreateConversationBody(String client_id, String provider_id, @Nullable String booking_id, String status) {
        this.client_id = client_id;
        this.provider_id = provider_id;
        this.booking_id = booking_id;
        this.status = status;
    }
}
