package com.example.selahbookingsystem.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.data.dto.ProfileRoleDto;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.ui.customer.CustomerHomeActivity;
import com.example.selahbookingsystem.ui.provider.SPHomeActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String userId = TokenStore.getUserId(this);
        if (userId == null || userId.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);

        api.getUserRole("eq." + userId, "role")
                .enqueue(new Callback<List<ProfileRoleDto>>() {
                    @Override
                    public void onResponse(Call<List<ProfileRoleDto>> call,
                                           Response<List<ProfileRoleDto>> response) {

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {
                            startActivity(new Intent(RouteActivity.this, MainActivity.class));
                            finish();
                            return;
                        }

                        String role = response.body().get(0).role;

                        Intent intent;
                        if ("client".equalsIgnoreCase(role)) {
                            intent = new Intent(RouteActivity.this, CustomerHomeActivity.class);
                        } else {
                            intent = new Intent(RouteActivity.this, SPHomeActivity.class);
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<List<ProfileRoleDto>> call, Throwable t) {
                        startActivity(new Intent(RouteActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }
}
