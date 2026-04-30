package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class Analysis {

    @Nullable public CurrentSet current_set;
    @Nullable public DesiredSet desired_set;
    @Nullable public Transformation transformation;
    @Nullable public String base_service_code;   // gel_extensions, biab_infill, etc
    @Nullable public List<String> addon_codes;   // removal, french_tip, chrome, etc
    @Nullable public String design_level;        // low, medium, high
    @Nullable public String shape;               // round, square, almond, coffin, stiletto
    @Nullable public String length;              // short, medium, long, xl

    @Nullable public Double confidence_score;
    @Nullable public List<String> notes;
}