package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class CurrentSet {
    @Nullable public Boolean has_existing_product;
    @Nullable public String likely_product_type;   // gel / acrylic / biab / unknown
    @Nullable public String length;                // short / medium / long / extra_long
    @Nullable public String shape;                 // round / square / almond / coffin / stiletto
    @Nullable public String condition;             // grown_out / fresh / damaged / unknown
}