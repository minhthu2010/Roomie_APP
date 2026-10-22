package com.example.roomie.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.ui.auth.MainActivity;
import com.example.roomie.ui.quiz.QuizIntroActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootProfile), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        openSubscreen(R.id.card_user_profile, R.layout.layout_edit_profile);
        openSubscreen(R.id.iv_edit_profile_icon, R.layout.layout_edit_profile);
        openSubscreen(R.id.menu_personal_info, R.layout.layout_edit_profile);
        openSubscreen(R.id.menu_verification_safety, R.layout.layout_verification_safety);
        openSubscreen(R.id.menu_notifications, R.layout.layout_notifications);
        openSubscreen(R.id.menu_settings, R.layout.layout_settings);

        View btnRetakeQuiz = findViewById(R.id.btn_retake_quiz);
        if (btnRetakeQuiz != null) {
            btnRetakeQuiz.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, QuizIntroActivity.class);
                startActivity(intent);
            });
        }

        View logout = findViewById(R.id.btn_logout_row);
        if (logout != null) {
            logout.setOnClickListener(view -> new AlertDialog.Builder(this)
                    .setTitle(R.string.box_logout_confirm_title)
                    .setMessage(R.string.box_logout_confirm_sub)
                    .setNegativeButton(R.string.btn_stay, null)
                    .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .show());
        }
    }

    private void openSubscreen(int viewId, int layoutId) {
        View view = findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> ProfileSubscreenActivity.open(this, layoutId));
    }
}