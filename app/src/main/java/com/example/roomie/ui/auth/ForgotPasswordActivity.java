package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_forgot_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        EditText etEmailOrPhone = findViewById(R.id.et_email_or_phone);
        AppCompatButton btnSendCode = findViewById(R.id.btn_send_code);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> finish());
        }

        if (btnSendCode != null) {
            btnSendCode.setOnClickListener(v -> {
                String input = etEmailOrPhone != null ? etEmailOrPhone.getText().toString().trim() : "";
                if (input.isEmpty()) {
                    AppNotificationHelper.showError(ForgotPasswordActivity.this, "Vui lòng nhập số điện thoại hoặc email");
                    return;
                }

                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                intent.putExtra("USER_INPUT", input);
                startActivity(intent);
            });
        }
    }
}