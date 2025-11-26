package com.example.selahbookingsystem;

public class SignUpReq {
    public String email;
    public String password;

    public SignUpReq(String email, String password) {
        this.email = email;
        this.password = password;
    }
}