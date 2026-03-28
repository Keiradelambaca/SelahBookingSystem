package com.example.selahbookingsystem.data.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class DesiredSet {
    @Nullable public String target_style;          // plain / french / chrome / ombre / art
    @Nullable public String target_shape;
    @Nullable public String target_length;
    @Nullable public String design_complexity;     // simple / medium / complex
    @Nullable public Boolean has_nail_art;
    @Nullable public List<String> special_features; // chrome, gems, 3d_art, etc.
}