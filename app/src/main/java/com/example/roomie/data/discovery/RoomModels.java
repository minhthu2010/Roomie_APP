package com.example.roomie.data.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO cho chi tiết phòng, thư viện ảnh, đã lưu và hồ sơ người đăng. */
public final class RoomModels {

    private RoomModels() {}

    public static class RoomDetail {
        public String id;
        public String title;
        public String description;
        public String roomType;
        public long monthlyRent;
        public Long depositAmount;
        public Double areaM2;
        public String utilityCostNote;
        /** yyyy-MM-dd... */
        public String availableFrom;
        public Integer currentOccupants;
        public int seekingRoommates;
        public String preferredGender;
        public String preferredOccupation;
        public Boolean smokingAllowed;
        public Boolean petsAllowed;
        public String roommateExpectations;
        public String addressText;
        public String districtName;
        public String provinceName;
        public LandmarkRef landmark;
        public Double latitude;
        public Double longitude;
        public String listingStatus;
        public boolean canContact;
        public List<Amenity> amenities = new ArrayList<>();
        public List<Image> images = new ArrayList<>();
        public Integer matchPercent;
        public boolean isSaved;
        public int savedCount;
        public boolean isOwnPost;
        public Owner owner;
        public Contact contact;
    }

    public static class LandmarkRef {
        public int id;
        public String name;
        public String shortName;
    }

    public static class Amenity {
        public String code;
        public String name;
        public String iconKey;
    }

    public static class Image {
        public String id;
        public String url;
        public String caption;
        public boolean isCover;
    }

    public static class Owner {
        public String userId;
        public String displayName;
        public String avatarUrl;
        public String occupation;
        public Integer birthYear;
        public String schoolOrWorkplace;
        public String memberSince;
    }

    public static class Contact {
        /** null khi chưa đăng nhập / chủ bài ẩn liên hệ / bài không còn tìm người */
        public String phone;
        public boolean allowZalo;
        public boolean allowCall;
        /** login | hidden_by_owner | not_available */
        public String hiddenReason;
    }

    public static class RoomImages {
        public String postId;
        public String title;
        public String addressText;
        public String ownerName;
        public List<Image> images = new ArrayList<>();
    }

    /** Phòng đã lưu = RoomCard + trạng thái bài */
    public static class SavedRoom extends SearchModels.RoomCard {
        /** searching | filled | hidden | expired */
        public String listingStatus;
        public boolean canContact;
        public String savedAt;
    }

    public static class SaveResult {
        public String postId;
        public boolean isSaved;
    }

    public static class ContactClick {
        public String channel;

        public ContactClick(String channel) {
            this.channel = channel;
        }
    }

    public static class PublicProfile {
        public String userId;
        public String displayName;
        public String avatarUrl;
        public boolean isMe;
        /** true khi người dùng tắt "Hiện hồ sơ" */
        public boolean isHidden;
        public String bio;
        public String gender;
        public Integer birthYear;
        public String occupation;
        public String schoolOrWorkplace;
        public String memberSince;
        public boolean emailVerified;
        public List<Habit> habits = new ArrayList<>();
        public Integer matchPercent;
        public String matchLevel;
        public List<SearchModels.RoomCard> activePosts = new ArrayList<>();
    }

    public static class Habit {
        public String questionKey;
        public String label;
    }
}
