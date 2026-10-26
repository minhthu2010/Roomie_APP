package com.example.roomie.ui.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Điều kiện tìm phòng, truyền giữa Home, Tìm kiếm và Bộ lọc (Intent extra EXTRA).
 * Khớp với tham số của GET /room-posts/search.
 */
public class SearchCriteria implements Serializable {

    public static final String EXTRA = "SEARCH_CRITERIA";

    public static final String SORT_MATCH = "match";
    public static final String SORT_NEWEST = "newest";
    public static final String SORT_PRICE_ASC = "price_asc";
    public static final String SORT_PRICE_DESC = "price_desc";
    public static final String SORT_DISTANCE = "distance";

    // Ô tìm kiếm: chỉ dùng 1 trong 3 (từ khóa / khu vực / gần trường)
    public String query;
    public String area;
    public Integer landmarkId;
    public String landmarkName;

    // Bộ lọc
    public Long minRent;
    public Long maxRent;
    public ArrayList<String> roomTypes = new ArrayList<>();
    /** female | male | null (không quan trọng) */
    public String gender;
    public ArrayList<String> amenities = new ArrayList<>();

    public String sort = SORT_MATCH;

    public SearchCriteria copy() {
        SearchCriteria c = new SearchCriteria();
        c.query = query;
        c.area = area;
        c.landmarkId = landmarkId;
        c.landmarkName = landmarkName;
        c.minRent = minRent;
        c.maxRent = maxRent;
        c.roomTypes = new ArrayList<>(roomTypes);
        c.gender = gender;
        c.amenities = new ArrayList<>(amenities);
        c.sort = sort;
        return c;
    }

    public void clearLocation() {
        query = null;
        area = null;
        landmarkId = null;
        landmarkName = null;
        if (SORT_DISTANCE.equals(sort)) sort = SORT_MATCH;
    }

    public void clearFilters() {
        minRent = null;
        maxRent = null;
        roomTypes.clear();
        gender = null;
        amenities.clear();
    }

    public boolean hasLocation() {
        return query != null || area != null || landmarkId != null;
    }

    public boolean hasFilters() {
        return minRent != null || maxRent != null || !roomTypes.isEmpty() || gender != null || !amenities.isEmpty();
    }

    public Map<String, String> toQuery(int page, int limit) {
        Map<String, String> m = new HashMap<>();
        if (query != null) m.put("q", query);
        if (area != null) m.put("area", area);
        if (landmarkId != null) m.put("landmarkId", String.valueOf(landmarkId));
        if (minRent != null) m.put("minRent", String.valueOf(minRent));
        if (maxRent != null) m.put("maxRent", String.valueOf(maxRent));
        if (!roomTypes.isEmpty()) m.put("roomTypes", String.join(",", roomTypes));
        if (gender != null) m.put("gender", gender);
        if (!amenities.isEmpty()) m.put("amenities", String.join(",", amenities));
        m.put("sort", sort);
        m.put("page", String.valueOf(page));
        m.put("limit", String.valueOf(limit));
        return m;
    }
}
