package com.example.roomie.data.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO cho bản đồ giá phòng và vị trí phòng. */
public final class MapModels {

    private MapModels() {}

    /** GET /room-posts/map */
    public static class MapRooms {
        public List<SearchModels.RoomCard> items = new ArrayList<>();
        public int total;
        /** true khi vùng có nhiều hơn số phòng trả về: cần phóng to */
        public boolean truncated;
    }

    /** GET /room-posts/{id}/nearby */
    public static class Nearby {
        public String postId;
        public String title;
        public String addressText;
        public String areaText;
        /** Tọa độ gần đúng (lệch ~100m) để bảo vệ quyền riêng tư */
        public Double latitude;
        public Double longitude;
        public List<Place> places = new ArrayList<>();
    }

    public static class Place {
        public int id;
        /** university | supermarket | bus_stop | hospital | workplace | other */
        public String type;
        public String name;
        public String fullName;
        public String subtitle;
        public double latitude;
        public double longitude;
        public double distanceKm;
    }
}
