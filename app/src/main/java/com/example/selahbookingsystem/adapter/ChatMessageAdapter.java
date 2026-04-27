package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.MessageDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<MessageDto> items;
    private final String currentUserId;

    public ChatMessageAdapter(List<MessageDto> items, String currentUserId) {
        this.items = items;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageDto item = items.get(position);
        return currentUserId != null && currentUserId.equals(item.sender_id) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_recieved, parent, false);
            return new ReceivedVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageDto message = items.get(position);
        String timeText = formatTime(message.created_at);

        if (holder instanceof SentVH) {
            SentVH h = (SentVH) holder;
            h.tvMessage.setText(message.message_text == null ? "" : message.message_text);
            h.tvTime.setText(timeText);
        } else {
            ReceivedVH h = (ReceivedVH) holder;
            h.tvMessage.setText(message.message_text == null ? "" : message.message_text);
            h.tvTime.setText(timeText);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    private String formatTime(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "";
        try {
            String cleaned = iso.replace("Z", "+0000");
            if (cleaned.matches(".*[+-]\\d\\d:\\d\\d$")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3) + cleaned.substring(cleaned.length() - 2);
            }

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
            Date date;
            try {
                date = parser.parse(cleaned);
            } catch (Exception ignore) {
                parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                date = parser.parse(cleaned);
            }

            if (date == null) return "";
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    static class SentVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentVH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        ReceivedVH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}