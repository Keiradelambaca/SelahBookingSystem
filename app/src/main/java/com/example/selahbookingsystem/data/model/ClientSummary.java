package com.example.selahbookingsystem.data.model;

public class ClientSummary {
    public String clientId;
    public String fullName;
    public String email;
    public String phone;

    public int timesBooked;
    public String lastAppointmentText;   // already formatted for UI
    public String paymentText;           // e.g. "Saved card: Visa •••• 4242"

    public boolean expanded;             // UI-only

    public ClientSummary(String clientId) {
        this.clientId = clientId;
    }
}

