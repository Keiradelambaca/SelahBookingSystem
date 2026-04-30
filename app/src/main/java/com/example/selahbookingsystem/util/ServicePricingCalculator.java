package com.example.selahbookingsystem.util;

import com.example.selahbookingsystem.data.dto.ServiceDto;
import com.example.selahbookingsystem.data.dto.ServiceModifierDto;

import java.util.List;

public class ServicePricingCalculator {

    public static Result calculate(ServiceDto service, List<ServiceModifierDto> selected) {
        int price = safe(service.base_price_cents);
        int mins  = safe(service.base_duration_mins);

        if (selected != null) {
            for (ServiceModifierDto m : selected) {
                price += safe(m.price_delta_cents);
                mins  += safe(m.duration_delta_mins);
            }
        }

        // Guardrails
        if (price < 0) price = 0;
        if (mins < 15) mins = 15;

        return new Result(price, mins);
    }

    private static int safe(Integer v) { return v == null ? 0 : v; }

    public static class Result {
        public final int totalPriceCents;
        public final int totalDurationMins;

        public Result(int totalPriceCents, int totalDurationMins) {
            this.totalPriceCents = totalPriceCents;
            this.totalDurationMins = totalDurationMins;
        }
    }
}
