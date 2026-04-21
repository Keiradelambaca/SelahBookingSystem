package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class AiBookingAssessmentResponse {
    @Nullable public String assessment_id;
    @Nullable public Analysis analysis;
    @Nullable public Recommendation recommendation;
    @Nullable public List<String> slot_suggestions;
}
