package com.example.selahbookingsystem.data.dto;

import java.util.ArrayList;
import java.util.List;

public class PriceEstimateDto {
    public int total_price_cents;
    public int total_duration_mins;
    public List<String> included_service_codes = new ArrayList<>();
    public List<String> missing_service_codes = new ArrayList<>();
}