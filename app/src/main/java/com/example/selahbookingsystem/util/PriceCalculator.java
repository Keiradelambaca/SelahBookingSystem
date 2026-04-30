package com.example.selahbookingsystem.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.selahbookingsystem.data.dto.PriceEstimateDto;
import com.example.selahbookingsystem.data.dto.ProviderServicePricingDto;
import com.example.selahbookingsystem.data.model.EditableAiSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PriceCalculator {

    private static final String DESIGN_LOW = "low";
    private static final String DESIGN_MEDIUM = "medium";
    private static final String DESIGN_HIGH = "high";

    private static final String DESIGN_MEDIUM_CODE = "design_medium";
    private static final String DESIGN_HIGH_CODE = "design_high";

    @NonNull
    public static PriceEstimateDto calculate(
            @Nullable EditableAiSelection selection,
            @Nullable List<ProviderServicePricingDto> providerPricing
    ) {
        PriceEstimateDto result = new PriceEstimateDto();

        if (selection == null || providerPricing == null || providerPricing.isEmpty()) {
            return result;
        }

        Map<String, ProviderServicePricingDto> pricingMap = buildPricingMap(providerPricing);
        HashSet<String> added = new HashSet<>();

        // 1. Base service
        if (selection.baseServiceCode != null && !selection.baseServiceCode.trim().isEmpty()) {
            addService(selection.baseServiceCode, pricingMap, added, result);
        }

        // 2. Add-ons
        if (selection.addonCodes != null) {
            for (String addonCode : selection.addonCodes) {
                if (addonCode == null || addonCode.trim().isEmpty()) continue;
                addService(addonCode, pricingMap, added, result);
            }
        }

        // 3. Design level surcharge
        String designLevel = normalize(selection.designLevel);
        if (DESIGN_MEDIUM.equals(designLevel)) {
            addService(DESIGN_MEDIUM_CODE, pricingMap, added, result);
        } else if (DESIGN_HIGH.equals(designLevel)) {
            addService(DESIGN_HIGH_CODE, pricingMap, added, result);
        }
        // low = no extra complexity charge

        result.included_service_codes = new ArrayList<>(added);
        return result;
    }

    @NonNull
    private static Map<String, ProviderServicePricingDto> buildPricingMap(
            @NonNull List<ProviderServicePricingDto> providerPricing
    ) {
        Map<String, ProviderServicePricingDto> map = new HashMap<>();

        for (ProviderServicePricingDto dto : providerPricing) {
            if (dto == null) continue;
            if (dto.service_code == null || dto.service_code.trim().isEmpty()) continue;
            if (!Boolean.TRUE.equals(dto.is_offered)) continue;

            map.put(normalize(dto.service_code), dto);
        }

        return map;
    }

    private static void addService(
            @NonNull String serviceCode,
            @NonNull Map<String, ProviderServicePricingDto> pricingMap,
            @NonNull HashSet<String> added,
            @NonNull PriceEstimateDto result
    ) {
        String normalizedCode = normalize(serviceCode);

        if (normalizedCode.isEmpty()) return;
        if (added.contains(normalizedCode)) return;

        ProviderServicePricingDto dto = pricingMap.get(normalizedCode);

        if (dto == null) {
            if (!result.missing_service_codes.contains(normalizedCode)) {
                result.missing_service_codes.add(normalizedCode);
            }
            return;
        }

        result.total_price_cents += safeInt(dto.price_cents);
        result.total_duration_mins += safeInt(dto.duration_mins);
        added.add(normalizedCode);
    }

    private static int safeInt(@Nullable Integer value) {
        return value == null ? 0 : value;
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}