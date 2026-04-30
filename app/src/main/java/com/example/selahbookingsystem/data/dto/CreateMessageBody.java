package com.example.selahbookingsystem.data.dto;

public class CreateMessageBody {
    public String conversation_id;
    public String sender_id;
    public String receiver_id;
    public String message_text;

    public CreateMessageBody(String conversation_id, String sender_id, String receiver_id, String message_text) {
        this.conversation_id = conversation_id;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.message_text = message_text;
    }
}
