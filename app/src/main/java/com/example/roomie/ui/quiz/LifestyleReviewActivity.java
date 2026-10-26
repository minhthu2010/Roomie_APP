package com.example.roomie.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.QuizModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Thói quen sống của tôi": xem lại câu trả lời bài test theo 4 nhóm.
 * Chạm một câu để sửa riêng câu đó (mở HabitQuizActivity ở chế độ sửa 1 câu).
 */
public class LifestyleReviewActivity extends AppCompatActivity {

    private DiscoveryApi api;

    private View cardStatus;
    private TextView tvStatusTitle;
    private TextView tvStatusSub;
    private ProgressBar pbAnswered;
    private TextView tvEditHint;
    private LinearLayout layoutGroups;
    private TextView tvStateMessage;
    private AppCompatButton btnRetakeAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lifestyle_review);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);

        TextView tvHeaderTitle = findViewById(R.id.tv_header_title);
        if (tvHeaderTitle != null) tvHeaderTitle.setText(R.string.lifestyle_review_title);
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        cardStatus = findViewById(R.id.card_status);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusSub = findViewById(R.id.tv_status_sub);
        pbAnswered = findViewById(R.id.pb_answered);
        tvEditHint = findViewById(R.id.tv_edit_hint);
        layoutGroups = findViewById(R.id.layout_groups);
        tvStateMessage = findViewById(R.id.tv_state_message);
        btnRetakeAll = findViewById(R.id.btn_retake_all);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại mỗi lần quay về màn này để thấy câu vừa sửa.
        load();
    }

    private void load() {
        if (layoutGroups.getChildCount() == 0) showMessage(getString(R.string.lifestyle_loading), false);
        api.getQuestionnaire(DiscoveryApi.MODE_PERSONAL).enqueue(new ApiCallback<QuizModels.Questionnaire>(this) {
            @Override
            public void onSuccess(QuizModels.Questionnaire questionnaire) {
                api.getMyLifestyleTest().enqueue(new ApiCallback<QuizModels.LifestyleTest>(LifestyleReviewActivity.this) {
                    @Override
                    public void onSuccess(QuizModels.LifestyleTest test) {
                        render(questionnaire, test);
                    }

                    @Override
                    public void onError(String message) {
                        showMessage(getString(R.string.lifestyle_load_error, message), true);
                    }
                });
            }

            @Override
            public void onError(String message) {
                showMessage(getString(R.string.lifestyle_load_error, message), true);
            }
        });
    }

    private void showMessage(String message, boolean tapToRetry) {
        cardStatus.setVisibility(View.GONE);
        tvEditHint.setVisibility(View.GONE);
        layoutGroups.removeAllViews();
        btnRetakeAll.setVisibility(View.GONE);
        tvStateMessage.setVisibility(View.VISIBLE);
        tvStateMessage.setText(message);
        tvStateMessage.setOnClickListener(tapToRetry ? v -> load() : null);
    }

    private void render(QuizModels.Questionnaire questionnaire, QuizModels.LifestyleTest test) {
        if (questionnaire == null || test == null) return;
        int total = questionnaire.questions.size();

        // Chưa làm câu nào: hiện lời mời làm bài
        if (test.answeredCount == 0) {
            showMessage(getString(R.string.lifestyle_empty), false);
            btnRetakeAll.setVisibility(View.VISIBLE);
            btnRetakeAll.setText(R.string.lifestyle_start_quiz);
            btnRetakeAll.setOnClickListener(v -> startActivity(new Intent(this, QuizIntroActivity.class)));
            return;
        }

        tvStateMessage.setVisibility(View.GONE);
        cardStatus.setVisibility(View.VISIBLE);
        tvEditHint.setVisibility(View.VISIBLE);

        tvStatusTitle.setText(test.isCompleted
                ? R.string.lifestyle_status_completed
                : R.string.lifestyle_status_in_progress);
        String date = QuizUi.formatDate(test.updatedAt != null ? test.updatedAt : test.completedAt);
        tvStatusSub.setText(date != null
                ? getString(R.string.lifestyle_status_sub, date, test.answeredCount, total)
                : getString(R.string.lifestyle_status_sub_no_date, test.answeredCount, total));
        pbAnswered.setProgress(total == 0 ? 0 : test.answeredCount * 100 / total);

        btnRetakeAll.setVisibility(View.VISIBLE);
        btnRetakeAll.setText(test.isCompleted ? R.string.lifestyle_retake_all : R.string.lifestyle_continue_quiz);
        btnRetakeAll.setOnClickListener(v -> startActivity(new Intent(this, HabitQuizActivity.class)));

        renderGroups(questionnaire, test);
    }

    private void renderGroups(QuizModels.Questionnaire questionnaire, QuizModels.LifestyleTest test) {
        Map<Long, String> chosen = new HashMap<>();
        for (QuizModels.Answer a : test.answers) chosen.put(a.questionId, a.optionKey);

        // Nhóm xếp theo thứ tự câu hỏi (giống thứ tự trong bài test); câu không thuộc nhóm nào cho vào "Khác".
        Map<String, String> groupNames = new HashMap<>();
        for (QuizModels.Criterion c : questionnaire.criteria) groupNames.put(c.key, c.name);
        Map<String, List<QuizModels.Question>> byGroup = new LinkedHashMap<>();
        for (QuizModels.Question q : questionnaire.questions) {
            String key = groupNames.containsKey(q.criterionKey) ? q.criterionKey : "";
            if (!byGroup.containsKey(key)) byGroup.put(key, new ArrayList<>());
            byGroup.get(key).add(q);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        layoutGroups.removeAllViews();
        for (Map.Entry<String, List<QuizModels.Question>> entry : byGroup.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            View group = inflater.inflate(R.layout.item_lifestyle_group, layoutGroups, false);
            String name = groupNames.get(entry.getKey());
            ((TextView) group.findViewById(R.id.tv_group_name))
                    .setText(name != null ? name : getString(R.string.lifestyle_group_other));

            LinearLayout answers = group.findViewById(R.id.layout_answers);
            for (QuizModels.Question q : entry.getValue()) {
                answers.addView(buildAnswerRow(inflater, answers, q, chosen.get(q.questionId)));
            }
            layoutGroups.addView(group);
        }
    }

    private View buildAnswerRow(LayoutInflater inflater, LinearLayout parent,
                                QuizModels.Question q, String optionKey) {
        View row = inflater.inflate(R.layout.item_lifestyle_answer, parent, false);
        ((ImageView) row.findViewById(R.id.img_question_icon)).setImageResource(QuizUi.iconFor(q.questionKey));
        ((TextView) row.findViewById(R.id.tv_question)).setText(q.text);

        TextView tvAnswer = row.findViewById(R.id.tv_answer);
        String label = null;
        for (QuizModels.Option o : q.options) {
            if (o.optionKey.equals(optionKey)) label = o.label;
        }
        if (label != null) {
            tvAnswer.setText(label);
        } else {
            tvAnswer.setText(R.string.lifestyle_not_answered);
            tvAnswer.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitQuizActivity.class);
            intent.putExtra(HabitQuizActivity.EXTRA_START_QUESTION_ID, q.questionId);
            intent.putExtra(HabitQuizActivity.EXTRA_SINGLE_QUESTION, true);
            startActivity(intent);
        });
        return row;
    }
}
