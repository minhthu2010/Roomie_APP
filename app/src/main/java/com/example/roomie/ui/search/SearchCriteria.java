package com.example.roomie.ui.search;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Điều kiện tìm phòng, truyền giữa Home, Tìm kiếm, Bộ lọc và Bản đồ (Intent extra EXTRA).
 * Khớp với tham số của GET /room-posts/search.
 */
public class SearchCriteria implements Serializable {

    public static final String EXTRA = "SEARCH_CRITERIA";

    public static final String SORT_MATCH = "match";
    public static final String SORT_NEWEST = "newest";
    public static final String SORT_PRICE_ASC = "price_asc";
    public static final String SORT_PRICE_DESC = "price_desc";
    public static final String SORT_DISTANCE = "distance";

    // Vị trí tìm: chỉ dùng 1 trong 5 cách
    /** Từ khóa tự do */
    public String query;
    /** Tên quận cũ ("Quận 10") khớp cột district_legacy */
    public String area;
    /** Gần trường */
    public Integer landmarkId;
    public String landmarkName;
    /** Quanh một địa chỉ đã tra (số nhà, đường, địa danh) */
    public String placeName;
    /** Đúng phường/xã (mã hành chính thật) */
    public String wardCode;
    public String wardName;
    /** Tâm để bản đồ nhảy tới: tọa độ trường / địa chỉ / phường (null nếu chưa biết) */
    public Double centerLat;
    public Double centerLng;
    /** placeName là vị trí GPS hiện tại ("Phòng gần tôi"), không lưu vào tìm kiếm gần đây */
    public boolean nearMe;
    /** Bán kính quanh trường / địa chỉ / vị trí của bạn (km); null = mặc định của server (3km) */
    public Integer radiusKm;

    // Bộ lọc
    public Long minRent;
    public Long maxRent;
    public ArrayList<String> roomTypes = new ArrayList<>();
    /** female | male | null (không quan trọng) */
    public String gender;
    public ArrayList<String> amenities = new ArrayList<>();
    /** Chuyển vào trong vòng N ngày tới (0 = ở được ngay); null = bất kỳ */
    public Integer moveInDays;
    public Integer minArea;
    public Long maxDeposit;
    /** Số người cần ở cùng lúc (đi cùng bạn bè) */
    public Integer minSlots;
    /** student | worker | null */
    public String occupation;

    public String sort = SORT_MATCH;

    public SearchCriteria copy() {
        SearchCriteria c = new SearchCriteria();
        c.query = query;
        c.area = area;
        c.landmarkId = landmarkId;
        c.landmarkName = landmarkName;
        c.placeName = placeName;
        c.wardCode = wardCode;
        c.wardName = wardName;
        c.centerLat = centerLat;
        c.centerLng = centerLng;
        c.nearMe = nearMe;
        c.radiusKm = radiusKm;
        c.minRent = minRent;
        c.maxRent = maxRent;
        c.roomTypes = new ArrayList<>(roomTypes);
        c.gender = gender;
        c.amenities = new ArrayList<>(amenities);
        c.moveInDays = moveInDays;
        c.minArea = minArea;
        c.maxDeposit = maxDeposit;
        c.minSlots = minSlots;
        c.occupation = occupation;
        c.sort = sort;
        return c;
    }

    public void clearLocation() {
        query = null;
        area = null;
        landmarkId = null;
        landmarkName = null;
        placeName = null;
        wardCode = null;
        wardName = null;
        centerLat = null;
        centerLng = null;
        nearMe = false;
        if (SORT_DISTANCE.equals(sort)) sort = SORT_MATCH;
    }

    public void clearFilters() {
        minRent = null;
        maxRent = null;
        roomTypes.clear();
        gender = null;
        amenities.clear();
        moveInDays = null;
        minArea = null;
        maxDeposit = null;
        minSlots = null;
        occupation = null;
        radiusKm = null;
    }

    public boolean hasLocation() {
        return query != null || area != null || landmarkId != null || placeName != null || wardCode != null;
    }

    public boolean hasFilters() {
        return minRent != null || maxRent != null || !roomTypes.isEmpty() || gender != null || !amenities.isEmpty()
                || moveInDays != null || minArea != null || maxDeposit != null || minSlots != null || occupation != null;
    }

    /** Có điểm gốc để tính khoảng cách (gần trường / quanh địa chỉ) thì mới sắp theo khoảng cách được */
    public boolean hasDistanceOrigin() {
        return landmarkId != null || (placeName != null && centerLat != null);
    }

    public boolean hasCenter() {
        return centerLat != null && centerLng != null;
    }

    /** Chữ hiện trên ô tìm kiếm */
    public String locationLabel() {
        if (landmarkName != null) return landmarkName;
        if (placeName != null) return placeName;
        if (wardName != null) return wardName;
        if (area != null) return area;
        return query;
    }

    public Map<String, String> toQuery(int page, int limit) {
        Map<String, String> m = new HashMap<>();
        if (query != null) m.put("q", query);
        if (area != null) m.put("area", area);
        if (landmarkId != null) m.put("landmarkId", String.valueOf(landmarkId));
        // Gần trường cũng gửi kèm tọa độ: trường trong lịch sử tìm kiếm có thể đã bị đổi mã khi nạp lại dữ liệu
        if ((placeName != null || landmarkId != null) && hasCenter()) {
            m.put("nearLat", String.valueOf(centerLat));
            m.put("nearLng", String.valueOf(centerLng));
        }
        if (radiusKm != null && hasDistanceOrigin()) m.put("radiusKm", String.valueOf(radiusKm));
        if (wardCode != null) m.put("wardCode", wardCode);
        if (minRent != null) m.put("minRent", String.valueOf(minRent));
        if (maxRent != null) m.put("maxRent", String.valueOf(maxRent));
        if (!roomTypes.isEmpty()) m.put("roomTypes", String.join(",", roomTypes));
        if (gender != null) m.put("gender", gender);
        if (!amenities.isEmpty()) m.put("amenities", String.join(",", amenities));
        if (moveInDays != null) {
            // Ngày theo giờ máy, gửi dạng YYYY-MM-DD
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_YEAR, moveInDays);
            m.put("moveInBy", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime()));
        }
        if (minArea != null) m.put("minArea", String.valueOf(minArea));
        if (maxDeposit != null) m.put("maxDeposit", String.valueOf(maxDeposit));
        if (minSlots != null) m.put("minSlots", String.valueOf(minSlots));
        if (occupation != null) m.put("occupation", occupation);
        m.put("sort", sort);
        m.put("page", String.valueOf(page));
        m.put("limit", String.valueOf(limit));
        return m;
    }
}
