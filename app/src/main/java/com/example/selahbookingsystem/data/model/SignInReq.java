package com.example.selahbookingsystem.data.model;

public class SignInReq {
    public String email;
    public String password;

    public SignInReq(String email, String password) {
        this.email = email;
        this.password = password;
    }
}

