package com.example.roomie.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.quiz.QuizIntroActivity;

import java.util.Calendar;
import java.util.Locale;

public class ProfileSetupActivity extends AppCompatActivity {

    public static final String EXTRA_FULL_NAME = "FULL_NAME";
    /** true: mở ngay sau đăng ký/đăng nhập lần đầu */
    public static final String EXTRA_FIRST_SETUP = "FIRST_SETUP";

    private boolean isFindRoomSelected = true;
    private String occupation;
    private String gender;
    private Integer birthYear;

    private AccountsApi api;
    private ImageView imgAvatar;
    private EditText etDisplayName;
    private TextView tvValOcc;
    private TextView tvValLoc;
    private TextView tvValGender;
    private TextView tvValBirthYear;
    private EditText etBio;
    private EditText etBudget;
    private EditText etMoveInDate;
    private TextView btnPillFindRoom;
    private TextView btnPillFindRoommate;

    private final ActivityResultLauncher<String> pickAvatar = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ProfileUi.uploadAvatar(this, uri,
                            profile -> ImageLoader.load(imgAvatar, profile.avatarUrl, R.drawable.ic_avatar_placeholder));
                }
            });

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

        api = ApiClient.create(this, AccountsApi.class);

        imgAvatar = findViewById(R.id.img_avatar);
        TextView tvChangeAvatarBtn = findViewById(R.id.tv_change_avatar_btn);
        etDisplayName = findViewById(R.id.et_display_name);
        RelativeLayout layoutSpinnerOcc = findViewById(R.id.layout_spinner_occupation);
        tvValOcc = findViewById(R.id.tv_val_occupation);
        RelativeLayout layoutSpinnerLoc = findViewById(R.id.layout_spinner_location);
        tvValLoc = findViewById(R.id.tv_val_location);
        RelativeLayout layoutSpinnerGender = findViewById(R.id.layout_spinner_gender);
        tvValGender = findViewById(R.id.tv_val_gender);
        RelativeLayout layoutSpinnerBirthYear = findViewById(R.id.layout_spinner_birth_year);
        tvValBirthYear = findViewById(R.id.tv_val_birth_year);
        etBio = findViewById(R.id.et_bio);
        etBudget = findViewById(R.id.et_budget);
        etMoveInDate = findViewById(R.id.et_move_in_date);
        btnPillFindRoom = findViewById(R.id.btn_pill_find_room);
        btnPillFindRoommate = findViewById(R.id.btn_pill_find_roommate);
        TextView tvNextQuizTitle = findViewById(R.id.tv_next_quiz_title);
        AppCompatButton btnSaveProfile = findViewById(R.id.btn_save_profile);

        // Bỏ dữ liệu mẫu trong layout, chờ hồ sơ thật từ API
        String nameExtra = getIntent().getStringExtra(EXTRA_FULL_NAME);
        etDisplayName.setText(nameExtra != null ? nameExtra : "");
        tvValOcc.setText("");
        tvValOcc.setHint(R.string.hint_choose);
        tvValLoc.setText("");
        tvValLoc.setHint(R.string.hint_choose);
        etMoveInDate.setText("");

        tvChangeAvatarBtn.setOnClickListener(v -> pickAvatar.launch("image/*"));
        imgAvatar.setOnClickListener(v -> pickAvatar.launch("image/*"));

        layoutSpinnerOcc.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Chọn nghề nghiệp")
                .setItems(ProfileUi.OCCUPATION_LABELS, (dialog, which) -> {
                    occupation = ProfileUi.OCCUPATION_VALUES[which];
                    tvValOcc.setText(ProfileUi.OCCUPATION_LABELS[which]);
                })
                .show());

        layoutSpinnerLoc.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Chọn khu vực")
                .setItems(ProfileUi.AREAS, (dialog, which) -> tvValLoc.setText(ProfileUi.AREAS[which]))
                .show());

        layoutSpinnerGender.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Chọn giới tính")
                .setItems(ProfileUi.GENDER_LABELS, (dialog, which) -> {
                    gender = ProfileUi.GENDER_VALUES[which];
                    tvValGender.setText(ProfileUi.GENDER_LABELS[which]);
                })
                .show());

        String[] years = ProfileUi.birthYears();
        layoutSpinnerBirthYear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Chọn năm sinh")
                .setItems(years, (dialog, which) -> {
                    birthYear = Integer.parseInt(years[which]);
                    tvValBirthYear.setText(years[which]);
                })
                .show());

        etMoveInDate.setFocusable(false);
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
            datePickerDialog.getDatePicker().setMinDate(c.getTimeInMillis() - 1000);
            datePickerDialog.show();
        });

        btnPillFindRoom.setOnClickListener(v -> selectGoal(true));
        btnPillFindRoommate.setOnClickListener(v -> selectGoal(false));

        tvNextQuizTitle.setOnClickListener(v -> startActivity(new Intent(this, QuizIntroActivity.class)));

        btnSaveProfile.setOnClickListener(v -> save(btnSaveProfile));

        loadProfile();
    }

    private void selectGoal(boolean findRoom) {
        isFindRoomSelected = findRoom;
        TextView selected = findRoom ? btnPillFindRoom : btnPillFindRoommate;
        TextView other = findRoom ? btnPillFindRoommate : btnPillFindRoom;
        selected.setBackgroundResource(R.drawable.bg_pill_selected);
        selected.setTextColor(getColor(R.color.text_white));
        other.setBackgroundResource(R.drawable.bg_pill_unselected);
        other.setTextColor(getColor(R.color.text_title));
    }

    // Điền lại những gì đã lưu (người dùng quay lại màn này lần 2)
    private void loadProfile() {
        api.getMyProfile().enqueue(new ApiCallback<AccountModels.Profile>(this) {
            @Override
            public void onSuccess(AccountModels.Profile p) {
                if (p == null) return;
                ImageLoader.load(imgAvatar, p.avatarUrl, R.drawable.ic_avatar_placeholder);
                if (p.displayName != null && etDisplayName.getText().toString().trim().isEmpty()) {
                    etDisplayName.setText(p.displayName);
                }
                occupation = p.occupation;
                String occLabel = ProfileUi.label(ProfileUi.OCCUPATION_LABELS, ProfileUi.OCCUPATION_VALUES, p.occupation);
                if (occLabel != null) tvValOcc.setText(occLabel);
                if (p.livingAreaText != null) tvValLoc.setText(p.livingAreaText);
                gender = p.gender;
                String genderLabel = ProfileUi.label(ProfileUi.GENDER_LABELS, ProfileUi.GENDER_VALUES, p.gender);
                if (genderLabel != null) tvValGender.setText(genderLabel);
                birthYear = p.birthYear;
                if (p.birthYear != null) tvValBirthYear.setText(String.valueOf(p.birthYear));
                if (p.bio != null) etBio.setText(p.bio);
                String budget = ProfileUi.formatBudget(p.budgetMin, p.budgetMax);
                if (budget != null) etBudget.setText(budget);
                String moveIn = ProfileUi.toUiDate(p.moveInDate);
                if (moveIn != null) etMoveInDate.setText(moveIn);
                if ("find_roommate".equals(p.onboardingGoal)) selectGoal(false);
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(ProfileSetupActivity.this, message);
            }
        });
    }

    private void save(AppCompatButton button) {
        String displayName = etDisplayName.getText().toString().trim();
        if (displayName.isEmpty()) {
            AppNotificationHelper.showError(this, "Vui lòng nhập tên hiển thị");
            return;
        }
        String budgetText = etBudget.getText().toString().trim();
        long[] budget = ProfileUi.parseBudget(budgetText);
        if (!budgetText.isEmpty() && budget == null) {
            AppNotificationHelper.showError(this, "Ngân sách chưa đúng, ví dụ: 2–4 triệu");
            return;
        }

        AccountModels.UpdateProfileRequest body = new AccountModels.UpdateProfileRequest();
        body.displayName = displayName;
        body.occupation = occupation;
        body.gender = gender;
        body.birthYear = birthYear;
        String area = tvValLoc.getText().toString().trim();
        if (!area.isEmpty()) body.livingAreaText = area;
        body.bio = etBio.getText().toString().trim();
        if (budget != null) {
            body.budgetMin = budget[0];
            body.budgetMax = budget[1];
        }
        body.moveInDate = ProfileUi.toApiDate(etMoveInDate.getText().toString());
        body.onboardingGoal = isFindRoomSelected ? "find_room" : "find_roommate";
        body.completeSetup = true;

        button.setEnabled(false);
        api.updateMyProfile(body).enqueue(new ApiCallback<AccountModels.Profile>(this) {
            @Override
            public void onSuccess(AccountModels.Profile profile) {
                AppNotificationHelper.showSuccess(ProfileSetupActivity.this, "Đã lưu hồ sơ thành công!");
                button.setEnabled(true);
                if (getIntent().getBooleanExtra(EXTRA_FIRST_SETUP, false)) {
                    // Giữ màn này dưới bài test để nút quay lại ở bước 3 trở về đây
                    startActivity(new Intent(ProfileSetupActivity.this, QuizIntroActivity.class));
                } else {
                    finish();
                }
            }

            @Override
            public void onError(String message) {
                button.setEnabled(true);
                AppNotificationHelper.showError(ProfileSetupActivity.this, message);
            }
        });
    }
}
