package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
        CheckBox cbTerms = findViewById(R.id.cb_terms);
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

        // Toggle Password Visibility
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

        // Handle Register click
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String name = etFullName != null ? etFullName.getText().toString().trim() : "";
                String account = etPhoneOrEmail != null ? etPhoneOrEmail.getText().toString().trim() : "";
                String password = etPassword != null ? etPassword.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập Họ và tên");
                    return;
                }
                if (account.isEmpty()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập Số điện thoại hoặc email");
                    return;
                }
                if (password.length() < 6) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng nhập mật khẩu tối thiểu 6 ký tự");
                    return;
                }
                if (cbTerms != null && !cbTerms.isChecked()) {
                    AppNotificationHelper.showError(RegisterActivity.this, "Vui lòng đồng ý với Điều khoản sử dụng");
                    return;
                }

                Intent intent = new Intent(RegisterActivity.this, VerifyRegisterActivity.class);
                intent.putExtra("FULL_NAME", name);
                intent.putExtra("USER_INPUT", account);
                startActivity(intent);
            });
        }
    }
}