package com.example.roomie.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.remote.SessionManager;
import com.example.roomie.ui.home.HomeActivity;

import java.util.Locale;

public class QuizSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_ANSWERED = "ANSWERED_COUNT";
    public static final String EXTRA_TOTAL = "TOTAL_QUESTIONS";
    public static final String EXTRA_DURATION_SEC = "DURATION_SEC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_success);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_quiz_success_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // TODO: lấy tên hiển thị từ API hồ sơ khi A làm xong
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = new SessionManager(this).getGreetingName();
        }
        if (userName == null) {
            userName = "bạn";
        }

        ImageButton btnClose = findViewById(R.id.btn_close);
        TextView tvSuccessHeading = findViewById(R.id.tv_success_heading);
        AppCompatButton btnExploreRooms = findViewById(R.id.btn_explore_rooms);

        if (tvSuccessHeading != null) {
            tvSuccessHeading.setText(getString(R.string.quiz_success_heading, userName));
        }

        int total = getIntent().getIntExtra(EXTRA_TOTAL, 0);
        int answered = getIntent().getIntExtra(EXTRA_ANSWERED, total);
        long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, -1);

        TextView tvAnswered = findViewById(R.id.tv_stat1_val);
        if (tvAnswered != null && total > 0) {
            tvAnswered.setText(String.format(Locale.getDefault(), "%d/%d", answered, total));
        }
        TextView tvDuration = findViewById(R.id.tv_stat2_val);
        if (tvDuration != null && durationSec >= 0) {
            tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", durationSec / 60, durationSec % 60));
        }

        Runnable goToHome = () -> {
            // Xóa các màn onboarding (thiết lập hồ sơ, bài test) khỏi back stack
            Intent intent = new Intent(QuizSuccessActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnClose != null) btnClose.setOnClickListener(v -> goToHome.run());
        if (btnExploreRooms != null) btnExploreRooms.setOnClickListener(v -> goToHome.run());
    }
}