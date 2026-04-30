package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepositPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_DEPOSIT_CENTS = "extra_deposit_cents";
    public static final String EXTRA_TOTAL_CENTS = "extra_total_cents";
    public static final String EXTRA_DEPOSIT_PERCENT = "extra_deposit_percent";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name"; // optional

    private SupabaseRestService api;

    private String bookingId;
    private String providerName;

    private int depositCents;
    private int totalCents;
    private int percent;

    private boolean startedCheckout = false;

    private MaterialButton btnPay;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pollAttempts = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit_payment);

        api = ApiClient.get().create(SupabaseRestService.class);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);

        depositCents = getIntent().getIntExtra(EXTRA_DEPOSIT_CENTS, 0);
        totalCents   = getIntent().getIntExtra(EXTRA_TOTAL_CENTS, 0);
        percent      = getIntent().getIntExtra(EXTRA_DEPOSIT_PERCENT, 0);

        TextView tvInfo = findViewById(R.id.tvDepositInfo);
        btnPay = findViewById(R.id.btnPayDeposit);
        MaterialButton btnCancel = findViewById(R.id.btnCancelPayment);

        tvInfo.setText(String.format(Locale.getDefault(),
                "Deposit required: %d%%\nDeposit: €%.2f\nTotal: €%.2f\n\nPay your deposit to confirm the booking.",
                percent, depositCents / 100.0, totalCents / 100.0));

        btnCancel.setOnClickListener(v -> finish());

        btnPay.setOnClickListener(v -> {
            if (TextUtils.isEmpty(bookingId)) {
                toast("Missing booking id");
                return;
            }

            // Important: avoid calling function with 0 cents
            if (depositCents <= 0) {
                Log.e("DEPOSIT", "depositCents is <= 0. totalCents=" + totalCents + " percent=" + percent);
                toast("Deposit amount is missing. Please go back and try again.");
                return;
            }

            btnPay.setEnabled(false);
            createCheckoutAndOpen();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If user comes back from Stripe, start polling
        if (startedCheckout) {
            pollAttempts = 0;
            pollBookingStatus();
        }
    }

    private void createCheckoutAndOpen() {
        Log.d("DEPOSIT", "Creating checkout for bookingId=" + bookingId
                + " depositCents=" + depositCents + " totalCents=" + totalCents + " percent=" + percent);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("booking_id", bookingId);
        payload.put("deposit_amount_cents", depositCents);
        if (!TextUtils.isEmpty(providerName)) payload.put("provider_name", providerName);

        api.fnCreateDepositCheckout(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                if (!resp.isSuccessful()) {
                    try {
                        String err = resp.errorBody() != null ? resp.errorBody().string() : "(no body)";
                        Log.e("DEPOSIT", "Checkout failed " + resp.code() + " => " + err);
                    } catch (Exception ignored) {}
                    toast("Payment setup failed (" + resp.code() + ")");
                    btnPay.setEnabled(true);
                    return;
                }

                Map<String, Object> body = resp.body();
                if (body == null) {
                    toast("Missing response body");
                    btnPay.setEnabled(true);
                    return;
                }

                Object urlObj = body.get("url");
                if (urlObj == null) {
                    toast("Missing checkout URL");
                    btnPay.setEnabled(true);
                    return;
                }

                String url = String.valueOf(urlObj);
                startedCheckout = true;

                CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
                intent.launchUrl(DepositPaymentActivity.this, Uri.parse(url));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("DEPOSIT", "Network error creating checkout", t);
                toast("Network error");
                btnPay.setEnabled(true);
            }
        });
    }

    private void pollBookingStatus() {
        pollAttempts++;
        if (pollAttempts > 60) {
            toast("Still waiting for payment confirmation… If you paid, come back in a moment.");
            btnPay.setEnabled(true);
            return;
        }

        api.getBookingById("id,status,payment_status", "eq." + bookingId).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) {
                    scheduleNextPoll();
                    return;
                }

                BookingDto b = resp.body().get(0);

                String pay = (b.payment_status == null) ? "" : b.payment_status.trim();
                String st  = (b.status == null) ? "" : b.status.trim();

                Log.d("DEPOSIT", "Poll #" + pollAttempts + " => status=" + st + " payment_status=" + pay);

                boolean paid =
                        "paid".equalsIgnoreCase(pay) ||
                                "succeeded".equalsIgnoreCase(pay);

                boolean confirmed =
                        "confirmed".equalsIgnoreCase(st) ||
                                "booked".equalsIgnoreCase(st);

                if (paid || confirmed) {
                    triggerConfirmedEmailSafely(bookingId);

                    Intent i = new Intent(DepositPaymentActivity.this, BookingConfirmedActivity.class);
                    i.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID, bookingId);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                } else {
                    scheduleNextPoll();
                }
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                scheduleNextPoll();
            }
        });
    }

    private void scheduleNextPoll() {
        handler.postDelayed(this::pollBookingStatus, 2000);
    }

    private void triggerConfirmedEmailSafely(String bookingId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("booking_id", bookingId);
        api.fnBookingConfirmedEmail(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) { }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { }
        });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
