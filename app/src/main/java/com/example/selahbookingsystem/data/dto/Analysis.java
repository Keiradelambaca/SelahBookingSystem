package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class Analysis {
    @Nullable public CurrentSet current_set;
    @Nullable public DesiredSet desired_set;
    @Nullable public Transformation transformation;
    @Nullable public Double confidence_score;
    @Nullable public List<String> notes;
    @Nullable public String base_service_code;   // gel_extensions etc
    @Nullable public List<String> addon_codes;   // french_tip, gems_charms, removal
    @Nullable public String design_level;        // low, medium, high
    @Nullable public String shape;               // almond, square, coffin
    @Nullable public String length;              // short, medium, long, xl
}