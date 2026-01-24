package com.example.selahbookingsystem.adapter;

import java.time.Instant;

public class CustomerAppointment {

    public enum PaymentStatus {
        DEPOSIT_PAID,
        DEPOSIT_NOT_PAID,
        PAID,
        PAY_BY_CASH
    }

    private final String id;
    private final String serviceTitle;
    private final String providerName;
    private final String locationArea;
    private final double price;
    private final PaymentStatus paymentStatus;
    private final String bannerUrl; // can be null
    private final Instant appointmentStart; // use UTC Instant (recommended)

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

    public String getId() { return id; }
    public String getServiceTitle() { return serviceTitle; }
    public String getProviderName() { return providerName; }
    public String getLocationArea() { return locationArea; }
    public double getPrice() { return price; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getBannerUrl() { return bannerUrl; }
    public Instant getAppointmentStart() { return appointmentStart; }
}

