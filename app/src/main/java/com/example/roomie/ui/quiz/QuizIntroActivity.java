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
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.QuizModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.home.HomeActivity;

public class QuizIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_intro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_quiz_intro_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnClose = findViewById(R.id.btn_close);
        AppCompatButton btnStartQuiz = findViewById(R.id.btn_start_quiz);
        TextView tvSkipQuiz = findViewById(R.id.tv_skip_quiz);

        // Người dùng đã đăng nhập: bỏ qua bài test thì vào Home (không về màn chào mừng)
        Runnable goToHome = () -> {
            Intent intent = new Intent(QuizIntroActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnClose != null) btnClose.setOnClickListener(v -> goToHome.run());
        if (tvSkipQuiz != null) tvSkipQuiz.setOnClickListener(v -> goToHome.run());

        if (btnStartQuiz != null) {
            btnStartQuiz.setOnClickListener(v -> {
                Intent intent = new Intent(QuizIntroActivity.this, HabitQuizActivity.class);
                startActivity(intent);
            });
        }

        loadQuestionCount();
    }

    // Số câu lấy từ bộ câu hỏi đang phát hành thay vì ghi cứng trong strings.xml.
    private void loadQuestionCount() {
        TextView tvBanner = findViewById(R.id.tv_banner_title);
        if (tvBanner == null) return;
        ApiClient.create(this, DiscoveryApi.class)
                .getQuestionnaire(DiscoveryApi.MODE_PERSONAL)
                .enqueue(new ApiCallback<QuizModels.Questionnaire>(this) {
                    @Override
                    public void onSuccess(QuizModels.Questionnaire data) {
                        if (data == null || data.totalQuestions == 0) return;
                        int minutes = Math.max(1, Math.round(data.totalQuestions * 0.4f));
                        tvBanner.setText(getString(R.string.quiz_intro_banner_format, data.totalQuestions, minutes));
                    }

                    @Override
                    public void onError(String message) {
                        // Giữ chữ mặc định; lỗi mạng sẽ báo ở màn làm bài.
                    }
                });
    }
}