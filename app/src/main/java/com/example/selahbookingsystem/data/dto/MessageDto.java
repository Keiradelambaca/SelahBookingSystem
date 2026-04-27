package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class MessageDto {
    public String id;
    public String conversation_id;
    public String sender_id;
    public String receiver_id;
    public String message_text;
    @Nullable public String created_at;
    @Nullable public String read_at;
}
