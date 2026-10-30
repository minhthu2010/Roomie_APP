package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Đã đăng nhập: kiểm tra token (hết hạn thì TokenAuthenticator tự refresh) rồi vào thẳng app
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (new SessionManager(this).getRefreshToken() != null) {
                checkSession();
            } else {
                openWelcome();
            }
        }, 1200);
    }

    private void checkSession() {
        ApiClient.create(this, AccountsApi.class).getMe().enqueue(new ApiCallback<AccountModels.User>(this) {
            @Override
            public void onSuccess(AccountModels.User user) {
                AuthFlow.openApp(SplashActivity.this, user);
            }

            @Override
            public void onError(String message) {
                SessionManager session = new SessionManager(SplashActivity.this);
                if (getHttpStatus() == 0 && session.isLoggedIn()) {
                    // Mất mạng: vẫn cho vào app, các màn sẽ tự báo lỗi kết nối
                    AuthFlow.openApp(SplashActivity.this, null);
                } else {
                    session.clear();
                    openWelcome();
                }
            }
        });
    }

    private void openWelcome() {
        if (isFinishing()) return;
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}