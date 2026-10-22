package com.example.roomie.ui.quiz;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.example.roomie.ui.auth.MainActivity;

import java.util.Locale;

public class HabitQuizActivity extends AppCompatActivity {

    private int currentQuestion = 0;
    private final int[] selectedOptions = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};

    private static class QuestionModel {
        String title;
        int categoryIconRes;
        String[] options;

        QuestionModel(String title, int categoryIconRes, String[] options) {
            this.title = title;
            this.categoryIconRes = categoryIconRes;
            this.options = options;
        }
    }

    private QuestionModel[] questions;

    private TextView tvQuestionNumber;
    private ProgressBar pbQuizProgress;
    private ImageView imgCategoryIcon;
    private TextView tvQuestionTitle;

    private RelativeLayout cardOpt1, cardOpt2, cardOpt3;
    private TextView tvOpt1Text, tvOpt2Text, tvOpt3Text;
    private ImageView imgOpt1Radio, imgOpt2Radio, imgOpt3Radio;

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

        initQuestions();

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnClose = findViewById(R.id.btn_close);

        tvQuestionNumber = findViewById(R.id.tv_question_number);
        pbQuizProgress = findViewById(R.id.pb_quiz_progress);
        imgCategoryIcon = findViewById(R.id.img_category_icon);
        tvQuestionTitle = findViewById(R.id.tv_question_title);

        cardOpt1 = findViewById(R.id.card_option_1);
        cardOpt2 = findViewById(R.id.card_option_2);
        cardOpt3 = findViewById(R.id.card_option_3);

        tvOpt1Text = findViewById(R.id.tv_opt1_text);
        tvOpt2Text = findViewById(R.id.tv_opt2_text);
        tvOpt3Text = findViewById(R.id.tv_opt3_text);

        imgOpt1Radio = findViewById(R.id.img_opt1_radio);
        imgOpt2Radio = findViewById(R.id.img_opt2_radio);
        imgOpt3Radio = findViewById(R.id.img_opt3_radio);

        AppCompatButton btnQuizPrev = findViewById(R.id.btn_quiz_prev);
        btnQuizNext = findViewById(R.id.btn_quiz_next);

        Runnable exitQuiz = () -> {
            Intent intent = new Intent(HabitQuizActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnClose != null) btnClose.setOnClickListener(v -> exitQuiz.run());

        if (cardOpt1 != null) cardOpt1.setOnClickListener(v -> selectOption(0));
        if (cardOpt2 != null) cardOpt2.setOnClickListener(v -> selectOption(1));
        if (cardOpt3 != null) cardOpt3.setOnClickListener(v -> selectOption(2));

        if (btnQuizPrev != null) {
            btnQuizPrev.setOnClickListener(v -> {
                if (currentQuestion > 0) {
                    currentQuestion--;
                    renderQuestion();
                } else {
                    finish();
                }
            });
        }

        if (btnQuizNext != null) {
            btnQuizNext.setOnClickListener(v -> {
                if (selectedOptions[currentQuestion] == -1) {
                    selectedOptions[currentQuestion] = 0;
                }

                if (currentQuestion < questions.length - 1) {
                    currentQuestion++;
                    renderQuestion();
                } else {
                    AppNotificationHelper.showSuccess(HabitQuizActivity.this, "Hoàn thành bài kiểm tra thói quen!");
                    Intent intent = new Intent(HabitQuizActivity.this, QuizSuccessActivity.class);
                    intent.putExtra("USER_NAME", "Linh");
                    startActivity(intent);
                    finish();
                }
            });
        }

        renderQuestion();
    }

    private void initQuestions() {
        questions = new QuestionModel[]{
                new QuestionModel(
                        "Bạn thường đi ngủ lúc mấy giờ?",
                        R.drawable.ic_moon,
                        new String[]{"Trước 23:00", "Từ 23:00 đến 00:00", "Sau 00:00, mình là cú đêm"}
                ),
                new QuestionModel(
                        "Bạn thích không gian sạch sẽ đến mức nào?",
                        R.drawable.ic_sparkles,
                        new String[]{"Gọn gàng, dọn dẹp mỗi ngày", "Dọn vài lần mỗi tuần", "Thoải mái, bừa một chút cũng ổn"}
                ),
                new QuestionModel(
                        "Bạn có hút thuốc không?",
                        R.drawable.ic_no_smoking,
                        new String[]{"Không hút, muốn phòng không khói", "Không hút, chấp nhận hút bên ngoài", "Có hút, chỉ hút ở nơi cho phép"}
                ),
                new QuestionModel(
                        "Tần suất nấu ăn ở nhà của bạn?",
                        R.drawable.ic_sparkles,
                        new String[]{"Nấu ăn mỗi ngày, thích tự nấu", "Nấu vài lần tuần khi có thời gian", "Hầu như đặt đồ ăn ngoài"}
                ),
                new QuestionModel(
                        "Mức độ dẫn bạn bè về phòng chơi?",
                        R.drawable.ic_people,
                        new String[]{"Thoải mái, thích không gian nhộn nhịp", "Cần báo trước cho bạn cùng phòng", "Hạn chế, thích không gian riêng tư"}
                ),
                new QuestionModel(
                        "Góc nhìn của bạn về nuôi thú cưng?",
                        R.drawable.ic_sparkles,
                        new String[]{"Rất yêu thích và đang nuôi thú cưng", "Đồng ý nuôi nếu sạch sẽ và không ồn", "Không muốn ở cùng thú cưng"}
                ),
                new QuestionModel(
                        "Mức độ nhạy cảm với tiếng ồn của bạn?",
                        R.drawable.ic_sparkles,
                        new String[]{"Cần không gian yên tĩnh tuyệt đối", "Bình thường, chấp nhận tiếng ồn vừa phải", "Dễ tính, không ảnh hưởng bởi tiếng ồn"}
                ),
                new QuestionModel(
                        "Phong cách giao tiếp bạn hướng tới?",
                        R.drawable.ic_people,
                        new String[]{"Thích trò chuyện, chia sẻ như bạn thân", "Thân thiện nhưng giữ không gian riêng", "Tôn trọng sự riêng tư, ít giao tiếp"}
                )
        };
    }

    private void renderQuestion() {
        QuestionModel q = questions[currentQuestion];

        if (selectedOptions[currentQuestion] == -1) {
            selectedOptions[currentQuestion] = (currentQuestion == 0) ? 1 : 0;
        }

        if (tvQuestionNumber != null) {
            tvQuestionNumber.setText(String.format(Locale.getDefault(), "Câu %d / %d", currentQuestion + 1, questions.length));
        }

        if (pbQuizProgress != null) {
            pbQuizProgress.setProgress(currentQuestion + 1);
        }

        if (imgCategoryIcon != null) {
            imgCategoryIcon.setImageResource(q.categoryIconRes);
        }

        if (tvQuestionTitle != null) {
            tvQuestionTitle.setText(q.title);
        }

        if (tvOpt1Text != null) tvOpt1Text.setText(q.options[0]);
        if (tvOpt2Text != null) tvOpt2Text.setText(q.options[1]);
        if (tvOpt3Text != null) tvOpt3Text.setText(q.options[2]);

        if (btnQuizNext != null) {
            if (currentQuestion == questions.length - 1) {
                btnQuizNext.setText(R.string.btn_finish);
            } else {
                btnQuizNext.setText(R.string.btn_next);
            }
        }

        updateOptionStyles();
    }

    private void selectOption(int index) {
        selectedOptions[currentQuestion] = index;
        updateOptionStyles();
    }

    private void updateOptionStyles() {
        int selected = selectedOptions[currentQuestion];

        setCardStyle(cardOpt1, tvOpt1Text, imgOpt1Radio, selected == 0);
        setCardStyle(cardOpt2, tvOpt2Text, imgOpt2Radio, selected == 1);
        setCardStyle(cardOpt3, tvOpt3Text, imgOpt3Radio, selected == 2);
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
}