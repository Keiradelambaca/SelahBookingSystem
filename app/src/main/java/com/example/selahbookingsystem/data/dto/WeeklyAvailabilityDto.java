package com.example.selahbookingsystem.data.dto;

public class WeeklyAvailabilityDto {
    public long id;            // bigserial
    public String provider_id; // uuid as string
    public int day_of_week;    // 0..6 (Sun..Sat)
    public String start_time;  // "09:00:00"
    public String end_time;    // "17:00:00"
    public boolean enabled;
}