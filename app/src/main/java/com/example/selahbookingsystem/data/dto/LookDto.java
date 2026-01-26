package com.example.selahbookingsystem.data.dto;

import java.util.List;

public class LookDto {
    public String id;
    public String provider_id;
    public String image_url;
    public String title;
    public String description;
    public List<String> tags;
    public Object addons; // keep generic if jsonb
    public String created_at;
}

