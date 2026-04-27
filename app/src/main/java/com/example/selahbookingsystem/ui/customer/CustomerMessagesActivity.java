package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatPreview;
import com.example.selahbookingsystem.adapter.ChatPreviewAdapter;
import com.example.selahbookingsystem.data.dto.ConversationPreviewDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerMessagesActivity extends BaseActivity {

    private RecyclerView rvChats;
    private ChatPreviewAdapter adapter;
    private final List<ChatPreview> chats = new ArrayList<>();
    private SupabaseRestService api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        api = ApiClient.supabase();

        rvChats = findViewById(R.id.rvChats);

        adapter = new ChatPreviewAdapter(chats, chat -> {
            Intent i = new Intent(CustomerMessagesActivity.this, ChatActivity.class);
            i.putExtra("chatId", chat.chatId);
            i.putExtra("otherUserId", chat.otherUserId);
            i.putExtra("name", chat.otherUserName);
            i.putExtra("photoUrl", chat.otherUserPhotoUrl);
            startActivity(i);
        });

        rvChats.setAdapter(adapter);
        loadChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats();
    }

    private void loadChats() {
        String clientId = TokenStore.getUserId(this);

        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        api.listCustomerConversationPreviews(
                "eq." + clientId,
                "*",
                "last_message_at.desc.nullslast"
        ).enqueue(new Callback<List<ConversationPreviewDto>>() {
            @Override
            public void onResponse(Call<List<ConversationPreviewDto>> call, Response<List<ConversationPreviewDto>> response) {
                chats.clear();

                if (response.isSuccessful() && response.body() != null) {
                    for (ConversationPreviewDto dto : response.body()) {
                        chats.add(new ChatPreview(
                                dto.id,
                                dto.provider_id,
                                dto.provider_name == null ? "Service provider" : dto.provider_name,
                                null,
                                dto.last_message == null ? "No messages yet" : dto.last_message,
                                ""
                        ));
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<ConversationPreviewDto>> call, Throwable t) {
                Toast.makeText(CustomerMessagesActivity.this, "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_messages;
    }
}