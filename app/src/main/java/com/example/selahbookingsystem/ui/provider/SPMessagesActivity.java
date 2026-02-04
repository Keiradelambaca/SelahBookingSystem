package com.example.selahbookingsystem.ui.provider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatPreview;
import com.example.selahbookingsystem.adapter.ChatPreviewAdapter;
import com.example.selahbookingsystem.data.store.SPMessageStore;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.example.selahbookingsystem.ui.customer.ChatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class SPMessagesActivity extends SPBaseActivity {

    private RecyclerView rvChats;
    private ChatPreviewAdapter adapter;
    private final List<ChatPreview> chats = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() {
        // this is the "child" content inflated into activity_base_provider
        return R.layout.activity_sp_messages;
    }

    @Override
    protected int getSelectedNavItemId() {
        // highlights provider Messages tab
        return R.id.nav_sp_messages;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SPMessageStore.seedIfNeeded();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMessages);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_message_requests) {
                startActivity(new Intent(this, MessageRequestsActivity.class));
                return true;
            }
            return false;
        });

        rvChats = findViewById(R.id.rvChats);

        adapter = new ChatPreviewAdapter(chats, chat -> {
            Intent i = new Intent(SPMessagesActivity.this, ChatActivity.class);
            i.putExtra("chatId", chat.chatId);
            i.putExtra("otherUserId", chat.otherUserId);
            i.putExtra("name", chat.otherUserName);
            i.putExtra("photoUrl", chat.otherUserPhotoUrl);
            startActivity(i);
        });

        rvChats.setAdapter(adapter);
        refreshChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChats();
    }

    private void refreshChats() {
        chats.clear();
        chats.addAll(SPMessageStore.getMainChats());
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
