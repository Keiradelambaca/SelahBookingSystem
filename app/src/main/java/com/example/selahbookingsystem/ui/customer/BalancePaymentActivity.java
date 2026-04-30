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

public class BalancePaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_BALANCE_CENTS = "extra_balance_cents";
    public static final String EXTRA_TOTAL_CENTS = "extra_total_cents";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";

    private SupabaseRestService api;

    private String bookingId;
    private String providerName;

    private int balanceCents;
    private int totalCents;

    private boolean startedCheckout = false;
    private boolean isPolling = false;

    private MaterialButton btnPay;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pollAttempts = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_payment);

        api = ApiClient.get().create(SupabaseRestService.class);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);

        balanceCents = getIntent().getIntExtra(EXTRA_BALANCE_CENTS, 0);
        totalCents = getIntent().getIntExtra(EXTRA_TOTAL_CENTS, 0);

        TextView tvInfo = findViewById(R.id.tvBalanceInfo);
        btnPay = findViewById(R.id.btnPayBalance);
        MaterialButton btnCancel = findViewById(R.id.btnCancelBalancePayment);

        tvInfo.setText(String.format(Locale.getDefault(),
                "Remaining balance: €%.2f\nAppointment total: €%.2f\n\nPay the remaining balance to fully pay this booking.",
                balanceCents / 100.0,
                totalCents / 100.0
        ));

        btnCancel.setOnClickListener(v -> finish());

        btnPay.setOnClickListener(v -> {
            if (TextUtils.isEmpty(bookingId)) {
                toast("Missing booking id");
                return;
            }

            if (balanceCents <= 0) {
                toast("Nothing left to pay");
                return;
            }

            btnPay.setEnabled(false);
            createCheckoutAndOpen();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (startedCheckout) {
            startPolling();
        }
    }

    private void createCheckoutAndOpen() {
        Log.d("BALANCE", "Creating checkout for bookingId=" + bookingId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("booking_id", bookingId);
        payload.put("amount_cents", balanceCents);

        if (!TextUtils.isEmpty(providerName)) {
            payload.put("provider_name", providerName);
        }

        api.fnCreateBalanceCheckout(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    toast("Payment setup failed");
                    btnPay.setEnabled(true);
                    return;
                }

                String url = String.valueOf(resp.body().get("url"));

                if (TextUtils.isEmpty(url)) {
                    toast("Missing checkout URL");
                    btnPay.setEnabled(true);
                    return;
                }

                startedCheckout = true;

                CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
                intent.launchUrl(BalancePaymentActivity.this, Uri.parse(url));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                toast("Network error");
                btnPay.setEnabled(true);
            }
        });
    }

    private void startPolling() {
        if (isPolling) return;

        isPolling = true;
        pollAttempts = 0;
        pollBookingStatus();
    }

    private void stopPolling() {
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void pollBookingStatus() {
        pollAttempts++;

        if (pollAttempts > 60) {
            stopPolling();
            toast("Still waiting for payment confirmation…");
            btnPay.setEnabled(true);
            return;
        }

        api.getBookingById(
                "id,status,payment_status,balance_paid_at",
                "eq." + bookingId
        ).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) {
                    scheduleNextPoll();
                    return;
                }

                BookingDto b = resp.body().get(0);

                String pay = b.payment_status == null ? "" : b.payment_status.trim();

                Log.d("BALANCE", "Poll #" + pollAttempts + " => payment_status=" + pay);

                if ("paid".equalsIgnoreCase(pay)) {
                    stopPolling();

                    Toast.makeText(BalancePaymentActivity.this,
                            "Payment complete",
                            Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(BalancePaymentActivity.this,
                            CustomerAppointmentDetailActivity.class);

                    i.putExtra(CustomerAppointmentDetailActivity.EXTRA_BOOKING_ID, bookingId);
                    i.putExtra("refresh", true);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}