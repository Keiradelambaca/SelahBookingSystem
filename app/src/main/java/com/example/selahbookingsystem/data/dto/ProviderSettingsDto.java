package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class ProviderSettingsDto {
    public String provider_id;
    public boolean deposits_enabled;

    @Nullable
    public Integer deposit_percent;
    public String updated_at;
}
