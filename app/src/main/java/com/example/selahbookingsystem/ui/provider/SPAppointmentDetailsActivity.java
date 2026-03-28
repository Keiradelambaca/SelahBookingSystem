package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPAppointmentDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvClientName;
    private ImageView imgInspo;
    private TextView tvServiceName;
    private TextView tvDate;
    private TextView tvTime;
    private MaterialButton btnStartAppointment;
    private MaterialButton btnReschedule;
    private MaterialButton btnCancel;

    private SupabaseRestService rest;

    private String bookingId;
    private String clientName;
    private String serviceName;
    private String startTimeIso;
    private String endTimeIso;
    private String inspoPhotoUrl;
    private String status;

    private boolean appointmentStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp_appointment_details);

        toolbar = findViewById(R.id.toolbar);
        tvClientName = findViewById(R.id.tvClientName);
        imgInspo = findViewById(R.id.imgInspo);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        btnStartAppointment = findViewById(R.id.btnStartAppointment);
        btnReschedule = findViewById(R.id.btnReschedule);
        btnCancel = findViewById(R.id.btnCancel);

        ApiClient.init(getApplicationContext());
        rest = ApiClient.get().create(SupabaseRestService.class);

        readIntentExtras();
        setupToolbar();
        bindUi();
        setupButtons();
    }

    private void readIntentExtras() {
        bookingId = getIntent().getStringExtra("booking_id");
        clientName = getIntent().getStringExtra("client_name");
        serviceName = getIntent().getStringExtra("service_name");
        startTimeIso = getIntent().getStringExtra("start_time");
        endTimeIso = getIntent().getStringExtra("end_time");
        inspoPhotoUrl = getIntent().getStringExtra("inspo_photo_url");
        status = getIntent().getStringExtra("status");

        appointmentStarted = "in_progress".equalsIgnoreCase(status);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
    }

    private void bindUi() {
        tvClientName.setText(safe(clientName, "Client"));
        tvServiceName.setText(safe(serviceName, "Appointment"));
        tvDate.setText(formatDate(startTimeIso));
        tvTime.setText(formatTimeRange(startTimeIso, endTimeIso));

        if (!TextUtils.isEmpty(inspoPhotoUrl)) {
            Glide.with(this)
                    .load(inspoPhotoUrl)
                    .placeholder(R.drawable.placeholder_banner)
                    .error(R.drawable.placeholder_banner)
                    .into(imgInspo);
        } else {
            imgInspo.setImageResource(R.drawable.placeholder_banner);
        }

        updateStartEndButtonUi();
    }

    private void setupButtons() {
        btnStartAppointment.setOnClickListener(v -> {
            if (TextUtils.isEmpty(bookingId)) {
                Toast.makeText(this, "Missing booking id", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!appointmentStarted) {
                markAppointmentInProgress();
            } else {
                markAppointmentCompleted();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (TextUtils.isEmpty(bookingId)) {
                Toast.makeText(this, "Missing booking id", Toast.LENGTH_SHORT).show();
                return;
            }
            cancelAppointment();
        });

        btnReschedule.setOnClickListener(v ->
                Toast.makeText(this, "Reschedule flow coming next", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateStartEndButtonUi() {
        if (appointmentStarted) {
            btnStartAppointment.setText("End Appointment");
            btnStartAppointment.setBackgroundTintList(
                    getColorStateList(R.color.buttonGreen)
            );
        } else {
            btnStartAppointment.setText("Start Appointment");
            btnStartAppointment.setBackgroundTintList(
                    getColorStateList(R.color.signaturePink)
            );
        }
    }

    private void markAppointmentInProgress() {
        setButtonsEnabled(false);

        SupabaseRestService.BookingUpdateBody body =
                new SupabaseRestService.BookingUpdateBody("in_progress", startTimeIso, endTimeIso);

        rest.updateBooking("eq." + bookingId, body).enqueue(new Callback<java.util.List<BookingDto>>() {
            @Override
            public void onResponse(Call<java.util.List<BookingDto>> call, Response<java.util.List<BookingDto>> response) {
                setButtonsEnabled(true);

                if (!response.isSuccessful()) {
                    Toast.makeText(SPAppointmentDetailsActivity.this,
                            "Failed to start appointment", Toast.LENGTH_SHORT).show();
                    return;
                }

                appointmentStarted = true;
                status = "in_progress";
                updateStartEndButtonUi();

                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Appointment started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<java.util.List<BookingDto>> call, Throwable t) {
                setButtonsEnabled(true);
                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Failed to start appointment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAppointmentCompleted() {
        setButtonsEnabled(false);

        SupabaseRestService.BookingUpdateBody body =
                new SupabaseRestService.BookingUpdateBody("completed", startTimeIso, endTimeIso);

        rest.updateBooking("eq." + bookingId, body).enqueue(new Callback<java.util.List<BookingDto>>() {
            @Override
            public void onResponse(Call<java.util.List<BookingDto>> call, Response<java.util.List<BookingDto>> response) {
                setButtonsEnabled(true);

                if (!response.isSuccessful()) {
                    Toast.makeText(SPAppointmentDetailsActivity.this,
                            "Failed to complete appointment", Toast.LENGTH_SHORT).show();
                    return;
                }

                appointmentStarted = false;
                status = "completed";
                updateStartEndButtonUi();

                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Appointment completed", Toast.LENGTH_SHORT).show();

                finish();
            }

            @Override
            public void onFailure(Call<java.util.List<BookingDto>> call, Throwable t) {
                setButtonsEnabled(true);
                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Failed to complete appointment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelAppointment() {
        setButtonsEnabled(false);

        SupabaseRestService.BookingUpdateBody body =
                new SupabaseRestService.BookingUpdateBody("cancelled", startTimeIso, endTimeIso);

        rest.updateBooking("eq." + bookingId, body).enqueue(new Callback<java.util.List<BookingDto>>() {
            @Override
            public void onResponse(Call<java.util.List<BookingDto>> call, Response<java.util.List<BookingDto>> response) {
                setButtonsEnabled(true);

                if (!response.isSuccessful()) {
                    Toast.makeText(SPAppointmentDetailsActivity.this,
                            "Failed to cancel appointment", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Appointment cancelled", Toast.LENGTH_SHORT).show();

                finish();
            }

            @Override
            public void onFailure(Call<java.util.List<BookingDto>> call, Throwable t) {
                setButtonsEnabled(true);
                Toast.makeText(SPAppointmentDetailsActivity.this,
                        "Failed to cancel appointment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnStartAppointment.setEnabled(enabled);
        btnReschedule.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String formatDate(String iso) {
        Date date = parseIso(iso);
        if (date == null) return "Date unavailable";

        SimpleDateFormat out = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
        return out.format(date);
    }

    private String formatTimeRange(String startIso, String endIso) {
        Date start = parseIso(startIso);
        Date end = parseIso(endIso);

        if (start == null || end == null) return "Time unavailable";

        SimpleDateFormat out = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return out.format(start) + " - " + out.format(end);
    }

    private Date parseIso(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String cleaned = value.trim();

        if (cleaned.endsWith("Z")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        int dotIndex = cleaned.indexOf(".");
        if (dotIndex != -1) {
            cleaned = cleaned.substring(0, dotIndex);
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
