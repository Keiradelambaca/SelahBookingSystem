package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String chatId = getIntent().getStringExtra("chatId");
        String otherUserId = getIntent().getStringExtra("otherUserId");
        String name = getIntent().getStringExtra("name");
        String photoUrl = getIntent().getStringExtra("photoUrl");

        // Set the header name
        TextView tvChatName = findViewById(R.id.tvChatName);
        if (name != null) tvChatName.setText(name);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

    }
}