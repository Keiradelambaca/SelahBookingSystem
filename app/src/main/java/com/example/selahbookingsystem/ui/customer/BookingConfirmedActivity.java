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
import com.example.selahbookingsystem.data.dto.ConversationDto;
import com.example.selahbookingsystem.data.dto.CreateConversationBody;
import com.example.selahbookingsystem.data.dto.CreateMessageBody;
import com.example.selahbookingsystem.data.dto.MessageDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.gson.Gson;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingConfirmedActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";

    private SupabaseRestService api;
    private BookingDto booking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmed);

        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (TextUtils.isEmpty(bookingId)) {
            finish();
            return;
        }

        api = ApiClient.supabase();

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

        btnMessage.setOnClickListener(v -> openChatWithProvider());

        api.getBookingById("*", "eq." + bookingId).enqueue(new Callback<List<BookingDto>>() {
            @Override
            public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    finish();
                    return;
                }

                booking = response.body().get(0);

                String img = booking.inspo_photo_url != null
                        ? booking.inspo_photo_url
                        : booking.current_photo_url;

                if (img != null) {
                    Glide.with(BookingConfirmedActivity.this).load(img).into(ivLook);
                }

                tvProvider.setText(booking.provider_name == null ? "Service provider" : booking.provider_name);
                tvStatus.setText(booking.status == null ? "" : booking.status);

                Instant s = Instant.parse(booking.start_time);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm")
                        .withZone(ZoneId.systemDefault());

                tvTime.setText(fmt.format(s) + " (" + booking.duration_mins + " mins)");
                tvDetails.setText(new Gson().toJson(booking.details_json));
            }

            @Override
            public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                Toast.makeText(BookingConfirmedActivity.this, "Network error", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void openChatWithProvider() {
        if (booking == null) {
            Toast.makeText(this, "Booking is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        String clientId = TokenStore.getUserId(this);

        if (TextUtils.isEmpty(clientId)) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(booking.provider_id)) {
            Toast.makeText(this, "Provider missing", Toast.LENGTH_SHORT).show();
            return;
        }

        api.findConversationByClientAndProvider(
                "eq." + clientId,
                "eq." + booking.provider_id,
                "*",
                1
        ).enqueue(new Callback<List<ConversationDto>>() {
            @Override
            public void onResponse(Call<List<ConversationDto>> call, Response<List<ConversationDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    sendFirstMessageThenOpen(response.body().get(0), clientId);
                } else {
                    createAcceptedConversation(clientId);
                }
            }

            @Override
            public void onFailure(Call<List<ConversationDto>> call, Throwable t) {
                Toast.makeText(BookingConfirmedActivity.this, "Failed to open chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAcceptedConversation(String clientId) {
        CreateConversationBody body = new CreateConversationBody(
                clientId,
                booking.provider_id,
                booking.id,
                "accepted"
        );

        api.createConversation("return=representation", body)
                .enqueue(new Callback<List<ConversationDto>>() {
                    @Override
                    public void onResponse(Call<List<ConversationDto>> call, Response<List<ConversationDto>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            sendFirstMessageThenOpen(response.body().get(0), clientId);
                        } else {
                            Toast.makeText(BookingConfirmedActivity.this, "Could not create chat", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ConversationDto>> call, Throwable t) {
                        Toast.makeText(BookingConfirmedActivity.this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendFirstMessageThenOpen(ConversationDto conversation, String clientId) {
        String messageText = "Hi, I’ve booked an appointment and wanted to message you about it.";

        CreateMessageBody body = new CreateMessageBody(
                conversation.id,
                clientId,
                booking.provider_id,
                messageText
        );

        api.createMessage("return=representation", body)
                .enqueue(new Callback<List<MessageDto>>() {
                    @Override
                    public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(BookingConfirmedActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        updateConversationPreviewThenOpen(conversation, messageText);
                    }

                    @Override
                    public void onFailure(Call<List<MessageDto>> call, Throwable t) {
                        Toast.makeText(BookingConfirmedActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateConversationPreviewThenOpen(ConversationDto conversation, String messageText) {
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("last_message", messageText);
        updateBody.put("last_message_at", OffsetDateTime.now().toString());

        api.updateConversationLastMessage(
                "return=representation",
                "eq." + conversation.id,
                updateBody
        ).enqueue(new Callback<List<ConversationDto>>() {
            @Override
            public void onResponse(Call<List<ConversationDto>> call, Response<List<ConversationDto>> response) {
                openChat(conversation);
            }

            @Override
            public void onFailure(Call<List<ConversationDto>> call, Throwable t) {
                openChat(conversation);
            }
        });
    }

    private void openChat(ConversationDto conversation) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("chatId", conversation.id);
        i.putExtra("otherUserId", booking.provider_id);
        i.putExtra("name", booking.provider_name);
        startActivity(i);
    }
}
