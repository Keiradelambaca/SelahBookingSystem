package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatPreview;
import com.example.selahbookingsystem.adapter.MessageRequestsAdapter;
import com.example.selahbookingsystem.data.store.SPMessageStore;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MessageRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private MessageRequestsAdapter adapter;
    private final List<ChatPreview> requests = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_requests);

        MaterialToolbar toolbar = findViewById(R.id.toolbarRequests);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvRequests = findViewById(R.id.rvRequests);

        adapter = new MessageRequestsAdapter(
                requests,
                chat -> { // accept
                    SPMessageStore.acceptRequest(chat);
                    refresh();
                },
                chat -> { // decline
                    SPMessageStore.declineRequest(chat);
                    refresh();
                }
        );

        rvRequests.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        requests.clear();
        requests.addAll(SPMessageStore.getRequests());
        adapter.notifyDataSetChanged();
    }
}

