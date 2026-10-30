package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class RegisterActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_register_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        EditText etFullName = findViewById(R.id.et_full_name);
        EditText etPhoneOrEmail = findViewById(R.id.et_phone_or_email);
        EditText etPassword = findViewById(R.id.et_password);
        ImageView imgTogglePassword = findViewById(R.id.img_toggle_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);
        ImageView imgToggleConfirm = findViewById(R.id.img_toggle_confirm_password);
        CheckBox cbTerms = findViewById(R.id.cb_terms);
        TextView tvTerms = findViewById(R.id.tv_terms_text);
        AppCompatButton btnRegister = findViewById(R.id.btn_register);
        TextView tvLoginNow = findViewById(R.id.tv_login_now);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvLoginNow != null) {
            tvLoginNow.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Toggle Password Visibility: một nút con mắt điều khiển cả hai ô mật khẩu
        View.OnClickListener togglePassword = v -> {
            isPasswordVisible = !isPasswordVisible;
            int type = InputType.TYPE_CLASS_TEXT | (isPasswordVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD);
            int icon = isPasswordVisible ? R.drawable.ic_eye : R.drawable.ic_eye_off;
            for (EditText field : new EditText[]{etPassword, etConfirmPassword}) {
                field.setInputType(type);
                field.setSelection(field.getText().length());
            }
            imgTogglePassword.setImageResource(icon);
            imgToggleConfirm.setImageResource(icon);
        };
        imgTogglePassword.setOnClickListener(togglePassword);
        imgToggleConfirm.setOnClickListener(togglePassword);

        // Bấm vào dòng chữ điều khoản cũng tick/bỏ tick
        if (tvTerms != null && cbTerms != null) {
            tvTerms.setOnClickListener(v -> cbTerms.toggle());
        }

        // Handle Register click
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String name = etFullName != null ? etFullName.getText().toString().trim() : "";
                String email = etPhoneOrEmail != null ? etPhoneOrEmail.getText().toString().trim().toLowerCase() : "";
                String password = etPassword != null ? etPassword.getText().toString() : "";

                if (name.isEmpty()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập Họ và tên");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập email hợp lệ");
                    return;
                }
                if (password.length() < 6) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập mật khẩu tối thiểu 6 ký tự");
                    return;
                }
                if (!password.equals(etConfirmPassword.getText().toString())) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Mật khẩu nhập lại không khớp");
                    return;
                }
                if (cbTerms != null && !cbTerms.isChecked()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng đồng ý với Điều khoản sử dụng");
                    return;
                }

                register(btnRegister, name, email, password);
            });
        }
    }

    private void register(AppCompatButton button, String name, String email, String password) {
        button.setEnabled(false);
        ApiClient.create(this, AccountsApi.class)
                .register(new AccountModels.RegisterRequest(name, email, password))
                .enqueue(new ApiCallback<AccountModels.OtpChallenge>(this) {
                    @Override
                    public void onSuccess(AccountModels.OtpChallenge challenge) {
                        button.setEnabled(true);
                        VerifyRegisterActivity.open(RegisterActivity.this, email, challenge);
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        if ("OTP_TOO_SOON".equals(getErrorCode())) {
                            // Vừa gửi mã cho email này: dùng lại mã đó
                            VerifyRegisterActivity.open(RegisterActivity.this, email, null);
                        } else {
                            AppNotificationHelper.showError(RegisterActivity.this, message);
                        }
                    }
                });
    }
}
