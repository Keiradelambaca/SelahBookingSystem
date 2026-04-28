package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerAppointmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";

    private ShapeableImageView ivBanner;
    private TextView tvTitle, tvProvider, tvDateTime, tvStatus, tvDetails, tvPrice;
    private Chip chipPayment;

    private LinearLayout receiptItemsContainer;
    private TextView tvSubtotal, tvTotal, tvPaymentMethod;
    private MaterialButton btnPay, btnReschedule, btnCancel, btnMessageProvider;

    private View progress, content;

    private MaterialCardView locationCard;
    private MaterialCardView mapClickableCard;
    private TextView tvLocationTitle, tvLocationSubtitle, tvLocationHint;
    private GoogleMap locationMap;

    private SupabaseRestService api;
    private String bookingId;
    private BookingDto booking;
    private MaterialButton btnPayRemaining;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_appointment_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivBanner = findViewById(R.id.ivBanner);
        tvTitle = findViewById(R.id.tvTitle);
        tvProvider = findViewById(R.id.tvProvider);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvDetails = findViewById(R.id.tvDetails);
        tvPrice = findViewById(R.id.tvPrice);
        chipPayment = findViewById(R.id.chipPayment);

        receiptItemsContainer = findViewById(R.id.receiptItemsContainer);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);

        btnPay = findViewById(R.id.btnPay);
        btnReschedule = findViewById(R.id.btnReschedule);
        btnCancel = findViewById(R.id.btnCancel);
        btnMessageProvider = findViewById(R.id.btnMessageProvider);

        locationCard = findViewById(R.id.locationCard);
        mapClickableCard = findViewById(R.id.mapClickableCard);
        tvLocationTitle = findViewById(R.id.tvLocationTitle);
        tvLocationSubtitle = findViewById(R.id.tvLocationSubtitle);
        tvLocationHint = findViewById(R.id.tvLocationHint);

        progress = findViewById(R.id.progress);
        content = findViewById(R.id.content);

        api = ApiClient.get().create(SupabaseRestService.class);

        btnPayRemaining = findViewById(R.id.btnPayRemaining);

        btnPayRemaining.setOnClickListener(v -> {
            if (booking == null) return;

            int leftToPay = calculateLeftToPay(booking);

            if (leftToPay <= 0) {
                toast("Nothing left to pay");
                return;
            }

            Intent i = new Intent(this, BalancePaymentActivity.class);
            i.putExtra(BalancePaymentActivity.EXTRA_BOOKING_ID, booking.id);
            i.putExtra(BalancePaymentActivity.EXTRA_BALANCE_CENTS, leftToPay);
            i.putExtra(
                    BalancePaymentActivity.EXTRA_TOTAL_CENTS,
                    booking.total_price_cents != null ? booking.total_price_cents : 0
            );
            i.putExtra(BalancePaymentActivity.EXTRA_PROVIDER_NAME, safe(booking.provider_name));
            startActivity(i);
        });

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (TextUtils.isEmpty(bookingId)) {
            toast("Missing booking id");
            finish();
            return;
        }

        initLocationMap();

        btnPay.setOnClickListener(v -> toast("Payment flow not connected here yet"));

        btnReschedule.setOnClickListener(v -> {
            if (booking == null) return;

            Intent i = new Intent(this, CustomerRescheduleActivity.class);
            i.putExtra(CustomerRescheduleActivity.EXTRA_BOOKING_ID, bookingId);
            i.putExtra(
                    CustomerRescheduleActivity.EXTRA_DURATION_MINS,
                    booking.duration_mins != null ? booking.duration_mins : 60
            );
            startActivity(i);
        });

        btnCancel.setOnClickListener(v -> cancelBooking());

        btnMessageProvider.setOnClickListener(v -> {
            if (booking == null) return;

            Intent intent = new Intent(this, CustomerMessagesActivity.class);
            intent.putExtra("extra_provider_id", booking.provider_id);
            intent.putExtra("extra_provider_name", safe(booking.provider_name));
            startActivity(intent);
        });

        loadBooking();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean shouldRefresh = getIntent().getBooleanExtra("refresh", false);

        if (shouldRefresh && !TextUtils.isEmpty(bookingId)) {
            getIntent().removeExtra("refresh");
            loadBooking();
        }
    }

    private int calculateLeftToPay(BookingDto b) {
        if (b == null) return 0;

        int total = b.total_price_cents != null ? b.total_price_cents : 0;
        int deposit = calculateDeposit(b, total);

        String paymentStatus = safe(b.payment_status).toLowerCase(Locale.ROOT);

        boolean fullyPaid =
                "fully_paid".equals(paymentStatus)
                        || "complete".equals(paymentStatus)
                        || "completed".equals(paymentStatus)
                        || "paid_in_full".equals(paymentStatus);

        if (fullyPaid) return 0;

        boolean depositPaid =
                "paid".equals(paymentStatus)
                        || "deposit_paid".equals(paymentStatus);

        if (depositPaid && deposit > 0) {
            return Math.max(total - deposit, 0);
        }

        return total;
    }

    private void updatePayRemainingButton(BookingDto b) {
        if (btnPayRemaining == null) return;

        int leftToPay = calculateLeftToPay(b);

        String paymentStatus = safe(b.payment_status).toLowerCase(Locale.ROOT);

        boolean shouldShow =
                leftToPay > 0
                        && (
                        "paid".equals(paymentStatus)
                                || "deposit_paid".equals(paymentStatus)
                                || "confirmed".equalsIgnoreCase(safe(b.status))
                );

        btnPayRemaining.setVisibility(shouldShow ? View.VISIBLE : View.GONE);

        if (shouldShow) {
            btnPayRemaining.setText("Pay remaining balance " + formatCents(leftToPay));
        }
    }

    private void openRemainingBalanceCheckout(String bookingId, int amountCents) {
        if (TextUtils.isEmpty(bookingId)) {
            toast("Missing booking id");
            return;
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("booking_id", bookingId);
        payload.put("amount_cents", amountCents);

        api.fnCreateBalanceCheckout(payload)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            toast("Could not start payment (" + response.code() + ")");
                            return;
                        }

                        Object url = response.body().get("url");

                        if (url == null || TextUtils.isEmpty(String.valueOf(url))) {
                            toast("Missing Stripe checkout URL");
                            return;
                        }

                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.valueOf(url)));
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        toast("Network error starting payment");
                    }
                });
    }

    private void loadBooking() {
        setLoading(true);

        String select =
                "id,client_id,provider_id,provider_name,start_time,end_time,created_at," +
                        "status,duration_mins,details_json,current_photo_url,inspo_photo_url," +
                        "total_price_cents,deposit_required,deposit_percent,deposit_amount_cents," +
                        "payment_status,payment_provider,payment_ref," +
                        "provider:profiles!bookings_provider_id_fkey(business_name,full_name,eircode,address,lat,lng)";

        api.getBookingById(select, "eq." + bookingId)
                .enqueue(new Callback<List<BookingDto>>() {
                    @Override
                    public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                        setLoading(false);

                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            toast("Booking not found (" + response.code() + ")");
                            finish();
                            return;
                        }

                        booking = response.body().get(0);
                        bindBooking(booking);
                    }

                    @Override
                    public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                        setLoading(false);
                        toast("Failed to load booking");
                        finish();
                    }
                });
    }

    private void bindBooking(BookingDto b) {
        String imageUrl = firstNonEmpty(b.inspo_photo_url, b.current_photo_url);

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_banner)
                    .error(R.drawable.placeholder_banner)
                    .into(ivBanner);
        } else {
            ivBanner.setImageResource(R.drawable.placeholder_banner);
        }

        tvTitle.setText(firstNonEmpty(
                getDisplayServiceFromDetails(b.details_json),
                getServiceFromDetails(b.details_json),
                "Nail Appointment"
        ));

        tvProvider.setText(!TextUtils.isEmpty(b.provider_name)
                ? "With " + b.provider_name
                : "Provider");

        tvDateTime.setText(formatDateTime(b.start_time, b.end_time, b.duration_mins));
        tvStatus.setText("Appointment Status: " + formatStatusCapitalised(b.status));

        int total = b.total_price_cents != null ? b.total_price_cents : 0;
        int deposit = calculateDeposit(b, total);

        tvPrice.setText(formatCents(total));
        chipPayment.setText(mapPaymentChipText(b.payment_status, b.status, total, deposit));

        tvDetails.setText(detailsToDisplayString(b.details_json));

        populateReceipt(b);
        bindLocationCard(b);

        boolean needsPayment =
                "requires_payment".equalsIgnoreCase(safe(b.payment_status)) ||
                        "payment_pending".equalsIgnoreCase(safe(b.status));

        btnPay.setVisibility(needsPayment ? View.VISIBLE : View.GONE);
        updatePayRemainingButton(b);
    }

    private int calculateDeposit(BookingDto b, int appointmentTotal) {
        if (b.deposit_amount_cents != null) {
            return b.deposit_amount_cents;
        }

        if (b.deposit_required != null && b.deposit_required
                && b.deposit_percent != null
                && appointmentTotal > 0) {
            return Math.round(appointmentTotal * (b.deposit_percent / 100f));
        }

        return 0;
    }

    private void populateReceipt(BookingDto b) {
        receiptItemsContainer.removeAllViews();

        int total = b.total_price_cents != null ? b.total_price_cents : 0;
        int deposit = calculateDeposit(b, total);

        String paymentStatus = safe(b.payment_status).toLowerCase(Locale.ROOT);

        boolean fullyPaid =
                "fully_paid".equals(paymentStatus)
                        || "complete".equals(paymentStatus)
                        || "completed".equals(paymentStatus)
                        || "paid_in_full".equals(paymentStatus);

        boolean depositPaid =
                "paid".equals(paymentStatus)
                        || "deposit_paid".equals(paymentStatus);

        int leftToPay = calculateLeftToPay(b);

        addReceiptRow("Appointment total", formatCents(total));

        if (deposit > 0) {
            addReceiptRow(
                    depositPaid ? "Deposit paid" : "Deposit due",
                    "- " + formatCents(deposit)
            );
        } else {
            addReceiptRow("Deposit", "Not required");
        }

        tvSubtotal.setText("Appointment total minus deposit");
        tvTotal.setText("Left to pay: " + formatCents(leftToPay));

        tvPaymentMethod.setText("Payment status: " +
                mapPaymentChipText(b.payment_status, b.status, total, deposit));
    }

    private void addReceiptRow(String label, String amountText) {
        View row = getLayoutInflater().inflate(R.layout.item_receipt_row, receiptItemsContainer, false);

        TextView tvLabel = row.findViewById(R.id.tvReceiptLabel);
        TextView tvAmount = row.findViewById(R.id.tvReceiptAmount);

        tvLabel.setText(label);
        tvAmount.setText(amountText);

        receiptItemsContainer.addView(row);
    }

    private void initLocationMap() {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.locationMapContainer, mapFragment)
                .commit();

        mapFragment.getMapAsync(gMap -> {
            locationMap = gMap;

            locationMap.getUiSettings().setAllGesturesEnabled(false);
            locationMap.getUiSettings().setMapToolbarEnabled(false);
            locationMap.getUiSettings().setCompassEnabled(false);

            locationMap.setOnMapClickListener(latLng -> openGoogleMapsLocation(booking));
            locationMap.setOnMarkerClickListener(marker -> {
                openGoogleMapsLocation(booking);
                return true;
            });

            if (booking != null) {
                bindLocationCard(booking);
            }
        });
    }

    private void bindLocationCard(BookingDto b) {
        if (locationCard == null) return;

        if (b == null || b.provider == null) {
            locationCard.setVisibility(View.GONE);
            return;
        }

        locationCard.setVisibility(View.VISIBLE);

        String businessName = firstNonEmpty(
                b.provider.business_name,
                b.provider.full_name,
                b.provider_name,
                "Appointment location"
        );

        String address = safe(b.provider.address);
        String eircode = safe(b.provider.eircode);

        tvLocationTitle.setText(businessName);
        tvLocationSubtitle.setText(firstNonEmpty(address, eircode, "Location details unavailable"));
        tvLocationHint.setText("Tap the map to open in Google Maps");

        Double lat = b.provider.lat;
        Double lng = b.provider.lng;

        boolean hasCoords = lat != null && lng != null && lat != 0 && lng != 0;

        if (hasCoords && locationMap != null) {
            LatLng pos = new LatLng(lat, lng);

            locationMap.clear();
            locationMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(businessName));

            locationMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        }

        locationCard.setOnClickListener(v -> openGoogleMapsLocation(b));
        mapClickableCard.setOnClickListener(v -> openGoogleMapsLocation(b));
    }

    private void openGoogleMapsLocation(BookingDto b) {
        if (b == null || b.provider == null) return;

        Double lat = b.provider.lat;
        Double lng = b.provider.lng;

        if (lat != null && lng != null && lat != 0 && lng != 0) {
            Uri appUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng);
            Intent appIntent = new Intent(Intent.ACTION_VIEW, appUri);
            appIntent.setPackage("com.google.android.apps.maps");

            try {
                startActivity(appIntent);
                return;
            } catch (Exception ignored) {
            }

            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng)
            );
            startActivity(browserIntent);
            return;
        }

        String query = firstNonEmpty(
                b.provider.address,
                b.provider.eircode,
                b.provider.business_name,
                b.provider.full_name
        );

        if (TextUtils.isEmpty(query)) {
            toast("Location unavailable");
            return;
        }

        Intent browserIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
        );
        startActivity(browserIntent);
    }

    private void cancelBooking() {
        if (booking == null) return;

        SupabaseRestService.BookingUpdateBody body =
                new SupabaseRestService.BookingUpdateBody("cancelled", null, null);

        api.updateBooking("eq." + bookingId, body)
                .enqueue(new Callback<List<BookingDto>>() {
                    @Override
                    public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                        if (response.isSuccessful()) {
                            toast("Cancelled");

                            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
                            payload.put("booking_id", bookingId);

                            api.fnBookingCancelledEmail(payload).enqueue(new Callback<java.util.Map<String, Object>>() {
                                @Override
                                public void onResponse(Call<java.util.Map<String, Object>> c, Response<java.util.Map<String, Object>> r) {}

                                @Override
                                public void onFailure(Call<java.util.Map<String, Object>> c, Throwable t) {}
                            });

                            loadBooking();
                        } else {
                            toast("Cancel failed (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                        toast("Network error");
                    }
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        content.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private String formatDateTime(String startIso, String endIso, Integer durationMins) {
        if (TextUtils.isEmpty(startIso)) return "Time TBC";

        try {
            OffsetDateTime start = OffsetDateTime.parse(startIso);

            String date = start.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()));
            String startTime = start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));

            if (!TextUtils.isEmpty(endIso)) {
                OffsetDateTime end = OffsetDateTime.parse(endIso);
                String endTime = end.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
                return date + " • " + startTime + " - " + endTime;
            }

            if (durationMins != null && durationMins > 0) {
                return date + " • " + startTime + " • " + durationMins + " mins";
            }

            return date + " • " + startTime;
        } catch (Exception e) {
            return startIso;
        }
    }

    private String detailsToDisplayString(Object detailsJson) {
        if (detailsJson == null) return "No extra details added.";

        try {
            if (!(detailsJson instanceof Map)) {
                return "Booking details unavailable.";
            }

            Map<?, ?> details = (Map<?, ?>) detailsJson;
            StringBuilder sb = new StringBuilder();

            Object displaySelectionsObj = details.get("display_selections");

            sb.append("Booking breakdown\n\n");

            if (displaySelectionsObj instanceof Map) {
                Map<?, ?> selections = (Map<?, ?>) displaySelectionsObj;

                appendSelectionLine(sb, selections, "Service");
                appendSelectionLine(sb, selections, "Length");
                appendSelectionLine(sb, selections, "Shape");
                appendSelectionLine(sb, selections, "Design Level");

                boolean hasAddons = false;

                for (Map.Entry<?, ?> entry : selections.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    String value = String.valueOf(entry.getValue());

                    if (key.startsWith("Add-on ") && !TextUtils.isEmpty(value)) {
                        if (!hasAddons) {
                            sb.append("\nAdd-ons\n");
                            hasAddons = true;
                        }

                        sb.append("• ").append(value).append("\n");
                    }
                }
            }

            Object pricingObj = details.get("pricing_snapshot");

            if (pricingObj instanceof Map) {
                Map<?, ?> pricing = (Map<?, ?>) pricingObj;

                Integer durationMins = numberToInt(pricing.get("total_duration_mins"));
                Integer totalPriceCents = numberToInt(pricing.get("total_price_cents"));

                sb.append("\n");

                if (durationMins != null && durationMins > 0) {
                    sb.append("Estimated duration: ")
                            .append(prettyDuration(durationMins))
                            .append("\n");
                }

                if (totalPriceCents != null && totalPriceCents > 0) {
                    sb.append("Estimated total: ")
                            .append(formatCents(totalPriceCents))
                            .append("\n");
                }
            }

            String result = sb.toString().trim();
            return TextUtils.isEmpty(result) ? "No extra details added." : result;

        } catch (Exception e) {
            return "Booking details unavailable.";
        }
    }

    private void appendSelectionLine(StringBuilder sb, Map<?, ?> selections, String key) {
        Object value = selections.get(key);

        if (value == null) return;

        String text = String.valueOf(value);

        if (TextUtils.isEmpty(text)) return;

        sb.append(key)
                .append(": ")
                .append(text)
                .append("\n");
    }

    private Integer numberToInt(Object value) {
        if (value == null) return null;

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
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

    private String getDisplayServiceFromDetails(Object detailsJson) {
        if (detailsJson instanceof Map) {
            Object displaySelections = ((Map<?, ?>) detailsJson).get("display_selections");

            if (displaySelections instanceof Map) {
                Object service = ((Map<?, ?>) displaySelections).get("Service");
                if (service != null) return String.valueOf(service);
            }
        }

        return "";
    }

    private String getServiceFromDetails(Object detailsJson) {
        if (detailsJson instanceof Map) {
            Object value = ((Map<?, ?>) detailsJson).get("base_service_code");
            if (value != null) return prettify(String.valueOf(value));
        }

        return "";
    }

    private void appendDetail(StringBuilder sb, Map<?, ?> map, String key, String label) {
        Object value = map.get(key);
        if (value == null) return;

        String text = String.valueOf(value);
        if (TextUtils.isEmpty(text)) return;

        sb.append(label)
                .append(": ")
                .append(prettify(text))
                .append("\n");
    }

    private String prettify(String value) {
        if (value == null) return "";

        return value
                .replace("[", "")
                .replace("]", "")
                .replace("_", " ")
                .replace(",", ", ")
                .trim();
    }

    private String formatStatusCapitalised(String status) {
        if (TextUtils.isEmpty(status)) return "Unknown";

        String pretty = status.replace("_", " ").trim();
        return pretty.substring(0, 1).toUpperCase() + pretty.substring(1).toLowerCase();
    }

    private String formatCents(Integer cents) {
        if (cents == null) return "€0.00";
        return String.format(Locale.getDefault(), "€%.2f", cents / 100.0);
    }

    private String mapPaymentChipText(String paymentStatus, String bookingStatus, int total, int deposit) {
        String value = !TextUtils.isEmpty(paymentStatus) ? paymentStatus : bookingStatus;
        String s = safe(value).toLowerCase(Locale.ROOT);

        if ("fully_paid".equals(s)
                || "complete".equals(s)
                || "completed".equals(s)
                || "paid_in_full".equals(s)) {
            return "Paid";
        }

        if (deposit <= 0 || "not_required".equals(s)) {
            return "Deposit not required";
        }

        if ("paid".equals(s) || "deposit_paid".equals(s)) {
            return "Deposit paid";
        }

        return "Deposit required";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) return "";

        for (String v : vals) {
            if (!TextUtils.isEmpty(v)) return v;
        }

        return "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
