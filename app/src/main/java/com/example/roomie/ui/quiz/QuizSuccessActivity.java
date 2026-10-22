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
import com.example.roomie.ui.home.HomeActivity;

public class QuizSuccessActivity extends AppCompatActivity {

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

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = "Linh";
        }

        ImageButton btnClose = findViewById(R.id.btn_close);
        TextView tvSuccessHeading = findViewById(R.id.tv_success_heading);
        AppCompatButton btnExploreRooms = findViewById(R.id.btn_explore_rooms);

        if (tvSuccessHeading != null) {
            tvSuccessHeading.setText(getString(R.string.quiz_success_heading, userName));
        }

        Runnable goToHome = () -> {
            Intent intent = new Intent(QuizSuccessActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnClose != null) btnClose.setOnClickListener(v -> goToHome.run());
        if (btnExploreRooms != null) btnExploreRooms.setOnClickListener(v -> goToHome.run());
    }
}