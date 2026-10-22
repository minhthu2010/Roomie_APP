package com.example.roomie.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.ui.quiz.QuizIntroActivity;

import java.util.Calendar;
import java.util.Locale;

public class ProfileSetupActivity extends AppCompatActivity {

    private boolean isFindRoomSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_setup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_profile_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvChangeAvatarBtn = findViewById(R.id.tv_change_avatar_btn);
        EditText etDisplayName = findViewById(R.id.et_display_name);
        RelativeLayout layoutSpinnerOcc = findViewById(R.id.layout_spinner_occupation);
        TextView tvValOcc = findViewById(R.id.tv_val_occupation);
        RelativeLayout layoutSpinnerLoc = findViewById(R.id.layout_spinner_location);
        TextView tvValLoc = findViewById(R.id.tv_val_location);
        EditText etMoveInDate = findViewById(R.id.et_move_in_date);
        TextView btnPillFindRoom = findViewById(R.id.btn_pill_find_room);
        TextView btnPillFindRoommate = findViewById(R.id.btn_pill_find_roommate);
        TextView tvNextQuizTitle = findViewById(R.id.tv_next_quiz_title);
        AppCompatButton btnSaveProfile = findViewById(R.id.btn_save_profile);

        String nameExtra = getIntent().getStringExtra("FULL_NAME");
        if (nameExtra != null && !nameExtra.isEmpty() && etDisplayName != null) {
            etDisplayName.setText(nameExtra);
        } else if (etDisplayName != null) {
            etDisplayName.setText("Mai Linh");
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvChangeAvatarBtn != null) {
            tvChangeAvatarBtn.setOnClickListener(v ->
                AppNotificationHelper.showInfo(ProfileSetupActivity.this, "Chọn ảnh đại diện từ thiết bị")
            );
        }

        String[] occupations = {"Sinh viên", "Người đi làm", "Tự do (Freelancer)", "Khác"};
        if (layoutSpinnerOcc != null && tvValOcc != null) {
            layoutSpinnerOcc.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSetupActivity.this);
                builder.setTitle("Chọn nghề nghiệp");
                builder.setItems(occupations, (dialog, which) -> tvValOcc.setText(occupations[which]));
                builder.show();
            });
        }

        String[] locations = {"Quận 1", "Quận 3", "Quận 5", "Quận 7", "Quận 10", "Bình Thạnh", "Thủ Đức", "Tân Bình", "Khác"};
        if (layoutSpinnerLoc != null && tvValLoc != null) {
            layoutSpinnerLoc.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSetupActivity.this);
                builder.setTitle("Chọn khu vực");
                builder.setItems(locations, (dialog, which) -> tvValLoc.setText(locations[which]));
                builder.show();
            });
        }

        if (etMoveInDate != null) {
            etMoveInDate.setText("15/10/2026");
            etMoveInDate.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        ProfileSetupActivity.this,
                        (view, year, month, dayOfMonth) ->
                                etMoveInDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year)),
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            });
        }

        if (btnPillFindRoom != null && btnPillFindRoommate != null) {
            btnPillFindRoom.setOnClickListener(v -> {
                isFindRoomSelected = true;
                btnPillFindRoom.setBackgroundResource(R.drawable.bg_pill_selected);
                btnPillFindRoom.setTextColor(getColor(R.color.text_white));

                btnPillFindRoommate.setBackgroundResource(R.drawable.bg_pill_unselected);
                btnPillFindRoommate.setTextColor(getColor(R.color.text_title));
            });

            btnPillFindRoommate.setOnClickListener(v -> {
                isFindRoomSelected = false;
                btnPillFindRoommate.setBackgroundResource(R.drawable.bg_pill_selected);
                btnPillFindRoommate.setTextColor(getColor(R.color.text_white));

                btnPillFindRoom.setBackgroundResource(R.drawable.bg_pill_unselected);
                btnPillFindRoom.setTextColor(getColor(R.color.text_title));
            });
        }

        if (tvNextQuizTitle != null) {
            tvNextQuizTitle.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileSetupActivity.this, QuizIntroActivity.class);
                startActivity(intent);
            });
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                AppNotificationHelper.showSuccess(ProfileSetupActivity.this, "Đã lưu hồ sơ thành công!");
                Intent intent = new Intent(ProfileSetupActivity.this, QuizIntroActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}