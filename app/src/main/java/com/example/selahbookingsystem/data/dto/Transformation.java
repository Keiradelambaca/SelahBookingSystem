package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

public class Transformation {
    @Nullable public Boolean needs_removal;
    @Nullable public String removal_type;      // soak_off / file_off / none
    @Nullable public Boolean needs_new_set;
    @Nullable public Boolean needs_infill;
}