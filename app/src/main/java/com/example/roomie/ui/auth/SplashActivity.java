package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;

/**
 * Màn chờ lúc mở app. Android 12+ luôn tự hiện màn chờ hệ thống (icon app trên nền trắng), nên dùng
 * chính màn đó (thư viện core-splashscreen, cũng chạy trên Android cũ) và giữ nó trên màn hình trong lúc
 * kiểm tra đăng nhập, thay vì hiện thêm một màn chờ riêng (logo + chữ Roomie) sau nó.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);   // phải gọi trước super.onCreate
        super.onCreate(savedInstanceState);
        // Giữ màn chờ cho đến khi chuyển sang màn tiếp theo (không lộ khung trắng ở giữa)
        splash.setKeepOnScreenCondition(() -> true);

        // Đã đăng nhập: kiểm tra token (hết hạn thì TokenAuthenticator tự refresh) rồi vào thẳng app
        if (new SessionManager(this).getRefreshToken() != null) {
            checkSession();
        } else {
            openWelcome();
        }
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