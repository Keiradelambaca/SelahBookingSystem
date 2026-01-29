package com.example.selahbookingsystem.data.model;

import com.google.gson.annotations.SerializedName;

public class ServiceItem {
    @SerializedName("id")
    public String id;

    // change this if your services table uses a different column name like "name" or "title"
    @SerializedName("name")
    public String name;

    @SerializedName("duration_mins")
    public Integer durationMins;

    @SerializedName("price")
    public Integer price;
}