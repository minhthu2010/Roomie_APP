package com.example.roomie.data.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO cho API bộ câu hỏi; tên field trùng với JSON camelCase của backend. */
public final class QuizModels {

    private QuizModels() {}

    public static class Questionnaire {
        public long versionId;
        public String versionKey;
        public String mode;
        public int totalQuestions;
        public List<Criterion> criteria = new ArrayList<>();
        public List<Question> questions = new ArrayList<>();
    }

    public static class Criterion {
        public String key;
        public String name;
        public String description;
    }

    public static class Question {
        public long questionId;
        public String questionKey;
        public String criterionKey;
        public String text;
        public String description;
        public String answerType;
        public boolean isRequired;
        public int displayOrder;
        public List<Option> options = new ArrayList<>();
    }

    public static class Option {
        public String optionKey;
        public String label;
        public int score;
    }

    /** Dùng cho cả bài test của tôi (/me/lifestyle-test) và của phòng. */
    public static class LifestyleTest {
        public long versionId;
        public int totalQuestions;
        public int answeredCount;
        public int currentStep;
        public boolean isStarted;
        public boolean isCompleted;
        public String completedAt;
        public String updatedAt;
        public List<Answer> answers = new ArrayList<>();
    }

    public static class Answer {
        public long questionId;
        public String questionKey;
        public String optionKey;

        public Answer(long questionId, String optionKey) {
            this.questionId = questionId;
            this.optionKey = optionKey;
        }
    }

    public static class SaveAnswersRequest {
        public List<Answer> answers;
        public Integer currentStep;
        public Boolean completed;

        public SaveAnswersRequest(List<Answer> answers, Integer currentStep, Boolean completed) {
            this.answers = answers;
            this.currentStep = currentStep;
            this.completed = completed;
        }
    }
}
