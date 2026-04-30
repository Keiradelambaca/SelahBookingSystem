package com.example.selahbookingsystem.data.dto;

public class EmailTemplateDto {
    public String id;
    public String provider_id;

    // "BOOKING_CONFIRMATION" | "CANCELLATION" | "RESCHEDULED" | "REMINDER_24H"
    public String type;

    public String subject;
    public String body;

    public Boolean is_enabled;
    public String updated_at;
}
