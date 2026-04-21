package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingConfirmedActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmed);

        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (TextUtils.isEmpty(bookingId)) {
            finish();
            return;
        }

        ImageView ivLook = findViewById(R.id.ivBookedLook);
        TextView tvProvider = findViewById(R.id.tvConfirmedProvider);
        TextView tvTime = findViewById(R.id.tvConfirmedTime);
        TextView tvStatus = findViewById(R.id.tvConfirmedStatus);
        TextView tvDetails = findViewById(R.id.tvConfirmedDetails);

        Button btnHome = findViewById(R.id.btnBackHome);
        Button btnMessage = findViewById(R.id.btnMessageProvider);

        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, CustomerHomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);
        api.getBookingById("*", "eq." + bookingId).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    finish();
                    return;
                }

                BookingDto b = response.body().get(0);

                String img = !isBlank(b.inspo_photo_url) ? b.inspo_photo_url : b.current_photo_url;
                if (!isBlank(img)) {
                    Glide.with(BookingConfirmedActivity.this).load(img).into(ivLook);
                }

                tvProvider.setText(!isBlank(b.provider_name) ? b.provider_name : "Provider");
                tvStatus.setText(!isBlank(b.status) ? b.status : "Confirmed");
                tvTime.setText(formatBookingTime(b.start_time, b.duration_mins));
                tvDetails.setText(buildBookingDetailsText(b));

                btnMessage.setOnClickListener(v -> {
                    Intent i = new Intent(BookingConfirmedActivity.this, CustomerMessagesActivity.class);
                    i.putExtra("extra_provider_id", b.provider_id);
                    i.putExtra("extra_provider_name", b.provider_name);
                    startActivity(i);
                });
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                Toast.makeText(BookingConfirmedActivity.this, "Network error", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private String formatBookingTime(String startTime, int durationMins) {
        if (isBlank(startTime)) {
            return durationMins > 0 ? durationMins + " mins" : "";
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant s = Instant.parse(startTime);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm", Locale.getDefault())
                        .withZone(ZoneId.systemDefault());

                if (durationMins > 0) {
                    return fmt.format(s) + " (" + durationMins + " mins)";
                }
                return fmt.format(s);
            }
        } catch (Exception ignored) {
        }

        return startTime + (durationMins > 0 ? " (" + durationMins + " mins)" : "");
    }

    private String buildBookingDetailsText(BookingDto b) {
        List<String> lines = new ArrayList<>();

        Map<String, Object> details = asMap(b.details_json);
        if (details == null || details.isEmpty()) {
            return "No additional booking details.";
        }

        String serviceName = firstNonBlank(
                getString(details, "service_name"),
                getString(details, "service"),
                getString(details, "service_code")
        );

        String shape = firstNonBlank(
                getString(details, "shape"),
                getString(details, "selected_shape")
        );

        String length = firstNonBlank(
                getString(details, "length"),
                getString(details, "selected_length")
        );

        String designLevel = firstNonBlank(
                getString(details, "design_level"),
                getString(details, "design"),
                getString(details, "design_style")
        );

        String addonCodes = getCommaJoined(details, "addon_codes");

        Boolean handDrawn = getBoolean(details, "hand_drawn");
        String handDrawnText = null;
        if (handDrawn != null) {
            handDrawnText = handDrawn ? "Yes" : "No";
        }

        Double totalPrice = firstNonNullDouble(
                getDouble(details, "total_price"),
                getDouble(details, "price"),
                getNestedDouble(details, "pricing_snapshot", "total_price")
        );

        if (!isBlank(serviceName)) {
            lines.add("Service: " + prettifyCode(serviceName));
        }

        if (!isBlank(shape)) {
            lines.add("Shape: " + prettifyCode(shape));
        }

        if (!isBlank(length)) {
            lines.add("Length: " + prettifyCode(length));
        }

        if (!isBlank(designLevel)) {
            lines.add("Design: " + prettifyCode(designLevel));
        }

        if (!isBlank(handDrawnText)) {
            lines.add("Hand-drawn art: " + handDrawnText);
        }

        if (!isBlank(addonCodes)) {
            lines.add("Add-ons: " + prettifyCsvCodes(addonCodes));
        }

        if (totalPrice != null && totalPrice > 0) {
            lines.add(String.format(Locale.getDefault(), "Estimated total: €%.2f", totalPrice));
        }

        if (lines.isEmpty()) {
            return "No additional booking details.";
        }

        return TextUtils.join("\n", lines);
    }

    @Nullable
    private Map<String, Object> asMap(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Map) {
            try {
                //noinspection unchecked
                return (Map<String, Object>) obj;
            } catch (Exception ignored) {
            }
        }

        try {
            String json = new Gson().toJson(obj);
            return new Gson().fromJson(json, LinkedTreeMap.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String getString(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    @Nullable
    private Boolean getBoolean(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String s = ((String) value).trim();
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        return null;
    }

    @Nullable
    private Double getDouble(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) return null;
        Object value = map.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Nullable
    private Double getNestedDouble(Map<String, Object> map, String parentKey, String childKey) {
        if (map == null || !map.containsKey(parentKey)) return null;

        Object parent = map.get(parentKey);
        if (!(parent instanceof Map)) return null;

        try {
            //noinspection unchecked
            Map<String, Object> nested = (Map<String, Object>) parent;
            return getDouble(nested, childKey);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCommaJoined(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) return null;

        Object value = map.get(key);
        if (value == null) return null;

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> parts = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                        parts.add(s);
                    }
                }
            }
            return parts.isEmpty() ? null : TextUtils.join(", ", parts);
        }

        String s = String.valueOf(value).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    @Nullable
    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    @Nullable
    private Double firstNonNullDouble(Double... values) {
        if (values == null) return null;
        for (Double v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private String prettifyCode(String raw) {
        if (isBlank(raw)) return "";
        String s = raw.replace("_", " ").replace("-", " ").trim();

        String[] words = s.split("\\s+");
        StringBuilder out = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(" ");
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(Locale.getDefault()));
            }
        }
        return out.toString();
    }

    private String prettifyCsvCodes(String csv) {
        if (isBlank(csv)) return "";
        String[] parts = csv.split(",");
        List<String> pretty = new ArrayList<>();
        for (String p : parts) {
            String cleaned = prettifyCode(p.trim());
            if (!cleaned.isEmpty()) {
                pretty.add(cleaned);
            }
        }
        return TextUtils.join(", ", pretty);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
