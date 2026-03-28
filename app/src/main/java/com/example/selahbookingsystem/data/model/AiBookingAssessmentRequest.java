package com.example.selahbookingsystem.data.model;

import androidx.annotation.Nullable;

public class AiBookingAssessmentRequest {
    @Nullable public String provider_id;
    @Nullable public String current_photo_url;
    @Nullable public String inspo_photo_url;

    public AiBookingAssessmentRequest(String provider_id, String current_photo_url, String inspo_photo_url) {
        this.provider_id = provider_id;
        this.current_photo_url = current_photo_url;
        this.inspo_photo_url = inspo_photo_url;
    }
}