package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.Locale;

public class CustomerAppointmentDetailActivity extends AppCompatActivity {

    // Intent extras
    public static final String EXTRA_APPT_ID = "extra_appt_id";
    public static final String EXTRA_BANNER_URL = "extra_banner_url";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_PROVIDER = "extra_provider";
    public static final String EXTRA_DATETIME = "extra_datetime"; // already formatted string for now
    public static final String EXTRA_LOCATION_AREA = "extra_location_area";
    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_PAYMENT_STATUS = "extra_payment_status";
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";

    private ImageView ivBanner;
    private TextView tvTitle, tvProvider, tvDateTime, tvPrice, tvLocation;
    private Chip chipPayment;

    private LinearLayout receiptItemsContainer;
    private TextView tvSubtotal, tvTotal, tvPaymentMethod;
    private MaterialButton btnPay;

    private MaterialButton btnReschedule, btnCancel;
    private View mapPreviewCard;
    private MaterialButton btnMessageProvider;
    private TextView tvTerms;

    private double lat = 0;
    private double lng = 0;

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
        tvPrice = findViewById(R.id.tvPrice);
        tvLocation = findViewById(R.id.tvLocation);
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
        tvTerms = findViewById(R.id.tvTerms);

        // ---- Get data from intent (for now) ----
        Intent i = getIntent();
        String apptId = i.getStringExtra(EXTRA_APPT_ID);

        String bannerUrl = i.getStringExtra(EXTRA_BANNER_URL);
        String title = i.getStringExtra(EXTRA_TITLE);
        String provider = i.getStringExtra(EXTRA_PROVIDER);
        String datetime = i.getStringExtra(EXTRA_DATETIME);
        String locationArea = i.getStringExtra(EXTRA_LOCATION_AREA);
        double price = i.getDoubleExtra(EXTRA_PRICE, 0);
        String paymentStatus = i.getStringExtra(EXTRA_PAYMENT_STATUS);

        lat = i.getDoubleExtra(EXTRA_LAT, 0);
        lng = i.getDoubleExtra(EXTRA_LNG, 0);

        // ---- Bind top section ----
        tvTitle.setText(title != null ? title : "Appointment");
        tvProvider.setText(provider != null ? provider : "");
        tvDateTime.setText(datetime != null ? datetime : "");
        tvLocation.setText(locationArea != null ? locationArea : "");
        tvPrice.setText(String.format(Locale.getDefault(), "€%.2f", price));

        if (paymentStatus != null) chipPayment.setText(paymentStatus);

        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            Glide.with(this).load(bannerUrl).centerCrop().into(ivBanner);
        } else {
            ivBanner.setImageResource(R.drawable.placeholder_banner);
        }

        // ---- Receipt mock (replace with real add-ons later) ----
        // You can remove this once you load from backend using apptId.
        populateReceiptMock(price);

        // ---- Pay button logic (basic V1) ----
        // Example: if Deposit not paid -> show "Pay deposit", else if not paid -> "Pay now"
        String payText = "Pay now";
        if ("Deposit not paid".equalsIgnoreCase(paymentStatus)) payText = "Pay deposit";
        btnPay.setText(payText);

        btnPay.setOnClickListener(v -> {
            // TODO: Integrate Stripe / payment flow later
        });

        // ---- Reschedule / Cancel (later) ----
        btnReschedule.setOnClickListener(v -> {
            // TODO: open reschedule UI
        });
        btnCancel.setOnClickListener(v -> {
            // TODO: open cancel confirmation
        });

        // ---- Map preview click -> full map page ----
        mapPreviewCard.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, CustomerAppointmentMapActivity.class);
            mapIntent.putExtra(CustomerAppointmentMapActivity.EXTRA_LAT, lat);
            mapIntent.putExtra(CustomerAppointmentMapActivity.EXTRA_LNG, lng);
            mapIntent.putExtra(CustomerAppointmentMapActivity.EXTRA_TITLE,
                    (title != null ? title : "Appointment location"));
            startActivity(mapIntent);
        });

        // ---- Message provider ----
        btnMessageProvider.setOnClickListener(v -> {
            // TODO: navigate to messages thread with provider
        });

        // ---- Terms ----
        tvTerms.setText(
                "• Cancellations within 24 hours may forfeit your deposit.\n" +
                        "• Arrive on time. Late arrivals may reduce service time.\n" +
                        "• If you need to reschedule, please do so at least 24 hours in advance."
        );
    }

    private void populateReceiptMock(double basePrice) {
        receiptItemsContainer.removeAllViews();

        // Mock add-ons
        addReceiptRow("Base service", basePrice);
        addReceiptRow("Add-on: Nail art", 10.00);
        addReceiptRow("Add-on: Extra length", 5.00);

        double subtotal = basePrice + 10 + 5;
        double total = subtotal;

        tvSubtotal.setText(String.format(Locale.getDefault(), "€%.2f", subtotal));
        tvTotal.setText(String.format(Locale.getDefault(), "€%.2f", total));
        tvPaymentMethod.setText("Card •••• 4242"); // replace later from booking
    }

    private void addReceiptRow(String label, double amount) {
        View row = getLayoutInflater().inflate(R.layout.item_receipt_row, receiptItemsContainer, false);
        TextView tvLabel = row.findViewById(R.id.tvReceiptLabel);
        TextView tvAmount = row.findViewById(R.id.tvReceiptAmount);

        tvLabel.setText(label);
        tvAmount.setText(String.format(Locale.getDefault(), "€%.2f", amount));

        receiptItemsContainer.addView(row);
    }
}
