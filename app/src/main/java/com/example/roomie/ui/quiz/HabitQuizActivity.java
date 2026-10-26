package com.example.roomie.ui.quiz;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.QuizModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.auth.MainActivity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bài kiểm tra thói quen. Câu hỏi lấy từ API, mỗi lần bấm "Tiếp" lưu câu trả lời lên server.
 *
 * Extra:
 *  - EXTRA_MODE: DiscoveryApi.MODE_PERSONAL (mặc định) hoặc MODE_ROOM (bước 6 đăng bài)
 *  - EXTRA_POST_ID: bắt buộc khi MODE_ROOM. Xong bài test phòng sẽ setResult(RESULT_OK) rồi đóng.
 *  - EXTRA_START_QUESTION_ID + EXTRA_SINGLE_QUESTION: mở thẳng một câu để sửa riêng câu đó
 *    (dùng ở LifestyleReviewActivity); lưu xong thì đóng màn hình.
 */
public class HabitQuizActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "MODE";
    public static final String EXTRA_POST_ID = "POST_ID";
    public static final String EXTRA_START_QUESTION_ID = "START_QUESTION_ID";
    public static final String EXTRA_SINGLE_QUESTION = "SINGLE_QUESTION";

    private DiscoveryApi api;
    private String mode;
    private String postId;
    private long startQuestionId;
    private boolean singleQuestion;
    // Bắt đầu tính giờ khi câu hỏi hiện ra, không tính thời gian chờ tải.
    private long startedAtMs = 0;

    private List<QuizModels.Question> questions = Collections.emptyList();
    // questionId -> optionKey đã chọn
    private final Map<Long, String> selected = new HashMap<>();
    private int currentQuestion = 0;
    private boolean saving = false;

    private TextView tvQuestionNumber;
    private TextView tvTimeLeft;
    private ProgressBar pbQuizProgress;
    private ImageView imgCategoryIcon;
    private TextView tvQuestionTitle;

    private RelativeLayout[] optionCards;
    private TextView[] optionTexts;
    private ImageView[] optionRadios;

    private AppCompatButton btnQuizPrev;
    private AppCompatButton btnQuizNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_quiz_scroll), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = DiscoveryApi.MODE_PERSONAL;
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        startQuestionId = getIntent().getLongExtra(EXTRA_START_QUESTION_ID, -1);
        singleQuestion = getIntent().getBooleanExtra(EXTRA_SINGLE_QUESTION, false) && startQuestionId >= 0;

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnClose = findViewById(R.id.btn_close);

        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvTimeLeft = findViewById(R.id.tv_time_left);
        pbQuizProgress = findViewById(R.id.pb_quiz_progress);
        imgCategoryIcon = findViewById(R.id.img_category_icon);
        tvQuestionTitle = findViewById(R.id.tv_question_title);

        optionCards = new RelativeLayout[]{
                findViewById(R.id.card_option_1), findViewById(R.id.card_option_2), findViewById(R.id.card_option_3)};
        optionTexts = new TextView[]{
                findViewById(R.id.tv_opt1_text), findViewById(R.id.tv_opt2_text), findViewById(R.id.tv_opt3_text)};
        optionRadios = new ImageView[]{
                findViewById(R.id.img_opt1_radio), findViewById(R.id.img_opt2_radio), findViewById(R.id.img_opt3_radio)};

        btnQuizPrev = findViewById(R.id.btn_quiz_prev);
        btnQuizNext = findViewById(R.id.btn_quiz_next);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnClose != null) btnClose.setOnClickListener(v -> exitQuiz());

        for (int i = 0; i < optionCards.length; i++) {
            final int index = i;
            if (optionCards[i] != null) optionCards[i].setOnClickListener(v -> selectOption(index));
        }

        if (btnQuizPrev != null) {
            btnQuizPrev.setOnClickListener(v -> {
                if (singleQuestion) {
                    finish();
                } else if (currentQuestion > 0) {
                    currentQuestion--;
                    renderQuestion();
                } else {
                    finish();
                }
            });
        }

        if (btnQuizNext != null) btnQuizNext.setOnClickListener(v -> saveCurrentAndContinue());

        if (DiscoveryApi.MODE_ROOM.equals(mode) && postId == null) {
            AppNotificationHelper.showError(this, "Thiếu mã bài đăng cho bài test của phòng");
            finish();
            return;
        }
        loadQuestions();
    }

    private void exitQuiz() {
        if (DiscoveryApi.MODE_ROOM.equals(mode)) {
            finish();
            return;
        }
        Intent intent = new Intent(HabitQuizActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ---------- Tải dữ liệu ----------

    private void loadQuestions() {
        showLoading("Đang tải câu hỏi...");
        api.getQuestionnaire(mode).enqueue(new ApiCallback<QuizModels.Questionnaire>(this) {
            @Override
            public void onSuccess(QuizModels.Questionnaire data) {
                if (data == null || data.questions.isEmpty()) {
                    showLoadError("Chưa có câu hỏi nào. Chạm để thử lại.");
                    return;
                }
                questions = data.questions;
                if (pbQuizProgress != null) pbQuizProgress.setMax(questions.size());
                loadSavedAnswers();
            }

            @Override
            public void onError(String message) {
                showLoadError(message + "\nChạm để thử lại.");
            }
        });
    }

    // Điền lại câu trả lời đã lưu để làm tiếp hoặc làm lại bài test.
    private void loadSavedAnswers() {
        boolean isRoom = DiscoveryApi.MODE_ROOM.equals(mode);
        (isRoom ? api.getPostAnswers(postId) : api.getMyLifestyleTest())
                .enqueue(new ApiCallback<QuizModels.LifestyleTest>(this) {
                    @Override
                    public void onSuccess(QuizModels.LifestyleTest data) {
                        if (data != null) {
                            for (QuizModels.Answer a : data.answers) selected.put(a.questionId, a.optionKey);
                            // Đang làm dở thì mở lại đúng câu; đã xong thì làm lại từ đầu.
                            if (!isRoom && data.isStarted && !data.isCompleted) {
                                currentQuestion = Math.min(Math.max(data.currentStep - 1, 0), questions.size() - 1);
                            }
                        }
                        showFirstQuestion();
                    }

                    @Override
                    public void onError(String message) {
                        // Không đọc được câu trả lời cũ vẫn làm bài được.
                        showFirstQuestion();
                    }
                });
    }

    private void showFirstQuestion() {
        if (startQuestionId >= 0) {
            for (int i = 0; i < questions.size(); i++) {
                if (questions.get(i).questionId == startQuestionId) currentQuestion = i;
            }
        }
        if (startedAtMs == 0) startedAtMs = SystemClock.elapsedRealtime();
        renderQuestion();
    }

    private void showLoading(String message) {
        setButtonsEnabled(false);
        if (tvQuestionTitle != null) {
            tvQuestionTitle.setText(message);
            tvQuestionTitle.setOnClickListener(null);
        }
        for (RelativeLayout card : optionCards) if (card != null) card.setVisibility(View.INVISIBLE);
    }

    private void showLoadError(String message) {
        AppNotificationHelper.showError(this, message);
        if (tvQuestionTitle != null) {
            tvQuestionTitle.setText(message);
            tvQuestionTitle.setOnClickListener(v -> loadQuestions());
        }
    }

    // ---------- Hiển thị ----------

    private void renderQuestion() {
        QuizModels.Question q = questions.get(currentQuestion);
        int total = questions.size();

        if (tvQuestionNumber != null) {
            tvQuestionNumber.setText(String.format(Locale.getDefault(), "Câu %d / %d", currentQuestion + 1, total));
        }
        if (tvTimeLeft != null) {
            // Khoảng 25 giây mỗi câu; sửa 1 câu thì không cần hiện thời gian
            int minutesLeft = Math.max(1, Math.round((total - currentQuestion) * 0.4f));
            tvTimeLeft.setVisibility(singleQuestion ? View.INVISIBLE : View.VISIBLE);
            tvTimeLeft.setText(getString(R.string.quiz_time_left_format, minutesLeft));
        }
        if (pbQuizProgress != null) pbQuizProgress.setProgress(currentQuestion + 1);
        if (imgCategoryIcon != null) imgCategoryIcon.setImageResource(QuizUi.iconFor(q.questionKey));
        if (tvQuestionTitle != null) {
            tvQuestionTitle.setText(q.text);
            tvQuestionTitle.setOnClickListener(null);
        }

        // Layout có 3 thẻ lựa chọn; câu có ít lựa chọn hơn thì ẩn thẻ thừa.
        for (int i = 0; i < optionCards.length; i++) {
            if (optionCards[i] == null) continue;
            if (i < q.options.size()) {
                optionCards[i].setVisibility(View.VISIBLE);
                if (optionTexts[i] != null) optionTexts[i].setText(q.options.get(i).label);
            } else {
                optionCards[i].setVisibility(View.GONE);
            }
        }

        if (singleQuestion) {
            if (btnQuizNext != null) btnQuizNext.setText(R.string.btn_save_answer);
            if (btnQuizPrev != null) btnQuizPrev.setText(R.string.btn_cancel);
        } else if (btnQuizNext != null) {
            btnQuizNext.setText(currentQuestion == total - 1 ? R.string.btn_finish : R.string.btn_next);
        }
        setButtonsEnabled(true);
        updateOptionStyles();
    }

    private void selectOption(int index) {
        if (saving || questions.isEmpty()) return;
        QuizModels.Question q = questions.get(currentQuestion);
        if (index >= q.options.size()) return;
        selected.put(q.questionId, q.options.get(index).optionKey);
        updateOptionStyles();
    }

    private void updateOptionStyles() {
        QuizModels.Question q = questions.get(currentQuestion);
        String chosen = selected.get(q.questionId);
        for (int i = 0; i < optionCards.length && i < q.options.size(); i++) {
            setCardStyle(optionCards[i], optionTexts[i], optionRadios[i],
                    q.options.get(i).optionKey.equals(chosen));
        }
    }

    private void setCardStyle(RelativeLayout card, TextView tv, ImageView radio, boolean isSelected) {
        if (card == null || tv == null || radio == null) return;

        if (isSelected) {
            card.setBackgroundResource(R.drawable.bg_option_selected);
            tv.setTypeface(null, Typeface.BOLD);
            radio.setImageResource(R.drawable.ic_check_circle);
        } else {
            card.setBackgroundResource(R.drawable.bg_option_unselected);
            tv.setTypeface(null, Typeface.NORMAL);
            radio.setImageResource(R.drawable.ic_radio_circle);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnQuizNext != null) btnQuizNext.setEnabled(enabled);
        if (btnQuizPrev != null) btnQuizPrev.setEnabled(enabled);
    }

    // ---------- Lưu câu trả lời ----------

    private void saveCurrentAndContinue() {
        if (saving || questions.isEmpty()) return;
        QuizModels.Question q = questions.get(currentQuestion);
        String optionKey = selected.get(q.questionId);
        if (optionKey == null) {
            AppNotificationHelper.showInfo(this, "Hãy chọn một đáp án giống bạn nhất");
            return;
        }

        boolean isLast = currentQuestion == questions.size() - 1;
        List<QuizModels.Answer> answers =
                Collections.singletonList(new QuizModels.Answer(q.questionId, optionKey));
        boolean isRoom = DiscoveryApi.MODE_ROOM.equals(mode);
        // Sửa 1 câu: không gửi currentStep/completed để giữ nguyên tiến độ trên server.
        QuizModels.SaveAnswersRequest body = isRoom || singleQuestion
                ? new QuizModels.SaveAnswersRequest(answers, null, null)
                : new QuizModels.SaveAnswersRequest(answers, Math.min(currentQuestion + 2, questions.size()), isLast);

        saving = true;
        setButtonsEnabled(false);
        (isRoom ? api.savePostAnswers(postId, body) : api.saveMyAnswers(body))
                .enqueue(new ApiCallback<QuizModels.LifestyleTest>(this) {
                    @Override
                    public void onSuccess(QuizModels.LifestyleTest data) {
                        saving = false;
                        if (singleQuestion) {
                            setResult(RESULT_OK);
                            finish();
                        } else if (isLast) {
                            onQuizFinished(data);
                        } else {
                            currentQuestion++;
                            renderQuestion();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        saving = false;
                        setButtonsEnabled(true);
                        AppNotificationHelper.showError(HabitQuizActivity.this, message);
                    }
                });
    }

    private void onQuizFinished(QuizModels.LifestyleTest data) {
        if (DiscoveryApi.MODE_ROOM.equals(mode)) {
            if (data != null && !data.isCompleted) {
                AppNotificationHelper.showError(this, "Phòng còn câu hỏi chưa trả lời");
                setButtonsEnabled(true);
                return;
            }
            setResult(RESULT_OK);
            finish();
            return;
        }

        AppNotificationHelper.showSuccess(this, "Hoàn thành bài kiểm tra thói quen!");
        Intent intent = new Intent(HabitQuizActivity.this, QuizSuccessActivity.class);
        intent.putExtra(QuizSuccessActivity.EXTRA_ANSWERED, data != null ? data.answeredCount : questions.size());
        intent.putExtra(QuizSuccessActivity.EXTRA_TOTAL, questions.size());
        intent.putExtra(QuizSuccessActivity.EXTRA_DURATION_SEC,
                (SystemClock.elapsedRealtime() - startedAtMs) / 1000);
        startActivity(intent);
        finish();
    }
}
