package com.example.roomie.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
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
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

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
                String input = etEmailOrPhone != null ? etEmailOrPhone.getText().toString().trim().toLowerCase() : "";
                if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                    AppNotificationHelper.showError(ForgotPasswordActivity.this, "Vui lòng nhập email hợp lệ");
                    return;
                }

                sendCode(btnSendCode, input);
            });
        }
    }

    // Server gửi mã khôi phục về email của tài khoản
    private void sendCode(AppCompatButton button, String account) {
        button.setEnabled(false);
        ApiClient.create(this, AccountsApi.class)
                .forgotPassword(new AccountModels.EmailRequest(account))
                .enqueue(new ApiCallback<AccountModels.OtpChallenge>(this) {
                    @Override
                    public void onSuccess(AccountModels.OtpChallenge challenge) {
                        button.setEnabled(true);
                        VerifyOtpActivity.open(ForgotPasswordActivity.this, account, challenge);
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        if ("OTP_TOO_SOON".equals(getErrorCode())) {
                            // Vừa gửi mã cho tài khoản này: nhập mã đã nhận
                            VerifyOtpActivity.open(ForgotPasswordActivity.this, account, null);
                        } else {
                            AppNotificationHelper.showError(ForgotPasswordActivity.this, message);
                        }
                    }
                });
    }
}