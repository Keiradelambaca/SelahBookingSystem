package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class Recommendation {
    @Nullable public List<RecommendedServiceItem> recommended_services;
    @Nullable public Integer total_duration_mins;
    @Nullable public Integer total_price_cents;
}