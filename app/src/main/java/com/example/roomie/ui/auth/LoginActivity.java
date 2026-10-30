package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
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
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

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
        View btnGoogleLogin = findViewById(R.id.btn_google_login);

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

        // Đăng nhập bằng email
        if (btnDoLogin != null) {
            btnDoLogin.setOnClickListener(v -> {
                String account = etEmailOrPhone != null ? etEmailOrPhone.getText().toString().trim().toLowerCase() : "";
                String password = etPassword != null ? etPassword.getText().toString() : "";

                if (!Patterns.EMAIL_ADDRESS.matcher(account).matches()) {
                    AppNotificationHelper.showError(LoginActivity.this, "Vui lòng nhập email hợp lệ");
                    return;
                }
                if (password.isEmpty()) {
                    AppNotificationHelper.showError(LoginActivity.this, "Vui lòng nhập mật khẩu");
                    return;
                }

                login(btnDoLogin, account, password);
            });
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> loginWithGoogle(btnGoogleLogin));
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

    // Chọn tài khoản Google trên máy -> gửi idToken cho backend -> vào app như đăng nhập thường
    private void loginWithGoogle(View button) {
        if (!GoogleSignIn.isConfigured()) {
            AppNotificationHelper.showError(this, "Chưa cấu hình roomie.googleWebClientId trong local.properties");
            return;
        }
        button.setEnabled(false);
        GoogleSignIn.start(this, new GoogleSignIn.Listener() {
            @Override
            public void onIdToken(String idToken) {
                ApiClient.create(LoginActivity.this, AccountsApi.class)
                        .loginWithGoogle(new AccountModels.GoogleLoginRequest(idToken))
                        .enqueue(new ApiCallback<AccountModels.AuthResult>(LoginActivity.this) {
                            @Override
                            public void onSuccess(AccountModels.AuthResult result) {
                                AppNotificationHelper.showSuccess(LoginActivity.this, "Đăng nhập thành công!");
                                AuthFlow.onAuthenticated(LoginActivity.this, result);
                            }

                            @Override
                            public void onError(String message) {
                                button.setEnabled(true);
                                AppNotificationHelper.showError(LoginActivity.this, message);
                            }
                        });
            }

            @Override
            public void onError(String message) {
                button.setEnabled(true);
                if (message != null) AppNotificationHelper.showError(LoginActivity.this, message);
            }
        });
    }

    private void login(AppCompatButton button, String account, String password) {
        button.setEnabled(false);
        ApiClient.create(this, AccountsApi.class)
                .login(new AccountModels.LoginRequest(account, password))
                .enqueue(new ApiCallback<AccountModels.AuthResult>(this) {
                    @Override
                    public void onSuccess(AccountModels.AuthResult result) {
                        AppNotificationHelper.showSuccess(LoginActivity.this, "Đăng nhập thành công!");
                        AuthFlow.onAuthenticated(LoginActivity.this, result);
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        if ("EMAIL_NOT_VERIFIED".equals(getErrorCode())) {
                            // Đăng ký dở dang: gửi mã mới rồi mở màn xác thực
                            VerifyRegisterActivity.resendAndOpen(LoginActivity.this, account);
                        } else {
                            AppNotificationHelper.showError(LoginActivity.this, message);
                        }
                    }
                });
    }
}