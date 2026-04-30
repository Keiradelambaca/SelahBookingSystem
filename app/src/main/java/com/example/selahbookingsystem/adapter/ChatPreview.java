package com.example.selahbookingsystem.adapter;

public class ChatPreview {
    public String chatId;
    public String otherUserId;
    public String otherUserName;
    public String otherUserPhotoUrl; // optional
    public String lastMessage;
    public String timeText; // e.g. "2:41 PM"


    public ChatPreview(String chatId, String otherUserId, String otherUserName, String otherUserPhotoUrl,
                       String lastMessage, String timeText) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherUserPhotoUrl = otherUserPhotoUrl;
        this.lastMessage = lastMessage;
        this.timeText = timeText;
    }
}
