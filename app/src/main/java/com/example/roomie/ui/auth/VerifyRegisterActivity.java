package com.example.roomie.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
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

public class VerifyRegisterActivity extends AppCompatActivity {

    private static final String EXTRA_EMAIL = "USER_INPUT";
    private static final String EXTRA_RESEND_AT = "RESEND_AVAILABLE_AT";
    private static final long DEFAULT_RESEND_MS = 30_000;

    private String email;
    private AccountsApi api;
    private EditText[] otpBoxes;
    private TextView tvResendCode;
    private CountDownTimer resendTimer;

    /** Mở màn xác thực sau khi đăng ký. challenge null: dùng thời gian chờ mặc định. */
    public static void open(Activity activity, String email, AccountModels.OtpChallenge challenge) {
        Intent intent = new Intent(activity, VerifyRegisterActivity.class);
        intent.putExtra(EXTRA_EMAIL, email);
        if (challenge != null) intent.putExtra(EXTRA_RESEND_AT, challenge.resendAvailableAt);
        activity.startActivity(intent);
    }

    /** Tài khoản đăng ký dở (đăng nhập báo chưa xác thực): gửi mã mới rồi mở màn xác thực. */
    public static void resendAndOpen(Activity activity, String email) {
        ApiClient.create(activity, AccountsApi.class)
                .resendRegisterOtp(new AccountModels.EmailRequest(email))
                .enqueue(new ApiCallback<AccountModels.OtpChallenge>(activity) {
                    @Override
                    public void onSuccess(AccountModels.OtpChallenge challenge) {
                        AppNotificationHelper.showInfo(activity, "Tài khoản chưa xác thực, mã mới đã được gửi tới email");
                        open(activity, email, challenge);
                    }

                    @Override
                    public void onError(String message) {
                        if ("OTP_TOO_SOON".equals(getErrorCode())) {
                            open(activity, email, null);
                        } else {
                            AppNotificationHelper.showError(activity, message);
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_verify_reg_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.isEmpty()) {
            finish();
            return;
        }
        api = ApiClient.create(this, AccountsApi.class);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvVerifySubtext = findViewById(R.id.tv_verify_subtext);
        tvResendCode = findViewById(R.id.tv_resend_code);
        AppCompatButton btnVerify = findViewById(R.id.btn_verify);

        if (tvVerifySubtext != null) {
            tvVerifySubtext.setText(getString(R.string.register_verify_subtext, maskEmail(email)));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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
        startResendTimer(millisUntil(getIntent().getStringExtra(EXTRA_RESEND_AT)));

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
                    AppNotificationHelper.showError(VerifyRegisterActivity.this, "Vui lòng nhập đủ 6 số xác thực");
                } else {
                    verify(btnVerify, otp);
                }
            });
        }
    }

    private void verify(AppCompatButton button, String otp) {
        button.setEnabled(false);
        api.verifyRegister(new AccountModels.VerifyOtpRequest(email, otp))
                .enqueue(new ApiCallback<AccountModels.AuthResult>(this) {
                    @Override
                    public void onSuccess(AccountModels.AuthResult result) {
                        AppNotificationHelper.showSuccess(VerifyRegisterActivity.this, "Đăng ký tài khoản thành công!");
                        AuthFlow.onAuthenticated(VerifyRegisterActivity.this, result);
                    }

                    @Override
                    public void onError(String message) {
                        button.setEnabled(true);
                        AppNotificationHelper.showError(VerifyRegisterActivity.this, message);
                        clearOtp();
                    }
                });
    }

    private void resend() {
        if (tvResendCode != null) tvResendCode.setOnClickListener(null);
        api.resendRegisterOtp(new AccountModels.EmailRequest(email))
                .enqueue(new ApiCallback<AccountModels.OtpChallenge>(this) {
                    @Override
                    public void onSuccess(AccountModels.OtpChallenge challenge) {
                        AppNotificationHelper.showInfo(VerifyRegisterActivity.this, "Đã gửi lại mã xác thực mới!");
                        clearOtp();
                        startResendTimer(millisUntil(challenge.resendAvailableAt));
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(VerifyRegisterActivity.this, message);
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
    private static long millisUntil(String isoUtc) {
        if (isoUtc == null) return DEFAULT_RESEND_MS;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = format.parse(isoUtc);
            return date == null ? DEFAULT_RESEND_MS : Math.max(0, date.getTime() - System.currentTimeMillis());
        } catch (ParseException e) {
            return DEFAULT_RESEND_MS;
        }
    }

    private String maskEmail(String input) {
        int atIndex = input.indexOf("@");
        if (atIndex > 2) {
            return input.substring(0, 2) + "•••" + input.substring(atIndex);
        }
        return input;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
