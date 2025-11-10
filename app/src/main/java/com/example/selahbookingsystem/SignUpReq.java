package com.example.selahbookingsystem;

import java.util.Map;

public class SignUpReq {
    public String email;
    public String password;
    // Optional: user metadata (Supabase will store under user_metadata)
    public Map<String, Object> data;

    public SignUpReq(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public SignUpReq(String email, String password, Map<String, Object> data) {
        this.email = email;
        this.password = password;
        this.data = data;
    }
}