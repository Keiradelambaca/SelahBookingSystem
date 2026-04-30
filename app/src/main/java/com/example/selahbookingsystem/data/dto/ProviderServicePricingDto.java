package com.example.selahbookingsystem.data.dto;

public class ProviderServicePricingDto {
    public String id;
    public String provider_id;
    public String app_service_id;
    public Boolean is_offered;
    public Integer price_cents;
    public Integer duration_mins;

    // from view
    public String service_code;
    public String service_name;
    public String service_category;
}