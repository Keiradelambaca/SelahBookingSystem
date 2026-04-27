package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatMessageAdapter;
import com.example.selahbookingsystem.data.dto.CreateMessageBody;
import com.example.selahbookingsystem.data.dto.MessageDto;
import com.example.selahbookingsystem.data.dto.ReadAtUpdateBody;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.button.MaterialButton;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_OTHER_USER_ID = "otherUserId";
    public static final String EXTRA_OTHER_USER_NAME = "name";

    private String conversationId;
    private String otherUserId;
    private String currentUserId;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private MaterialButton btnSend;
    private TextView tvChatName;

    private ChatMessageAdapter adapter;
    private final List<MessageDto> messages = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        String otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);
        currentUserId = TokenStore.getUserId(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        if (otherUserName != null) {
            tvChatName.setText(otherUserName);
        }

        btnBack.setOnClickListener(v -> finish());

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messages, currentUserId);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    private void loadMessages() {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            Toast.makeText(this, "Conversation not found", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseRestService api = ApiClient.supabase();
        api.listMessages(
                "eq." + conversationId,
                "*",
                "created_at.asc"
        ).enqueue(new Callback<List<MessageDto>>() {
            @Override
            public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    return;
                }

                messages.clear();
                messages.addAll(response.body());
                adapter.notifyDataSetChanged();

                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }

                markIncomingMessagesRead();
            }

            @Override
            public void onFailure(Call<List<MessageDto>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Could not load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(text)) return;
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            Toast.makeText(this, "Recipient missing", Toast.LENGTH_SHORT).show();
            return;
        }

        CreateMessageBody body = new CreateMessageBody(
                conversationId,
                currentUserId,
                otherUserId,
                text
        );

        SupabaseRestService api = ApiClient.supabase();
        api.createMessage("return=representation", body).enqueue(new Callback<List<MessageDto>>() {
            @Override
            public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    return;
                }

                etMessage.setText("");
                loadMessages();
            }

            @Override
            public void onFailure(Call<List<MessageDto>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Could not send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markIncomingMessagesRead() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) return;
        if (conversationId == null || conversationId.trim().isEmpty()) return;

        String nowIso = OffsetDateTime.now().toString();

        SupabaseRestService api = ApiClient.supabase();
        api.markMessagesRead(
                "return=representation",
                "eq." + conversationId,
                "eq." + currentUserId,
                "is.null",
                new ReadAtUpdateBody(nowIso)
        ).enqueue(new Callback<List<MessageDto>>() {
            @Override
            public void onResponse(Call<List<MessageDto>> call, Response<List<MessageDto>> response) {
            }

            @Override
            public void onFailure(Call<List<MessageDto>> call, Throwable t) {
            }
        });
    }
}
