package com.example.selahbookingsystem.data.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ServiceItem {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("duration_mins")
    public Integer durationMins;

    @SerializedName("price")
    public Integer price;

}