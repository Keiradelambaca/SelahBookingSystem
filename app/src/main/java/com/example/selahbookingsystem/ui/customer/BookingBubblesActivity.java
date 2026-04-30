package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.AiBookingAssessmentResponse;
import com.example.selahbookingsystem.data.dto.Analysis;
import com.example.selahbookingsystem.data.dto.PriceEstimateDto;
import com.example.selahbookingsystem.data.dto.ProviderServicePricingDto;
import com.example.selahbookingsystem.data.dto.RecommendedServiceItem;
import com.example.selahbookingsystem.data.model.AiBookingAssessmentRequest;
import com.example.selahbookingsystem.data.model.EditableAiSelection;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.util.PriceCalculator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingBubblesActivity extends AppCompatActivity
        implements AddOnBottomSheet.OnAddOnSelectedListener {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";

    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_TOTAL_PRICE_CENTS = "extra_total_price_cents";

    private static final String TAG = "BookingBubbles";

    private ChipGroup chipGroup;
    private TextView tvEstimated;

    private final LinkedHashMap<String, String> selections = new LinkedHashMap<>();
    private final EditableAiSelection editableSelection = new EditableAiSelection();
    private final List<ProviderServicePricingDto> providerPricing = new ArrayList<>();

    private PriceEstimateDto currentEstimate = new PriceEstimateDto();

    private String providerId;
    private String providerName;
    private String currentUriStr;
    private String inspoUriStr;

    private boolean aiLoaded = false;
    private boolean aiLoading = false;
    private boolean pricingLoaded = false;
    private boolean pricingLoading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_bubbles);

        ApiClient.init(getApplicationContext());

        providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        currentUriStr = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        inspoUriStr = getIntent().getStringExtra(EXTRA_INSPO_URI);

        chipGroup = findViewById(R.id.chipGroup);
        tvEstimated = findViewById(R.id.tvEstimated);
        Button btnAddOn = findViewById(R.id.btnAddOn);
        Button btnContinue = findViewById(R.id.btnContinue);

        // Safe defaults before AI returns
        editableSelection.baseServiceCode = "gel_extensions";
        editableSelection.length = "medium";
        editableSelection.shape = "square";
        editableSelection.designLevel = "low";

        rebuildSelectionsMap();
        renderChips();
        recalculateEstimate();

        loadProviderPricing();
        maybeRunAiAssessment();

        btnAddOn.setOnClickListener(v -> {
            AddOnBottomSheet sheet = AddOnBottomSheet.newInstance(providerId);
            sheet.show(getSupportFragmentManager(), "addOnSheet");
        });

        btnContinue.setOnClickListener(v -> {
            if (providerId == null || providerId.trim().isEmpty()
                    || currentUriStr == null || currentUriStr.trim().isEmpty()) {
                Toast.makeText(this, "Missing booking info. Go back.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!pricingLoaded) {
                Toast.makeText(this, "Still loading provider pricing. Please wait.", Toast.LENGTH_LONG).show();
                return;
            }

            if (editableSelection.baseServiceCode == null || editableSelection.baseServiceCode.trim().isEmpty()) {
                Toast.makeText(this, "Please select a base service.", Toast.LENGTH_LONG).show();
                return;
            }

            if (currentEstimate.total_price_cents <= 0) {
                Toast.makeText(this, "Could not calculate price yet. Please review your selection.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent i = new Intent(this, PickTimeslotActivity.class);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_ID, providerId);
            i.putExtra(PickTimeslotActivity.EXTRA_PROVIDER_NAME, providerName);
            i.putExtra(PickTimeslotActivity.EXTRA_CURRENT_URI, currentUriStr);

            if (inspoUriStr != null && !inspoUriStr.trim().isEmpty()) {
                i.putExtra(PickTimeslotActivity.EXTRA_INSPO_URI, inspoUriStr);
            }

            i.putExtra(PickTimeslotActivity.EXTRA_SELECTED_MAP, serializeSelections(selections));
            i.putExtra(PickTimeslotActivity.EXTRA_EST_MINS, currentEstimate.total_duration_mins);
            i.putExtra(PickTimeslotActivity.EXTRA_TOTAL_PRICE_CENTS, currentEstimate.total_price_cents);

            Log.d(TAG, "currentUrl=" + currentUriStr);
            Log.d(TAG, "inspoUrl=" + inspoUriStr);
            Log.d(TAG, "totalPriceCents=" + currentEstimate.total_price_cents);
            Log.d(TAG, "totalDurationMins=" + currentEstimate.total_duration_mins);

            startActivity(i);
        });
    }

    private void loadProviderPricing() {
        if (pricingLoading || pricingLoaded) return;

        if (providerId == null || providerId.trim().isEmpty()) {
            Log.e("PRICING", "Missing providerId");
            return;
        }

        pricingLoading = true;

        ApiClient.supabase()
                .getProviderServicePricing(
                        "eq." + providerId,
                        "*",
                        "service_category.asc,service_name.asc"
                )
                .enqueue(new Callback<List<ProviderServicePricingDto>>() {
                    @Override
                    public void onResponse(Call<List<ProviderServicePricingDto>> call,
                                           Response<List<ProviderServicePricingDto>> response) {
                        pricingLoading = false;

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e("PRICING", "Failed to load provider pricing: HTTP " + response.code());
                            Toast.makeText(
                                    BookingBubblesActivity.this,
                                    "Failed to load provider pricing",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        providerPricing.clear();
                        providerPricing.addAll(response.body());
                        pricingLoaded = true;

                        Log.d("PRICING", "Loaded provider pricing rows=" + providerPricing.size());
                        recalculateEstimate();
                    }

                    @Override
                    public void onFailure(Call<List<ProviderServicePricingDto>> call, Throwable t) {
                        pricingLoading = false;
                        Log.e("PRICING", "Provider pricing call failed", t);
                        Toast.makeText(
                                BookingBubblesActivity.this,
                                "Network error loading provider pricing",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void maybeRunAiAssessment() {
        if (aiLoaded || aiLoading) return;

        if (providerId == null || providerId.trim().isEmpty()) {
            Log.e("AI_BOOKING", "Missing providerId");
            return;
        }

        if (currentUriStr == null || currentUriStr.trim().isEmpty()) {
            Log.e("AI_BOOKING", "Missing current photo URL");
            return;
        }

        String inspoForRequest = (inspoUriStr != null && !inspoUriStr.trim().isEmpty())
                ? inspoUriStr
                : null;

        Log.d("AI_BOOKING", "providerId=" + providerId);
        Log.d("AI_BOOKING", "current_photo_url=" + currentUriStr);
        Log.d("AI_BOOKING", "inspo_photo_url=" + inspoForRequest);

        aiLoading = true;
        Toast.makeText(this, "Analysing nail photos...", Toast.LENGTH_SHORT).show();

        AiBookingAssessmentRequest request =
                new AiBookingAssessmentRequest(providerId, currentUriStr, inspoForRequest);

        ApiClient.supabase()
                .analyseNailBooking(request)
                .enqueue(new Callback<AiBookingAssessmentResponse>() {
                    @Override
                    public void onResponse(Call<AiBookingAssessmentResponse> call,
                                           Response<AiBookingAssessmentResponse> response) {
                        aiLoading = false;

                        Log.d("AI_BOOKING", "HTTP code=" + response.code());
                        Log.d("AI_BOOKING", "HTTP success=" + response.isSuccessful());

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e("AI_BOOKING", "AI response failed: HTTP " + response.code());
                            Toast.makeText(
                                    BookingBubblesActivity.this,
                                    "AI analysis failed. You can still edit the bubbles manually.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        AiBookingAssessmentResponse ai = response.body();
                        aiLoaded = true;

                        Log.d("AI_BOOKING", "assessment_id=" + ai.assessment_id);
                        applyAiAssessment(ai);
                    }

                    @Override
                    public void onFailure(Call<AiBookingAssessmentResponse> call, Throwable t) {
                        aiLoading = false;
                        Log.e("AI_BOOKING", "AI call failed", t);
                        Toast.makeText(
                                BookingBubblesActivity.this,
                                "Could not analyse images. You can still edit the bubbles manually.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void applyAiAssessment(@Nullable AiBookingAssessmentResponse ai) {
        if (ai == null) return;

        boolean appliedStructured = applyStructuredAnalysis(ai.analysis);

        if (!appliedStructured) {
            applyLegacyRecommendation(ai);
        }

        rebuildSelectionsMap();
        renderChips();
        recalculateEstimate();

        Toast.makeText(this, "AI recommendations loaded", Toast.LENGTH_SHORT).show();
    }

    private boolean applyStructuredAnalysis(@Nullable Analysis analysis) {
        if (analysis == null) return false;

        boolean changed = false;

        if (analysis.base_service_code != null && !analysis.base_service_code.trim().isEmpty()) {
            editableSelection.baseServiceCode = normalizeCode(analysis.base_service_code);
            changed = true;
        }

        if (analysis.addon_codes != null) {
            editableSelection.clearAddons();

            for (String code : analysis.addon_codes) {
                if (code != null && !code.trim().isEmpty()) {
                    editableSelection.addAddon(normalizeCode(code));
                }
            }
            changed = true;
        }

        if (analysis.design_level != null && !analysis.design_level.trim().isEmpty()) {
            editableSelection.designLevel = normalizeValue(analysis.design_level);
            changed = true;
        }

        if (analysis.shape != null && !analysis.shape.trim().isEmpty()) {
            editableSelection.shape = normalizeValue(analysis.shape);
            changed = true;
        }

        if (analysis.length != null && !analysis.length.trim().isEmpty()) {
            editableSelection.length = normalizeValue(analysis.length);
            changed = true;
        }

        if ("xl".equalsIgnoreCase(editableSelection.length)) {
            editableSelection.addAddon("extra_length");
        } else {
            editableSelection.removeAddon("extra_length");
        }

        return changed;
    }

    private void applyLegacyRecommendation(@Nullable AiBookingAssessmentResponse ai) {
        if (ai == null || ai.recommendation == null || ai.recommendation.recommended_services == null) {
            return;
        }

        for (RecommendedServiceItem item : ai.recommendation.recommended_services) {
            if (item == null || item.service_code == null) continue;

            String code = safeUpper(item.service_code);

            switch (code) {
                case "REMOVAL":
                    editableSelection.addAddon("removal");
                    break;

                case "INFILL":
                    editableSelection.baseServiceCode = "biab_infill";
                    break;

                case "OVERLAY":
                    editableSelection.baseServiceCode = "biab_overlay";
                    break;

                case "FULL_SET":
                case "EXTENSIONS":
                    editableSelection.baseServiceCode = "gel_extensions";
                    break;

                case "FRENCH_ADDON":
                    editableSelection.addAddon("french_tip");
                    break;

                case "CHROME_ADDON":
                    editableSelection.addAddon("chrome");
                    break;

                case "OMBRE_ADDON":
                    editableSelection.addAddon("ombre");
                    break;

                case "ART_SIMPLE":
                    editableSelection.designLevel = "medium";
                    break;

                case "ART_DETAILED":
                    editableSelection.designLevel = "high";
                    break;

                case "GEMS_ADDON":
                    editableSelection.addAddon("gems_charms");
                    break;

                case "THREED_ART_ADDON":
                    editableSelection.addAddon("three_d_art");
                    editableSelection.designLevel = "high";
                    break;

                case "LENGTH_SHORT":
                    editableSelection.length = "short";
                    editableSelection.removeAddon("extra_length");
                    break;

                case "LENGTH_MEDIUM":
                    editableSelection.length = "medium";
                    editableSelection.removeAddon("extra_length");
                    break;

                case "LENGTH_LONG":
                    editableSelection.length = "long";
                    editableSelection.removeAddon("extra_length");
                    break;

                case "LENGTH_EXTRA_LONG":
                    editableSelection.length = "xl";
                    editableSelection.addAddon("extra_length");
                    break;

                case "SHAPE_ROUND":
                    editableSelection.shape = "round";
                    break;

                case "SHAPE_SQUARE":
                    editableSelection.shape = "square";
                    break;

                case "SHAPE_ALMOND":
                    editableSelection.shape = "almond";
                    break;

                case "SHAPE_COFFIN":
                    editableSelection.shape = "coffin";
                    break;

                case "SHAPE_STILETTO":
                    editableSelection.shape = "stiletto";
                    break;
            }
        }
    }

    private void rebuildSelectionsMap() {
        selections.clear();

        if (editableSelection.baseServiceCode != null && !editableSelection.baseServiceCode.trim().isEmpty()) {
            selections.put("Service", friendlyServiceName(editableSelection.baseServiceCode));
        }

        if (editableSelection.length != null && !editableSelection.length.trim().isEmpty()) {
            selections.put("Length", friendlyValue(editableSelection.length));
        }

        if (editableSelection.shape != null && !editableSelection.shape.trim().isEmpty()) {
            selections.put("Shape", friendlyValue(editableSelection.shape));
        }

        if (editableSelection.designLevel != null && !editableSelection.designLevel.trim().isEmpty()) {
            selections.put("Design Level", friendlyValue(editableSelection.designLevel));
        }

        if (editableSelection.addonCodes != null) {
            int counter = 1;
            for (String addonCode : editableSelection.addonCodes) {
                if (addonCode == null || addonCode.trim().isEmpty()) continue;
                selections.put("Add-on " + counter, friendlyServiceName(addonCode));
                counter++;
            }
        }
    }

    private void renderChips() {
        chipGroup.removeAllViews();

        for (Map.Entry<String, String> entry : selections.entrySet()) {
            String category = entry.getKey();
            String value = entry.getValue();

            Chip chip = new Chip(this);
            chip.setText(category + ": " + value);
            chip.setCloseIconVisible(true);
            chip.setCheckable(false);

            chip.setOnCloseIconClickListener(v -> {
                removeSelectionByChipCategory(category);
                rebuildSelectionsMap();
                renderChips();
                recalculateEstimate();
            });

            chipGroup.addView(chip);
        }
    }

    private void removeSelectionByChipCategory(String category) {
        if (category == null) return;

        if ("Service".equals(category)) {
            editableSelection.baseServiceCode = null;
            return;
        }

        if ("Length".equals(category)) {
            editableSelection.length = null;
            editableSelection.removeAddon("extra_length");
            return;
        }

        if ("Shape".equals(category)) {
            editableSelection.shape = null;
            return;
        }

        if ("Design Level".equals(category)) {
            editableSelection.designLevel = "low";
            return;
        }

        if (category.startsWith("Add-on ")) {
            String value = selections.get(category);
            String addonCode = codeFromFriendlyServiceName(value);
            editableSelection.removeAddon(addonCode);
        }
    }

    private void recalculateEstimate() {
        if (!pricingLoaded) {
            tvEstimated.setText("Loading provider pricing...");
            return;
        }

        currentEstimate = PriceCalculator.calculate(editableSelection, providerPricing);

        String timeText = prettyDuration(currentEstimate.total_duration_mins);
        String priceText = formatEuro(currentEstimate.total_price_cents);

        if (currentEstimate.missing_service_codes != null && !currentEstimate.missing_service_codes.isEmpty()) {
            tvEstimated.setText(
                    "Estimated: " + timeText + " • " + priceText +
                            "\nSome selected items are not offered by this provider"
            );
        } else {
            tvEstimated.setText("Estimated: " + timeText + " • " + priceText);
        }
    }

    @Override
    public void onAddOnSelected(String category, String value, @Nullable String selectedCode) {
        if (category == null || value == null) return;

        switch (category) {
            case "Base Service":
                editableSelection.baseServiceCode = normalizeCode(selectedCode);
                break;

            case "Add-on":
                editableSelection.toggleAddon(normalizeCode(selectedCode));
                break;

            case "Shape":
                editableSelection.shape = normalizeValue(value);
                break;

            case "Length":
                editableSelection.length = normalizeValue(value);
                if ("xl".equalsIgnoreCase(editableSelection.length)) {
                    editableSelection.addAddon("extra_length");
                } else {
                    editableSelection.removeAddon("extra_length");
                }
                break;

            case "Design Level":
                editableSelection.designLevel = normalizeValue(value);
                break;
        }

        rebuildSelectionsMap();
        renderChips();
        recalculateEstimate();
    }

    public static String serializeSelections(LinkedHashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append(e.getKey())
                    .append("=")
                    .append(e.getValue() == null ? "" : e.getValue().replace(";", ","))
                    .append(";");
        }
        return sb.toString();
    }

    public static LinkedHashMap<String, String> deserializeSelections(String raw) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (raw == null) return out;

        String[] parts = raw.split(";");
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            int idx = p.indexOf('=');
            if (idx <= 0) continue;

            String k = p.substring(0, idx);
            String v = p.substring(idx + 1);
            out.put(k, v);
        }
        return out;
    }

    private String safeUpper(@Nullable String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(@Nullable String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeValue(@Nullable String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private String friendlyValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return "";

        String[] parts = value.replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }

        return sb.toString();
    }

    private String friendlyServiceName(@Nullable String code) {
        if (code == null) return "";

        switch (code) {
            case "biab_overlay": return "BIAB Overlay";
            case "biab_infill": return "BIAB Infill";
            case "gel_polish": return "Gel Polish";
            case "gel_extensions": return "Gel Extensions";
            case "acrylic_extensions": return "Acrylic Extensions";
            case "acrylic_infill": return "Acrylic Infill";
            case "removal": return "Removal";
            case "french_tip": return "French Tip";
            case "chrome": return "Chrome";
            case "ombre": return "Ombre";
            case "gems_charms": return "Gems / Charms";
            case "hand_drawn_art": return "Hand Drawn Art";
            case "three_d_art": return "3D Art";
            case "glitter": return "Glitter";
            case "extra_length": return "Extra Length";
            case "design_medium": return "Medium Design";
            case "design_high": return "High Design";
            default: return friendlyValue(code);
        }
    }

    private String codeFromFriendlyServiceName(@Nullable String name) {
        if (name == null) return "";

        String normalized = name.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "biab overlay": return "biab_overlay";
            case "biab infill": return "biab_infill";
            case "gel polish": return "gel_polish";
            case "gel extensions": return "gel_extensions";
            case "acrylic extensions": return "acrylic_extensions";
            case "acrylic infill": return "acrylic_infill";
            case "removal": return "removal";
            case "french tip": return "french_tip";
            case "chrome": return "chrome";
            case "ombre": return "ombre";
            case "gems / charms": return "gems_charms";
            case "hand drawn art": return "hand_drawn_art";
            case "3d art": return "three_d_art";
            case "glitter": return "glitter";
            case "extra length": return "extra_length";
            case "medium design": return "design_medium";
            case "high design": return "design_high";
            default:
                return normalized.replace(" ", "_");
        }
    }

    private String formatEuro(int cents) {
        return String.format(Locale.getDefault(), "€%.2f", cents / 100.0);
    }

    private String prettyDuration(int mins) {
        if (mins <= 0) return "0 mins";

        int hours = mins / 60;
        int remaining = mins % 60;

        if (hours > 0) {
            return remaining > 0 ? hours + "h " + remaining + "m" : hours + "h";
        }
        return mins + " mins";
    }
}
