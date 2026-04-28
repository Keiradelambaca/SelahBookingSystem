package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.MessageAdapter;
import com.example.selahbookingsystem.data.dto.CreateMessageBody;
import com.example.selahbookingsystem.data.dto.MessageDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.textfield.TextInputEditText;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private SupabaseRestService api;

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private ImageView btnSend;

    private MessageAdapter adapter;
    private final List<MessageDto> messages = new ArrayList<>();

    private String chatId;
    private String otherUserId;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        api = ApiClient.supabase();

        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        String name = getIntent().getStringExtra("name");

        currentUserId = TokenStore.getUserId(this);

        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(otherUserId) || TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Missing chat details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvChatName = findViewById(R.id.tvChatName);
        if (name != null) tvChatName.setText(name);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messages, currentUserId);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        api.listMessages(
                "eq." + chatId,
                "*",
                "created_at.asc"
        ).enqueue(new Callback<List<MessageDto>>() {
            @Override
            public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
                messages.clear();

                if (response.isSuccessful() && response.body() != null) {
                    messages.addAll(response.body());
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<List<MessageDto>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        etMessage.setText("");

        CreateMessageBody body = new CreateMessageBody(
                chatId,
                currentUserId,
                otherUserId,
                text
        );

        api.createMessage("return=representation", body)
                .enqueue(new Callback<List<MessageDto>>() {
                    @Override
                    public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            messages.add(response.body().get(0));
                            adapter.notifyItemInserted(messages.size() - 1);
                            scrollToBottom();
                            updateConversationPreview(text);
                        } else {
                            Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<MessageDto>> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateConversationPreview(String messageText) {
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("last_message", messageText);
        updateBody.put("last_message_at", OffsetDateTime.now().toString());

        api.updateConversationLastMessage(
                "return=representation",
                "eq." + chatId,
                updateBody
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<com.example.selahbookingsystem.data.dto.ConversationDto>> call,
                                   Response<List<com.example.selahbookingsystem.data.dto.ConversationDto>> response) {
                // No UI needed
            }

            @Override
            public void onFailure(Call<List<com.example.selahbookingsystem.data.dto.ConversationDto>> call, Throwable t) {
                // Message was already sent, so do not block the user
            }
        });
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            rvMessages.post(() -> rvMessages.scrollToPosition(messages.size() - 1));
        }
    }
}
