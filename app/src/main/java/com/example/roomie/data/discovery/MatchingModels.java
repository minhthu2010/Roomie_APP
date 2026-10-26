package com.example.roomie.data.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO cho GET /room-posts/{postId}/compatibility */
public final class MatchingModels {

    private MatchingModels() {}

    public static final String STATUS_MATCH = "match";
    public static final String STATUS_CLOSE = "close";
    public static final String STATUS_DISCUSS = "discuss";

    public static class Compatibility {
        public String postId;
        public PostInfo post;
        public OwnerInfo owner;
        public boolean isOwnPost;
        public int totalQuestions;
        public int seekerAnsweredCount;
        /** Người tìm so với bài test của phòng (bước 6 đăng bài) */
        public Comparison room;
        /** Người tìm so với câu trả lời cá nhân của chủ phòng */
        public Comparison host;
    }

    public static class PostInfo {
        public String title;
        public String addressText;
        public String coverImageUrl;
    }

    public static class OwnerInfo {
        public String userId;
        public String displayName;
        public String avatarUrl;
    }

    public static class Comparison {
        /** null khi chưa có câu nào cả hai bên cùng trả lời */
        public Integer overall;
        public String level;
        public String tagline;
        public int answeredBoth;
        public List<Group> groups = new ArrayList<>();
        public List<Criterion> criteria = new ArrayList<>();
        public List<String> highlights = new ArrayList<>();
    }

    public static class Group {
        public String key;
        public String name;
        public Integer percent;
    }

    public static class Criterion {
        public String questionKey;
        public String criterionKey;
        public String text;
        /** match | close | discuss | unknown */
        public String status;
        public String seekerAnswer;
        public String otherAnswer;
    }
}
