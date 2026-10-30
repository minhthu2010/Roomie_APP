package com.example.roomie.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.QuizModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;
import com.example.roomie.ui.auth.AuthFlow;
import com.example.roomie.ui.quiz.LifestyleReviewActivity;
import com.example.roomie.ui.quiz.QuizIntroActivity;
import com.example.roomie.ui.quiz.QuizUi;

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

        View cardHabits = findViewById(R.id.card_habits_quiz);
        if (cardHabits != null) {
            cardHabits.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, LifestyleReviewActivity.class)));
        }

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
                    .setPositiveButton(R.string.btn_logout, (dialog, which) -> AuthFlow.logout(this))
                    .show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
        loadHabitsStatus();
    }

    // Thẻ người dùng: tên, nghề nghiệp · khu vực, ảnh đại diện
    private void loadProfile() {
        TextView tvName = findViewById(R.id.tv_profile_name);
        TextView tvSub = findViewById(R.id.tv_profile_sub);
        ImageView ivAvatar = findViewById(R.id.iv_profile_avatar);
        // Hiện ngay tên đã lưu (không để lộ tên mẫu "Mai Linh" trong lúc chờ API)
        SessionManager session = new SessionManager(this);
        String cachedName = session.getDisplayName();
        tvName.setText(cachedName != null ? cachedName : "");
        ApiClient.create(this, AccountsApi.class).getMyProfile()
                .enqueue(new ApiCallback<AccountModels.Profile>(this) {
                    @Override
                    public void onSuccess(AccountModels.Profile profile) {
                        if (profile == null) return;
                        session.saveDisplayName(profile.displayName);
                        tvName.setText(profile.displayName);
                        tvSub.setText(ProfileUi.subtitle(profile));
                        ImageLoader.load(ivAvatar, profile.avatarUrl, R.drawable.ic_avatar_profile);
                    }

                    @Override
                    public void onError(String message) {
                        // Giữ thẻ mặc định; lỗi đăng nhập đã được TokenAuthenticator xử lý.
                    }
                });
    }

    // Thẻ "Hồ sơ thói quen sống": trạng thái + ngày cập nhật lấy từ API (người C)
    private void loadHabitsStatus() {
        TextView tvBadge = findViewById(R.id.tv_quiz_completed);
        TextView tvDate = findViewById(R.id.tv_habits_date);
        ProgressBar pbHabits = findViewById(R.id.pb_habits);
        if (tvBadge == null || tvDate == null || pbHabits == null) return;

        ApiClient.create(this, DiscoveryApi.class).getMyLifestyleTest()
                .enqueue(new ApiCallback<QuizModels.LifestyleTest>(this) {
                    @Override
                    public void onSuccess(QuizModels.LifestyleTest test) {
                        if (test == null) return;
                        int total = Math.max(test.totalQuestions, 1);
                        pbHabits.setProgress(test.answeredCount * 100 / total);
                        if (test.isCompleted) {
                            tvBadge.setText(R.string.badge_quiz_completed);
                        } else if (test.answeredCount > 0) {
                            tvBadge.setText(getString(R.string.lifestyle_profile_in_progress,
                                    test.answeredCount, test.totalQuestions));
                        } else {
                            tvBadge.setText(R.string.lifestyle_profile_not_done);
                        }
                        String date = QuizUi.formatDate(test.updatedAt);
                        tvDate.setText(date != null
                                ? getString(R.string.lifestyle_profile_updated, date)
                                : getString(R.string.lifestyle_profile_tap_to_view));
                    }

                    @Override
                    public void onError(String message) {
                        // Giữ nguyên thẻ; lỗi sẽ hiện khi mở màn xem lại.
                    }
                });
    }

    private void openSubscreen(int viewId, int layoutId) {
        View view = findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> ProfileSubscreenActivity.open(this, layoutId));
    }
}