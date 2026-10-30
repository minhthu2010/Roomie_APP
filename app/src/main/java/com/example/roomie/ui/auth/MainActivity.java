package com.example.roomie.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;

public class MainActivity extends AppCompatActivity {

    /** TokenAuthenticator mở màn này khi phiên đăng nhập hết hạn */
    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppCompatButton btnLogin = findViewById(R.id.btn_login);
        AppCompatButton btnRegister = findViewById(R.id.btn_register);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            AppNotificationHelper.showInfo(this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }
}