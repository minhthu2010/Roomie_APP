package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;

public class ResetSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_success);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_success_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        AppCompatButton btnBackToLogin = findViewById(R.id.btn_back_to_login);

        Runnable goToLogin = () -> {
            Intent intent = new Intent(ResetSuccessActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goToLogin.run());
        }

        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(v -> goToLogin.run());
        }
    }
}