package com.example.selahbookingsystem.data.dto;

public class DailyAvailabilityDto {
    public long id;
    public String provider_id;
    public String date;       // "YYYY-MM-DD"
    public String start_time; // "09:00:00"
    public String end_time;   // "17:00:00"
    public boolean enabled;
}
