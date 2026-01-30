package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

                String img = b.inspo_photo_url != null ? b.inspo_photo_url : b.current_photo_url;
                if (img != null) Glide.with(BookingConfirmedActivity.this).load(img).into(ivLook);

                tvProvider.setText(b.provider_name);
                tvStatus.setText(b.status);

                Instant s = Instant.parse(b.start_time);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm")
                        .withZone(ZoneId.systemDefault());
                tvTime.setText(fmt.format(s) + " (" + b.duration_mins + " mins)");

                tvDetails.setText(new Gson().toJson(b.details_json));

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
}