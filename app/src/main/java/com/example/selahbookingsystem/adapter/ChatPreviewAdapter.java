package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ChatPreview;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatPreviewAdapter.VH> {

    public interface OnChatClickListener {
        void onChatClick(ChatPreview chat);
    }

    private final List<ChatPreview> items;
    private final OnChatClickListener listener;

    public ChatPreviewAdapter(List<ChatPreview> items, OnChatClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_preview, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatPreview chat = items.get(position);

        h.tvName.setText(chat.otherUserName);
        h.tvLastMessage.setText(chat.lastMessage == null ? "" : chat.lastMessage);

        // If you use Glide/Picasso later, load chat.otherUserPhotoUrl into h.ivProfile
        // For now keep placeholder.

        h.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfile;
        TextView tvName, tvLastMessage;

        VH(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}