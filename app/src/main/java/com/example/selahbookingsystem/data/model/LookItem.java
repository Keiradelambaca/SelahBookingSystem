package com.example.selahbookingsystem.data.model;

import com.example.selahbookingsystem.data.dto.LookDto;

import java.util.List;

public class LookItem {

    private String id;
    private String providerId;
    private String imageUrl;
    private String title;
    private String description;
    private List<String> tags;

    // UI state (not necessarily stored in DB)
    private boolean liked;

    public LookItem() {}

    // --- Mapper from DTO ---
    public static LookItem fromDto(LookDto d) {
        LookItem i = new LookItem();
        i.id = d.id;
        i.providerId = d.provider_id;
        i.imageUrl = d.image_url;
        i.title = d.title;
        i.description = d.description;
        i.tags = d.tags;
        i.liked = false;
        return i;
    }

    // --- Getters / Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
}