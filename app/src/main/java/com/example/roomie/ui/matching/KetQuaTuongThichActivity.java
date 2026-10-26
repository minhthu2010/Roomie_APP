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
 * Kết quả tương thích giữa tôi và CHỦ PHÒNG (so câu trả lời cá nhân của hai người).
 * Extra bắt buộc: EXTRA_POST_ID.
 */
public class KetQuaTuongThichActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";

    public static Intent intent(Context context, String postId) {
        return new Intent(context, KetQuaTuongThichActivity.class).putExtra(EXTRA_POST_ID, postId);
    }

    private String postId;
    private TextView tvPercent;
    private TextView tvLevel;
    private TextView tvSubject;
    private TextView tvTagline;
    private LinearLayout layoutGroups;
    private TextView tvHighlightsTitle;
    private LinearLayout layoutHighlights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ket_qua_tuong_thich);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvPercent = findViewById(R.id.tv_match_percent);
        tvLevel = findViewById(R.id.tv_match_level);
        tvSubject = findViewById(R.id.tv_match_subject);
        tvTagline = findViewById(R.id.tv_match_tagline);
        layoutGroups = findViewById(R.id.layout_group_rows);
        tvHighlightsTitle = findViewById(R.id.tv_highlights_title);
        layoutHighlights = findViewById(R.id.layout_highlights);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) tvTagline.setText(R.string.match_missing_post);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại khi quay về, VD sau khi vừa làm bài kiểm tra
        if (postId != null) load();
    }

    private void load() {
        tvTagline.setOnClickListener(null);
        ApiClient.create(this, DiscoveryApi.class).getCompatibility(postId)
                .enqueue(new ApiCallback<MatchingModels.Compatibility>(this) {
                    @Override
                    public void onSuccess(MatchingModels.Compatibility data) {
                        if (data != null) render(data);
                    }

                    @Override
                    public void onError(String message) {
                        tvLevel.setText(R.string.match_no_data);
                        tvTagline.setText(getString(R.string.match_load_error, message));
                        tvTagline.setOnClickListener(v -> load());
                    }
                });
    }

    private void render(MatchingModels.Compatibility data) {
        String ownerName = data.owner != null ? data.owner.displayName : null;
        tvSubject.setText(ownerName != null
                ? getString(R.string.match_subject_host, ownerName)
                : getString(R.string.match_subject_host_default));

        MatchingModels.Comparison host = data.host;
        MatchUi.bindScore(tvPercent, tvLevel, host);

        if (data.isOwnPost) {
            tvTagline.setText(R.string.match_own_post);
        } else if (data.seekerAnsweredCount == 0) {
            tvTagline.setText(R.string.match_need_quiz);
            tvTagline.setOnClickListener(v -> startActivity(new Intent(this, QuizIntroActivity.class)));
        } else {
            tvTagline.setText(host.tagline);
        }

        MatchUi.fillGroups(LayoutInflater.from(this), layoutGroups, host.groups);

        layoutHighlights.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (host.highlights.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.match_no_highlights);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setPadding(0, 8, 0, 0);
            layoutHighlights.addView(empty);
        }
        for (String text : host.highlights) {
            View chip = inflater.inflate(R.layout.item_match_highlight, layoutHighlights, false);
            ((TextView) chip.findViewById(R.id.tv_highlight)).setText(text);
            layoutHighlights.addView(chip);
        }
        tvHighlightsTitle.setVisibility(View.VISIBLE);
    }
}
