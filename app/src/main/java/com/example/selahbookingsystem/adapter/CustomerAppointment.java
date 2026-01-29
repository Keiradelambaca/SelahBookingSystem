package com.example.selahbookingsystem.adapter;

import java.time.Instant;

public class CustomerAppointment {

    public enum PaymentStatus {
        DEPOSIT_PAID,
        DEPOSIT_NOT_PAID,
        PAID,
        PAY_BY_CASH
    }

    private String id;
    private String serviceTitle;
    private String providerName;
    private String locationArea;
    private double price;
    private PaymentStatus paymentStatus = PaymentStatus.DEPOSIT_NOT_PAID;
    private String bannerUrl;            // nullable
    private Instant appointmentStart;   // UTC Instant
    private int durationMins = 60;       // default

    // ✅ REQUIRED empty constructor (for backend mapping)
    public CustomerAppointment() {
    }

    // ✅ Keep your original constructor for convenience if you still want it
    public CustomerAppointment(
            String id,
            String serviceTitle,
            String providerName,
            String locationArea,
            double price,
            PaymentStatus paymentStatus,
            String bannerUrl,
            Instant appointmentStart
    ) {
        this.id = id;
        this.serviceTitle = serviceTitle;
        this.providerName = providerName;
        this.locationArea = locationArea;
        this.price = price;
        this.paymentStatus = paymentStatus;
        this.bannerUrl = bannerUrl;
        this.appointmentStart = appointmentStart;
    }

    // =====================
    // GETTERS
    // =====================

    public String getId() { return id; }
    public String getServiceTitle() { return serviceTitle; }
    public String getProviderName() { return providerName; }
    public String getLocationArea() { return locationArea; }
    public double getPrice() { return price; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getBannerUrl() { return bannerUrl; }
    public Instant getAppointmentStart() { return appointmentStart; }
    public int getDurationMins() { return durationMins; }

    // =====================
    // SETTERS ✅
    // =====================

    public void setId(String id) {
        this.id = id;
    }

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setLocationArea(String locationArea) {
        this.locationArea = locationArea;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public void setAppointmentStart(Instant appointmentStart) {
        this.appointmentStart = appointmentStart;
    }

    public void setDurationMins(int durationMins) {
        this.durationMins = durationMins;
    }

    // =====================
    // OPTIONAL HELPERS
    // =====================

    public Instant getAppointmentEnd() {
        if (appointmentStart == null) return null;
        return appointmentStart.plusSeconds(durationMins * 60L);
    }
}