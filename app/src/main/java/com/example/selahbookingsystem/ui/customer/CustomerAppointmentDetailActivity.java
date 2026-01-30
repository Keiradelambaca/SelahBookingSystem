package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerAppointmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";

    private ImageView ivBanner;
    private TextView tvTitle, tvProvider, tvDateTime, tvLocation, tvStatus, tvDetails, tvPrice;
    private Chip chipPayment;

    private LinearLayout receiptItemsContainer;
    private TextView tvSubtotal, tvTotal, tvPaymentMethod;
    private MaterialButton btnPay;

    private MaterialButton btnReschedule, btnCancel;
    private View mapPreviewCard;
    private MaterialButton btnMessageProvider;

    private View progress;
    private View content;

    private SupabaseRestService api;

    private String bookingId;
    private BookingDto booking;

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
        tvLocation = findViewById(R.id.tvLocation);
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

        mapPreviewCard = findViewById(R.id.mapPreviewCard);
        btnMessageProvider = findViewById(R.id.btnMessageProvider);

        progress = findViewById(R.id.progress);
        content = findViewById(R.id.content);

        api = ApiClient.get().create(SupabaseRestService.class);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (TextUtils.isEmpty(bookingId)) {
            Toast.makeText(this, "Missing booking id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Buttons (basic V1)
        btnPay.setOnClickListener(v ->
                Toast.makeText(this, "Payment flow (todo)", Toast.LENGTH_SHORT).show());

        btnReschedule.setOnClickListener(v ->
                Toast.makeText(this, "Reschedule (todo)", Toast.LENGTH_SHORT).show());

        btnCancel.setOnClickListener(v ->
                Toast.makeText(this, "Cancel (todo)", Toast.LENGTH_SHORT).show());

        // You don't have lat/lng in BookingDto yet → hide/disable map for now
        mapPreviewCard.setVisibility(View.GONE);

        btnMessageProvider.setOnClickListener(v -> {
            if (booking == null) return;
            Intent intent = new Intent(this, CustomerMessagesActivity.class);
            intent.putExtra("extra_provider_id", booking.provider_id);
            intent.putExtra("extra_provider_name", safe(booking.provider_name));
            startActivity(intent);
        });

        loadBooking();
    }

    private void loadBooking() {
        setLoading(true);

        api.getBookingById("*", "eq." + bookingId)
                .enqueue(new Callback<List<BookingDto>>() {
                    @Override
                    public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            setLoading(false);
                            Toast.makeText(CustomerAppointmentDetailActivity.this,
                                    "Booking not found (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        booking = response.body().get(0);
                        bindBooking(booking);
                        setLoading(false);
                    }

                    @Override
                    public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(CustomerAppointmentDetailActivity.this,
                                "Failed to load booking: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void bindBooking(BookingDto b) {

        // --- Banner image (DTO has inspo_photo_url / current_photo_url) ---
        String imageUrl = firstNonEmpty(b.inspo_photo_url, b.current_photo_url);
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this).load(imageUrl).centerCrop().into(ivBanner);
        } else {
            ivBanner.setImageResource(R.drawable.placeholder_banner);
        }

        // --- Title (no service_name yet, keep placeholder) ---
        tvTitle.setText("Appointment");

        // --- Provider ---
        tvProvider.setText(safe(b.provider_name));

        // --- Date/time + duration ---
        tvDateTime.setText(formatDateTime(b.start_time, b.duration_mins));

        // --- Location (not in booking table yet) ---
        tvLocation.setText("Near you");

        // --- Price (not in booking table yet) ---
        tvPrice.setText("—");

        // --- Status ---
        tvStatus.setText(!TextUtils.isEmpty(b.status) ? b.status : "");

        // --- Payment chip: no payment_status column yet ---
        chipPayment.setText(mapPaymentChipText(b.status));

        // --- details_json (Object) → show as JSON string ---
        tvDetails.setText(detailsToString(b.details_json));

        // Receipt minimal (no prices yet)
        populateReceiptMock();
    }

    private void populateReceiptMock() {
        receiptItemsContainer.removeAllViews();

        addReceiptRow("Service", 0.00);

        tvSubtotal.setText("€0.00");
        tvTotal.setText("€0.00");
        tvPaymentMethod.setText("—");
    }

    private void addReceiptRow(String label, double amount) {
        View row = getLayoutInflater().inflate(R.layout.item_receipt_row, receiptItemsContainer, false);
        TextView tvLabel = row.findViewById(R.id.tvReceiptLabel);
        TextView tvAmount = row.findViewById(R.id.tvReceiptAmount);

        tvLabel.setText(label);
        tvAmount.setText(String.format(Locale.getDefault(), "€%.2f", amount));

        receiptItemsContainer.addView(row);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        content.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private String formatDateTime(String startIso, Integer durationMins) {
        if (TextUtils.isEmpty(startIso)) return "";

        try {
            OffsetDateTime odt = OffsetDateTime.parse(startIso);
            String date = odt.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()));
            String time = odt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));

            if (durationMins != null && durationMins > 0) {
                return date + " • " + time + " • " + durationMins + " mins";
            }
            return date + " • " + time;
        } catch (Exception e) {
            return startIso; // fallback
        }
    }

    private String detailsToString(Object detailsJson) {
        if (detailsJson == null) return "—";
        try {
            // Gson will turn LinkedTreeMap etc into JSON
            return gson.toJson(detailsJson);
        } catch (Exception e) {
            return String.valueOf(detailsJson);
        }
    }

    private String mapPaymentChipText(String status) {
        if (status == null) return "Deposit not paid";

        if ("paid".equalsIgnoreCase(status)) return "Paid";
        if ("deposit_paid".equalsIgnoreCase(status)) return "Deposit paid";
        if ("cash".equalsIgnoreCase(status) || "pay_by_cash".equalsIgnoreCase(status)) return "Pay by cash";

        return "Deposit not paid";
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
}


