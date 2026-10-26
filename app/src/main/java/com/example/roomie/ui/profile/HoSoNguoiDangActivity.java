package com.example.roomie.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.quiz.QuizUi;
import com.example.roomie.ui.search.RoomCardBinder;

import java.util.Calendar;
import java.util.Locale;

/** Hồ sơ công khai của người đăng. Extra bắt buộc: EXTRA_USER_ID. */
public class HoSoNguoiDangActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "USER_ID";

    public static Intent intent(Context context, String userId) {
        return new Intent(context, HoSoNguoiDangActivity.class).putExtra(EXTRA_USER_ID, userId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ho_so_nguoi_dang);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null) {
            finish();
            return;
        }
        ApiClient.create(this, DiscoveryApi.class).getPublicProfile(userId)
                .enqueue(new ApiCallback<RoomModels.PublicProfile>(this) {
                    @Override
                    public void onSuccess(RoomModels.PublicProfile data) {
                        if (data != null) render(data);
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(HoSoNguoiDangActivity.this, message);
                    }
                });
    }

    private void render(RoomModels.PublicProfile p) {
        ImageLoader.load(findViewById(R.id.img_avatar_hero), p.avatarUrl, R.drawable.img_avatar_host);
        TextView tvName = findViewById(R.id.tv_profile_name);
        TextView tvOccupation = findViewById(R.id.tv_profile_occupation);
        TextView tvAbout = findViewById(R.id.tv_about);

        if (p.isHidden) {
            tvName.setText(p.displayName);
            tvOccupation.setText("");
            tvAbout.setText(R.string.profile_hidden);
            findViewById(R.id.tv_habits_title).setVisibility(View.GONE);
            findViewById(R.id.card_profile_match).setVisibility(View.GONE);
            return;
        }

        // "Ngọc Anh, 23 tuổi"
        tvName.setText(p.birthYear != null
                ? getString(R.string.profile_name_age_fmt, p.displayName, Calendar.getInstance().get(Calendar.YEAR) - p.birthYear)
                : p.displayName);

        // "Sinh viên · ĐH Bách Khoa TP.HCM" + "Thành viên từ 2026"
        String occupation = "student".equals(p.occupation) ? getString(R.string.occupation_student)
                : "worker".equals(p.occupation) ? getString(R.string.occupation_worker) : null;
        String line = occupation != null && p.schoolOrWorkplace != null
                ? getString(R.string.profile_occupation_fmt, occupation, p.schoolOrWorkplace)
                : occupation != null ? occupation : p.schoolOrWorkplace;
        String since = QuizUi.formatDate(p.memberSince);
        String member = since != null ? getString(R.string.profile_member_since, since.substring(since.length() - 4)) : null;
        tvOccupation.setText(line != null && member != null ? line + "\n" + member : line != null ? line : member);

        ((TextView) findViewById(R.id.tv_about_title)).setText(getString(R.string.about_poster_title_fmt, p.displayName));
        tvAbout.setText(p.bio != null && !p.bio.isEmpty() ? p.bio : getString(R.string.profile_no_bio));

        // Thói quen nổi bật
        LinearLayout habits = findViewById(R.id.layout_habits);
        habits.removeAllViews();
        if (p.habits.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.profile_no_habits);
            empty.setTextColor(getColor(R.color.text_secondary));
            habits.addView(empty);
        }
        for (RoomModels.Habit h : p.habits) {
            View chip = LayoutInflater.from(this).inflate(R.layout.item_detail_chip, habits, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) chip.getLayoutParams();
            lp.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
            ((TextView) chip.findViewById(R.id.tv_chip_text)).setText(h.label);
            ImageView icon = chip.findViewById(R.id.img_chip_icon);
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(QuizUi.iconFor(h.questionKey));
            icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary_maroon)));
            habits.addView(chip);
        }

        // Mức tương thích giữa tôi và người này (ẩn khi xem hồ sơ của chính mình)
        View matchCard = findViewById(R.id.card_profile_match);
        if (p.isMe || p.matchPercent == null) {
            matchCard.setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.tv_profile_match))
                    .setText(String.format(Locale.getDefault(), "%d%%", p.matchPercent));
            ((ProgressBar) findViewById(R.id.pb_profile_match)).setProgress(p.matchPercent);
            ((TextView) findViewById(R.id.tv_profile_match_level)).setText(p.matchLevel);
        }

        // Bài đăng đang tìm người
        TextView postsTitle = findViewById(R.id.tv_posts_title);
        LinearLayout posts = findViewById(R.id.layout_profile_posts);
        posts.removeAllViews();
        postsTitle.setVisibility(p.activePosts.isEmpty() ? View.GONE : View.VISIBLE);
        postsTitle.setText(getString(R.string.profile_posts_title, p.activePosts.size()));
        for (int i = 0; i < p.activePosts.size(); i++) {
            posts.addView(RoomCardBinder.create(this, posts, p.activePosts.get(i), i));
        }
    }
}
