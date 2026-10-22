package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.ui.home.HomeActivity;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_login_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etEmailOrPhone = findViewById(R.id.et_email_or_phone);
        EditText etPassword = findViewById(R.id.et_password);
        ImageView imgTogglePassword = findViewById(R.id.img_toggle_password);
        AppCompatButton btnDoLogin = findViewById(R.id.btn_do_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvRegisterNow = findViewById(R.id.tv_register_now);

        // Password visibility toggle
        if (imgTogglePassword != null && etPassword != null) {
            imgTogglePassword.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
                    isPasswordVisible = false;
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye);
                    isPasswordVisible = true;
                }
                if (etPassword.getText() != null) {
                    etPassword.setSelection(etPassword.getText().length());
                }
            });
        }

        // Login button click -> navigate to HomeActivity
        if (btnDoLogin != null) {
            btnDoLogin.setOnClickListener(v -> {
                String account = etEmailOrPhone != null ? etEmailOrPhone.getText().toString().trim() : "";
                String password = etPassword != null ? etPassword.getText().toString().trim() : "";

                if (account.isEmpty()) {
                    AppNotificationHelper.showError(LoginActivity.this, "Vui lòng nhập số điện thoại hoặc email");
                    return;
                }
                if (password.isEmpty()) {
                    AppNotificationHelper.showError(LoginActivity.this, "Vui lòng nhập mật khẩu");
                    return;
                }

                AppNotificationHelper.showSuccess(LoginActivity.this, "Đăng nhập thành công!");
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Forgot password click
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        // Register now click
        if (tvRegisterNow != null) {
            tvRegisterNow.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }
}