package com.example.selahbookingsystem.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EditableAiSelection {

    @Nullable
    public String baseServiceCode;

    @NonNull
    public List<String> addonCodes = new ArrayList<>();

    @NonNull
    public String designLevel = "low"; // low, medium, high

    @Nullable
    public String shape;

    @Nullable
    public String length;

    public boolean hasAddon(@Nullable String code) {
        if (code == null || addonCodes == null) return false;
        return addonCodes.contains(code);
    }

    public void addAddon(@Nullable String code) {
        if (code == null || code.trim().isEmpty()) return;
        if (addonCodes == null) addonCodes = new ArrayList<>();
        if (!addonCodes.contains(code)) {
            addonCodes.add(code);
        }
    }

    public void removeAddon(@Nullable String code) {
        if (code == null || addonCodes == null) return;
        addonCodes.remove(code);
    }

    public void toggleAddon(@Nullable String code) {
        if (code == null || code.trim().isEmpty()) return;
        if (addonCodes == null) addonCodes = new ArrayList<>();

        if (addonCodes.contains(code)) {
            addonCodes.remove(code);
        } else {
            addonCodes.add(code);
        }
    }

    public void clearAddons() {
        if (addonCodes != null) {
            addonCodes.clear();
        }
    }
}
