package com.example.selahbookingsystem.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.LookItem;

import java.util.List;

public class LooksAdapter extends RecyclerView.Adapter<LooksAdapter.VH> {

    public interface OnLookClick {
        void onClick(LookItem look);
    }

    public interface OnLikeClick {
        void onClick(LookItem look);
    }

    private final List<LookItem> items;
    private final OnLookClick onLookClick;
    private final OnLikeClick onLikeClick;

    public LooksAdapter(List<LookItem> items, OnLookClick onLookClick, OnLikeClick onLikeClick) {
        this.items = items;
        this.onLookClick = onLookClick;
        this.onLikeClick = onLikeClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_look_tile, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LookItem look = items.get(position);

        Glide.with(h.itemView.getContext())
                .load(look.getImageUrl())
                .centerCrop()
                .into(h.img);

        h.btnLike.setImageResource(look.isLiked() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        h.itemView.setOnClickListener(v -> {
            if (onLookClick != null) onLookClick.onClick(look);
        });

        h.btnLike.setOnClickListener(v -> {
            if (onLikeClick != null) onLikeClick.onClick(look); // <-- NO, see below
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int indexOf(LookItem look) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(look.getId())) return i;
        }
        return -1;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        ImageButton btnLike;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgLook);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
    }
}