package com.example.selahbookingsystem.ui.customer;

import com.example.selahbookingsystem.adapter.CustomerAppointment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class MockAppointments {

    public static CustomerAppointment createInHours(int hours) {
        return new CustomerAppointment(
                UUID.randomUUID().toString(),
                "Gel Nails (Full Set)",
                "Selah Beauty",
                "Dublin 2",
                55.00,
                CustomerAppointment.PaymentStatus.DEPOSIT_PAID,
                null,
                Instant.now().plus(hours, ChronoUnit.HOURS)
        );
    }

    public static CustomerAppointment createInDays(int days) {
        return new CustomerAppointment(
                UUID.randomUUID().toString(),
                "Lash Extensions",
                "Glow Studio",
                "Dundrum",
                70.00,
                CustomerAppointment.PaymentStatus.PAY_BY_CASH,
                null,
                Instant.now().plus(days, ChronoUnit.DAYS)
        );
    }
}