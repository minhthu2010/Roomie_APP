package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
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

import java.util.Locale;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText[] otpBoxes;
    private TextView tvVerifySubtext;
    private TextView tvOtpExpiryTimer;
    private TextView tvResendCode;
    private TextView tvOtpError;
    private CountDownTimer expiryTimer;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_otp_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userInput = getIntent().getStringExtra("USER_INPUT");
        if (userInput == null || userInput.isEmpty()) {
            userInput = "linh.nguyen@email.com";
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvVerifySubtext = findViewById(R.id.tv_verify_subtext);
        tvOtpExpiryTimer = findViewById(R.id.tv_otp_expiry_timer);
        tvResendCode = findViewById(R.id.tv_resend_code);
        tvOtpError = findViewById(R.id.tv_otp_error);
        AppCompatButton btnVerify = findViewById(R.id.btn_verify);
        TextView tvChangeEmail = findViewById(R.id.tv_change_email);

        String maskedInput = maskInput(userInput);
        if (tvVerifySubtext != null) {
            tvVerifySubtext.setText("Mã 6 số đã được gửi đến " + maskedInput + ". Mã có hiệu lực trong 05:00.");
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvChangeEmail != null) {
            tvChangeEmail.setOnClickListener(v -> finish());
        }

        otpBoxes = new EditText[]{
                findViewById(R.id.et_otp_1),
                findViewById(R.id.et_otp_2),
                findViewById(R.id.et_otp_3),
                findViewById(R.id.et_otp_4),
                findViewById(R.id.et_otp_5),
                findViewById(R.id.et_otp_6)
        };

        setupOtpInputs();
        startExpiryTimer();
        startResendTimer();

        final String finalUserInput = userInput;
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                StringBuilder otpBuilder = new StringBuilder();
                for (EditText box : otpBoxes) {
                    if (box != null) {
                        otpBuilder.append(box.getText().toString().trim());
                    }
                }
                String otp = otpBuilder.toString();
                if (otp.length() < 6) {
                    if (tvOtpError != null) {
                        tvOtpError.setText("Mã chưa đủ 6 số. Vui lòng kiểm tra và nhập lại.");
                        tvOtpError.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvOtpError != null) {
                        tvOtpError.setVisibility(View.GONE);
                    }
                    AppNotificationHelper.showSuccess(VerifyOtpActivity.this, "Xác thực mã khôi phục thành công!");
                    Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("USER_INPUT", finalUserInput);
                    startActivity(intent);
                }
            });
        }
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;
            if (otpBoxes[i] == null) continue;

            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpBoxes.length - 1) {
                        otpBoxes[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpBoxes[index].getText().toString().isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus();
                        otpBoxes[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void startExpiryTimer() {
        if (expiryTimer != null) expiryTimer.cancel();
        expiryTimer = new CountDownTimer(300000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                if (tvOtpExpiryTimer != null) {
                    tvOtpExpiryTimer.setText(String.format(Locale.getDefault(), "Mã hết hạn sau %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (tvOtpExpiryTimer != null) {
                    tvOtpExpiryTimer.setText("Mã đã hết hạn.");
                }
            }
        }.start();
    }

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();
        resendTimer = new CountDownTimer(24000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                if (tvResendCode != null) {
                    tvResendCode.setText(String.format(Locale.getDefault(), "Chưa nhận được mã? Gửi lại sau %02d:%02d", 0, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (tvResendCode != null) {
                    tvResendCode.setText("Gửi lại mã ngay");
                    tvResendCode.setOnClickListener(v -> {
                        AppNotificationHelper.showInfo(VerifyOtpActivity.this, "Đã gửi lại mã xác thực mới!");
                        startResendTimer();
                    });
                }
            }
        }.start();
    }

    private String maskInput(String input) {
        if (input.contains("@")) {
            int atIndex = input.indexOf("@");
            if (atIndex > 2) {
                return input.substring(0, 2) + "•••" + input.substring(atIndex);
            }
            return input;
        } else if (input.length() >= 7) {
            return input.substring(0, 3) + " ••• " + input.substring(input.length() - 4);
        }
        return input;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expiryTimer != null) expiryTimer.cancel();
        if (resendTimer != null) resendTimer.cancel();
    }
}