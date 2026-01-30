package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatPreview;
import com.example.selahbookingsystem.adapter.ChatPreviewAdapter;
import com.example.selahbookingsystem.ui.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class CustomerMessagesActivity extends BaseActivity {

    private RecyclerView rvChats;
    private ChatPreviewAdapter adapter;
    private final List<ChatPreview> chats = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

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

        loadDummyChats();
    }

    private void loadDummyChats() {
        chats.clear();

        chats.add(new ChatPreview("chat_001", "provider_001", "Saoirse Kelly", null,
                "Hey! Just checking if you’re free for Friday...", "2:41 PM"));

        chats.add(new ChatPreview("chat_002", "provider_002", "Niamh Byrne", null,
                "Perfect — see you at 2pm 💗", "Yesterday"));

        chats.add(new ChatPreview("chat_003", "provider_003", "Aoife Murphy", null,
                "Can you send an inspo pic of the nails you want?", "Mon"));

        adapter.notifyDataSetChanged();
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_messages;
    }
}
