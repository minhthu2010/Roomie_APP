package com.example.roomie.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String EXTRA_ACCOUNT = "USER_INPUT";
    private static final String EXTRA_RESET_TOKEN = "RESET_TOKEN";

    private boolean isNewPassVisible = false;
    private boolean isConfirmPassVisible = false;

    /** resetToken: server cấp sau khi nhập đúng mã khôi phục, dùng một lần, hết hạn sau 10 phút. */
    public static void open(Activity activity, String account, String resetToken) {
        Intent intent = new Intent(activity, ResetPasswordActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_RESET_TOKEN, resetToken);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_reset_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userInput = getIntent().getStringExtra(EXTRA_ACCOUNT);
        String resetToken = getIntent().getStringExtra(EXTRA_RESET_TOKEN);
        if (userInput == null || resetToken == null) {
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvResetSubtext = findViewById(R.id.tv_reset_subtext);
        EditText etNewPassword = findViewById(R.id.et_new_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);
        ImageView imgToggleNewPass = findViewById(R.id.img_toggle_new_pass);
        ImageView imgToggleConfirmPass = findViewById(R.id.img_toggle_confirm_pass);
        TextView tvMatchStatus = findViewById(R.id.tv_match_status);
        AppCompatButton btnDoReset = findViewById(R.id.btn_do_reset);

        if (tvResetSubtext != null) {
            tvResetSubtext.setText(getString(R.string.reset_subtext, userInput));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (imgToggleNewPass != null && etNewPassword != null) {
            imgToggleNewPass.setOnClickListener(v -> {
                if (isNewPassVisible) {
                    etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgToggleNewPass.setImageResource(R.drawable.ic_eye_off);
                    isNewPassVisible = false;
                } else {
                    etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgToggleNewPass.setImageResource(R.drawable.ic_eye);
                    isNewPassVisible = true;
                }
                if (etNewPassword.getText() != null) {
                    etNewPassword.setSelection(etNewPassword.getText().length());
                }
            });
        }

        if (imgToggleConfirmPass != null && etConfirmPassword != null) {
            imgToggleConfirmPass.setOnClickListener(v -> {
                if (isConfirmPassVisible) {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgToggleConfirmPass.setImageResource(R.drawable.ic_eye_off);
                    isConfirmPassVisible = false;
                } else {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgToggleConfirmPass.setImageResource(R.drawable.ic_eye);
                    isConfirmPassVisible = true;
                }
                if (etConfirmPassword.getText() != null) {
                    etConfirmPassword.setSelection(etConfirmPassword.getText().length());
                }
            });
        }

        TextWatcher passWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (etNewPassword == null || etConfirmPassword == null || tvMatchStatus == null) return;

                String p1 = etNewPassword.getText().toString();
                String p2 = etConfirmPassword.getText().toString();

                if (!p1.isEmpty() && !p2.isEmpty()) {
                    if (p1.equals(p2)) {
                        tvMatchStatus.setText(R.string.passwords_match);
                        tvMatchStatus.setTextColor(ContextCompat.getColor(ResetPasswordActivity.this, R.color.color_success));
                    } else {
                        tvMatchStatus.setText(R.string.passwords_not_match);
                        tvMatchStatus.setTextColor(ContextCompat.getColor(ResetPasswordActivity.this, R.color.color_error));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        if (etNewPassword != null) etNewPassword.addTextChangedListener(passWatcher);
        if (etConfirmPassword != null) etConfirmPassword.addTextChangedListener(passWatcher);

        final String finalUserInput = userInput;
        if (btnDoReset != null) {
            btnDoReset.setOnClickListener(v -> {
                String p1 = etNewPassword != null ? etNewPassword.getText().toString() : "";
                String p2 = etConfirmPassword != null ? etConfirmPassword.getText().toString() : "";

                if (p1.length() < 8) {
                    AppNotificationHelper.showError(ResetPasswordActivity.this, "Mật khẩu phải có ít nhất 8 ký tự");
                    return;
                }

                if (!p1.matches(".*[a-z].*") || !p1.matches(".*[A-Z].*") || !p1.matches(".*\\d.*")) {
                    AppNotificationHelper.showError(ResetPasswordActivity.this, "Mật khẩu cần có chữ hoa, chữ thường và số");
                    return;
                }

                if (!p1.equals(p2)) {
                    AppNotificationHelper.showError(ResetPasswordActivity.this, "Hai mật khẩu chưa trùng nhau");
                    return;
                }

                resetPassword(btnDoReset, resetToken, p1, finalUserInput);
            });
        }
    }

    private void resetPassword(AppCompatButton button, String resetToken, String newPassword, String account) {
        button.setEnabled(false);
        ApiClient.create(this, AccountsApi.class)
                .resetPassword(new AccountModels.ResetPasswordRequest(resetToken, newPassword))
                .enqueue(new ApiCallback<Object>(this) {
                    @Override
                    public void onSuccess(Object data) {
                        // Server đã đăng xuất mọi thiết bị: xóa phiên cũ trên máy này nếu có
                        new SessionManager(ResetPasswordActivity.this).clear();
                        Intent intent = new Intent(ResetPasswordActivity.this, ResetSuccessActivity.class);
                        intent.putExtra(EXTRA_ACCOUNT, account);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        AppNotificationHelper.showError(ResetPasswordActivity.this, message);
                    }
                });
    }
}