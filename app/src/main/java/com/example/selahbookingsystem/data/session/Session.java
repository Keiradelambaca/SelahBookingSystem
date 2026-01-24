package com.example.selahbookingsystem.data.session;

import com.example.selahbookingsystem.data.model.User;
import com.google.gson.annotations.SerializedName;

public class Session {
    @SerializedName("access_token") public String accessToken;
    @SerializedName("token_type")   public String tokenType;
    @SerializedName("expires_in")   public Integer expiresIn;
    @SerializedName("refresh_token")public String refreshToken;
    public User user;
}
