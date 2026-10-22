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
import com.example.roomie.ui.auth.MainActivity;

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

        ImageButton btnClose = findViewById(R.id.btn_close);
        AppCompatButton btnStartQuiz = findViewById(R.id.btn_start_quiz);
        TextView tvSkipQuiz = findViewById(R.id.tv_skip_quiz);

        Runnable goToMain = () -> {
            Intent intent = new Intent(QuizIntroActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnClose != null) btnClose.setOnClickListener(v -> goToMain.run());
        if (tvSkipQuiz != null) tvSkipQuiz.setOnClickListener(v -> goToMain.run());

        if (btnStartQuiz != null) {
            btnStartQuiz.setOnClickListener(v -> {
                Intent intent = new Intent(QuizIntroActivity.this, HabitQuizActivity.class);
                startActivity(intent);
            });
        }
    }
}