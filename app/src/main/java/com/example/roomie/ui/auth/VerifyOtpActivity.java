package com.example.roomie.ui.auth;

import android.app.Activity;
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
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Nhập mã khôi phục mật khẩu (bước 2 của Quên mật khẩu). */
public class VerifyOtpActivity extends AppCompatActivity {

    private static final String EXTRA_ACCOUNT = "USER_INPUT";
    private static final String EXTRA_MASKED_EMAIL = "MASKED_EMAIL";
    private static final String EXTRA_EXPIRES_AT = "EXPIRES_AT";
    private static final String EXTRA_RESEND_AT = "RESEND_AVAILABLE_AT";
    private static final long DEFAULT_EXPIRY_MS = 5 * 60_000;
    private static final long DEFAULT_RESEND_MS = 30_000;

    private String account;
    private AccountsApi api;
    private EditText[] otpBoxes;
    private TextView tvVerifySubtext;
    private TextView tvOtpExpiryTimer;
    private TextView tvResendCode;
    private TextView tvOtpError;
    private CountDownTimer expiryTimer;
    private CountDownTimer resendTimer;

    /** challenge null: vừa gửi mã trước đó, dùng thời gian mặc định. */
    public static void open(Activity activity, String account, AccountModels.OtpChallenge challenge) {
        Intent intent = new Intent(activity, VerifyOtpActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        if (challenge != null) {
            intent.putExtra(EXTRA_MASKED_EMAIL, challenge.email);
            intent.putExtra(EXTRA_EXPIRES_AT, challenge.expiresAt);
            intent.putExtra(EXTRA_RESEND_AT, challenge.resendAvailableAt);
        }
        activity.startActivity(intent);
    }

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

        account = getIntent().getStringExtra(EXTRA_ACCOUNT);
        if (account == null || account.isEmpty()) {
            finish();
            return;
        }
        api = ApiClient.create(this, AccountsApi.class);

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvVerifySubtext = findViewById(R.id.tv_verify_subtext);
        tvOtpExpiryTimer = findViewById(R.id.tv_otp_expiry_timer);
        tvResendCode = findViewById(R.id.tv_resend_code);
        tvOtpError = findViewById(R.id.tv_otp_error);
        AppCompatButton btnVerify = findViewById(R.id.btn_verify);
        TextView tvChangeEmail = findViewById(R.id.tv_change_email);

        showDestination(getIntent().getStringExtra(EXTRA_MASKED_EMAIL));

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
        startExpiryTimer(millisUntil(getIntent().getStringExtra(EXTRA_EXPIRES_AT), DEFAULT_EXPIRY_MS));
        startResendTimer(millisUntil(getIntent().getStringExtra(EXTRA_RESEND_AT), DEFAULT_RESEND_MS));

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
                    showOtpError("Mã chưa đủ 6 số. Vui lòng kiểm tra và nhập lại.");
                } else {
                    if (tvOtpError != null) tvOtpError.setVisibility(View.GONE);
                    verify(btnVerify, otp);
                }
            });
        }
    }

    private void showDestination(String maskedEmail) {
        if (tvVerifySubtext == null) return;
        String target = maskEmail(maskedEmail != null ? maskedEmail : account);
        tvVerifySubtext.setText("Mã 6 số đã được gửi đến " + target + ". Mã có hiệu lực trong 05:00.");
    }

    /** "mailinh@roomie.test" -> "ma•••@roomie.test" */
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return at > 2 ? email.substring(0, 2) + "•••" + email.substring(at) : email;
    }

    private void showOtpError(String message) {
        if (tvOtpError != null) {
            tvOtpError.setText(message);
            tvOtpError.setVisibility(View.VISIBLE);
        } else {
            AppNotificationHelper.showError(this, message);
        }
    }

    private void verify(AppCompatButton button, String otp) {
        button.setEnabled(false);
        api.verifyResetOtp(new AccountModels.VerifyResetOtpRequest(account, otp))
                .enqueue(new ApiCallback<AccountModels.ResetToken>(this) {
                    @Override
                    public void onSuccess(AccountModels.ResetToken result) {
                        button.setEnabled(true);
                        AppNotificationHelper.showSuccess(VerifyOtpActivity.this, "Xác thực mã khôi phục thành công!");
                        ResetPasswordActivity.open(VerifyOtpActivity.this, account, result.resetToken);
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        showOtpError(message);
                        clearOtp();
                    }
                });
    }

    private void resend() {
        if (tvResendCode != null) tvResendCode.setOnClickListener(null);
        api.forgotPassword(new AccountModels.EmailRequest(account))
                .enqueue(new ApiCallback<AccountModels.OtpChallenge>(this) {
                    @Override
                    public void onSuccess(AccountModels.OtpChallenge challenge) {
                        AppNotificationHelper.showInfo(VerifyOtpActivity.this, "Đã gửi lại mã xác thực mới!");
                        if (tvOtpError != null) tvOtpError.setVisibility(View.GONE);
                        clearOtp();
                        showDestination(challenge.email);
                        startExpiryTimer(millisUntil(challenge.expiresAt, DEFAULT_EXPIRY_MS));
                        startResendTimer(millisUntil(challenge.resendAvailableAt, DEFAULT_RESEND_MS));
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(VerifyOtpActivity.this, message);
                        startResendTimer(0);
                    }
                });
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

    private void clearOtp() {
        for (EditText box : otpBoxes) {
            if (box != null) box.setText("");
        }
        if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
    }

    private void startExpiryTimer(long durationMs) {
        if (expiryTimer != null) expiryTimer.cancel();
        expiryTimer = new CountDownTimer(durationMs, 1000) {
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

    private void startResendTimer(long durationMs) {
        if (resendTimer != null) resendTimer.cancel();
        if (tvResendCode != null) tvResendCode.setOnClickListener(null);
        resendTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                if (tvResendCode != null) {
                    tvResendCode.setText(String.format(Locale.getDefault(),
                            "Chưa nhận được mã? Gửi lại sau %02d:%02d", seconds / 60, seconds % 60));
                }
            }

            @Override
            public void onFinish() {
                if (tvResendCode != null) {
                    tvResendCode.setText("Gửi lại mã ngay");
                    tvResendCode.setOnClickListener(v -> resend());
                }
            }
        }.start();
    }

    /** Thời gian còn lại tới mốc ISO (UTC) server trả về; không đọc được thì dùng mặc định. */
    private static long millisUntil(String isoUtc, long fallback) {
        if (isoUtc == null) return fallback;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = format.parse(isoUtc);
            return date == null ? fallback : Math.max(0, date.getTime() - System.currentTimeMillis());
        } catch (ParseException e) {
            return fallback;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expiryTimer != null) expiryTimer.cancel();
        if (resendTimer != null) resendTimer.cancel();
    }
}
