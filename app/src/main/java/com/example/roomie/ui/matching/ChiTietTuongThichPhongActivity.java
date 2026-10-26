package com.example.roomie.ui.matching;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.MatchingModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.quiz.QuizIntroActivity;

/**
 * Tương thích giữa tôi và PHÒNG: câu trả lời cá nhân so với bài test nếp sinh hoạt của phòng
 * (bước 6 đăng bài), hiện từng tiêu chí. Extra bắt buộc: EXTRA_POST_ID.
 */
public class ChiTietTuongThichPhongActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";

    public static Intent intent(Context context, String postId) {
        return new Intent(context, ChiTietTuongThichPhongActivity.class).putExtra(EXTRA_POST_ID, postId);
    }

    private String postId;
    private TextView tvRoomTitle;
    private TextView tvRoomAddress;
    private TextView tvPercent;
    private TextView tvLevel;
    private TextView tvNote;
    private TextView tvCriteriaTitle;
    private LinearLayout layoutCriteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chi_tiet_tuong_thich_phong);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(view -> finish());

        View btnBackToRoom = findViewById(R.id.btn_back_to_room);
        if (btnBackToRoom != null) btnBackToRoom.setOnClickListener(view -> finish());

        tvRoomTitle = findViewById(R.id.tv_room_title);
        tvRoomAddress = findViewById(R.id.tv_room_address);
        tvPercent = findViewById(R.id.tv_room_percent);
        tvLevel = findViewById(R.id.tv_room_level);
        tvNote = findViewById(R.id.tv_room_note);
        tvCriteriaTitle = findViewById(R.id.tv_criteria_title);
        layoutCriteria = findViewById(R.id.layout_criteria);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) tvNote.setText(R.string.match_missing_post);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (postId != null) load();
    }

    private void load() {
        tvNote.setOnClickListener(null);
        ApiClient.create(this, DiscoveryApi.class).getCompatibility(postId)
                .enqueue(new ApiCallback<MatchingModels.Compatibility>(this) {
                    @Override
                    public void onSuccess(MatchingModels.Compatibility data) {
                        if (data != null) render(data);
                    }

                    @Override
                    public void onError(String message) {
                        tvLevel.setText(R.string.match_no_data);
                        tvNote.setText(getString(R.string.match_load_error, message));
                        tvNote.setOnClickListener(v -> load());
                    }
                });
    }

    private void render(MatchingModels.Compatibility data) {
        if (data.post != null) {
            if (data.post.title != null) tvRoomTitle.setText(data.post.title);
            tvRoomAddress.setText(data.post.addressText);
        }

        MatchingModels.Comparison room = data.room;
        MatchUi.bindScore(tvPercent, tvLevel, room);

        boolean roomHasTest = false;
        for (MatchingModels.Criterion c : room.criteria) {
            if (c.otherAnswer != null) roomHasTest = true;
        }
        if (data.isOwnPost) {
            tvNote.setText(R.string.match_own_post);
        } else if (data.seekerAnsweredCount == 0) {
            tvNote.setText(R.string.match_need_quiz);
            tvNote.setOnClickListener(v -> startActivity(new Intent(this, QuizIntroActivity.class)));
        } else if (!roomHasTest) {
            tvNote.setText(R.string.match_room_no_test);
        } else {
            tvNote.setText(getString(R.string.match_room_note, room.answeredBoth, data.totalQuestions));
        }

        tvCriteriaTitle.setText(getString(R.string.match_criteria_title, room.criteria.size()));

        LayoutInflater inflater = LayoutInflater.from(this);
        layoutCriteria.removeAllViews();
        for (MatchingModels.Criterion c : room.criteria) {
            View card = inflater.inflate(R.layout.item_compat_criterion, layoutCriteria, false);
            ((TextView) card.findViewById(R.id.tv_criterion_title)).setText(c.text);
            bindStatus(card.findViewById(R.id.tv_criterion_status), c.status);
            ((TextView) card.findViewById(R.id.tv_criterion_seeker)).setText(
                    getString(R.string.match_seeker_answer, orNotAnswered(c.seekerAnswer)));
            ((TextView) card.findViewById(R.id.tv_criterion_other)).setText(
                    getString(R.string.match_room_answer, orNotAnswered(c.otherAnswer)));
            layoutCriteria.addView(card);
        }
    }

    private String orNotAnswered(String answer) {
        return answer != null ? answer : getString(R.string.match_not_answered);
    }

    // Cùng kích thước chữ/padding (style CompatibilityStatusGood), chỉ đổi nền theo trạng thái
    private void bindStatus(TextView tv, String status) {
        if (MatchingModels.STATUS_MATCH.equals(status)) {
            tv.setText(R.string.match_status_match);
            tv.setBackgroundResource(R.drawable.bg_badge_sage);
        } else if (MatchingModels.STATUS_CLOSE.equals(status)) {
            tv.setText(R.string.match_status_close);
            tv.setBackgroundResource(R.drawable.bg_badge_mint);
        } else if (MatchingModels.STATUS_DISCUSS.equals(status)) {
            tv.setText(R.string.match_status_discuss);
            tv.setBackgroundResource(R.drawable.bg_chip_unselected);
        } else {
            tv.setText(R.string.match_status_unknown);
            tv.setBackgroundResource(R.drawable.bg_card_outline);
        }
        // setBackgroundResource xóa padding của style, đặt lại
        float d = tv.getResources().getDisplayMetrics().density;
        tv.setPadding((int) (9 * d), (int) (4 * d), (int) (9 * d), (int) (4 * d));
    }
}
