package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.MessageDto;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final List<MessageDto> messages;
    private final String currentUserId;

    public MessageAdapter(List<MessageDto> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageDto message = messages.get(position);

        if (message.sender_id != null && message.sender_id.equals(currentUserId)) {
            return TYPE_OUTGOING;
        }

        return TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_OUTGOING) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_outgoing, parent, false);
            return new OutgoingVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_incoming, parent, false);
            return new IncomingVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageDto message = messages.get(position);

        if (holder instanceof OutgoingVH) {
            ((OutgoingVH) holder).tvMsgOut.setText(message.message_text);
        } else if (holder instanceof IncomingVH) {
            ((IncomingVH) holder).tvMsgIn.setText(message.message_text);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class OutgoingVH extends RecyclerView.ViewHolder {
        TextView tvMsgOut;

        OutgoingVH(@NonNull View itemView) {
            super(itemView);
            tvMsgOut = itemView.findViewById(R.id.tvMsgOut);
        }
    }

    static class IncomingVH extends RecyclerView.ViewHolder {
        TextView tvMsgIn;

        IncomingVH(@NonNull View itemView) {
            super(itemView);
            tvMsgIn = itemView.findViewById(R.id.tvMsgIn);
        }
    }
}