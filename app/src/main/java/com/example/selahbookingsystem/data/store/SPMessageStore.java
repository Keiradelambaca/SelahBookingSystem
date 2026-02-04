package com.example.selahbookingsystem.data.store;

import com.example.selahbookingsystem.adapter.ChatPreview;

import java.util.ArrayList;
import java.util.List;

public class SPMessageStore {

    private static final List<ChatPreview> MAIN_CHATS = new ArrayList<>();
    private static final List<ChatPreview> REQUEST_CHATS = new ArrayList<>();

    // Seed demo data once
    private static boolean seeded = false;

    public static void seedIfNeeded() {
        if (seeded) return;
        seeded = true;

        MAIN_CHATS.add(new ChatPreview(
                "sp_chat_001", "client_001", "Keira Client", null,
                "Perfect, see you tomorrow 💗", "Yesterday"
        ));

        REQUEST_CHATS.add(new ChatPreview(
                "req_chat_101", "client_101", "Aisling Byrne", null,
                "Hi! Are you taking new clients?", "2:41 PM"
        ));

        REQUEST_CHATS.add(new ChatPreview(
                "req_chat_102", "client_102", "Emma Murphy", null,
                "How much for BIAB + French tips?", "Mon"
        ));
    }

    public static List<ChatPreview> getMainChats() {
        return MAIN_CHATS;
    }

    public static List<ChatPreview> getRequests() {
        return REQUEST_CHATS;
    }

    public static void acceptRequest(ChatPreview chat) {
        REQUEST_CHATS.remove(chat);
        MAIN_CHATS.add(0, chat); // move to top
    }

    public static void declineRequest(ChatPreview chat) {
        REQUEST_CHATS.remove(chat);
    }
}