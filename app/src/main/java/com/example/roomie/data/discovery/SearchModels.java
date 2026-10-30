package com.example.roomie.data.discovery;

/** DTO cho tìm kiếm phòng và gợi ý trường. */
public final class SearchModels {

    private SearchModels() {}

    /** RoomCardDto dùng chung cho Home, Tìm kiếm, Bản đồ, Đã lưu */
    public static class RoomCard {
        public String id;
        public String title;
        public long monthlyRent;
        public String addressText;
        public String coverImageUrl;
        /** null khi chưa làm bài test hoặc phòng chưa có dữ liệu để so */
        public Integer matchPercent;
        public boolean isSaved;
        public String roomType;
        public String preferredGender;
        public String districtName;
        public String landmarkName;
        /** Chỉ có khi tìm gần trường (landmarkId) */
        public Double distanceKm;
        public Double latitude;
        public Double longitude;
    }

    public static class Landmark {
        public int id;
        public String type;
        public String name;
        public String shortName;
        public String addressText;
        public String subtitle;
        public Double latitude;
        public Double longitude;
        public int roomCount;
    }
}
