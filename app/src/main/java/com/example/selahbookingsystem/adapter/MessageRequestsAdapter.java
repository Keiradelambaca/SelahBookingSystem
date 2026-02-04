package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MessageRequestsAdapter extends RecyclerView.Adapter<MessageRequestsAdapter.VH> {

    public interface OnAccept { void onAccept(ChatPreview chat); }
    public interface OnDecline { void onDecline(ChatPreview chat); }

    private final List<ChatPreview> items;
    private final OnAccept onAccept;
    private final OnDecline onDecline;

    public MessageRequestsAdapter(List<ChatPreview> items, OnAccept onAccept, OnDecline onDecline) {
        this.items = items;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatPreview chat = items.get(position);

        h.tvName.setText(chat.otherUserName);
        h.tvPreview.setText(chat.lastMessage == null ? "" : chat.lastMessage);

        h.btnAccept.setOnClickListener(v -> onAccept.onAccept(chat));
        h.btnDecline.setOnClickListener(v -> onDecline.onDecline(chat));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPreview;
        MaterialButton btnAccept, btnDecline;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
