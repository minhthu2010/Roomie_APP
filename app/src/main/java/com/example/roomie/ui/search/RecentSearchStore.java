package com.example.roomie.ui.search;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** "Tìm kiếm gần đây" lưu trên máy (SharedPreferences), tối đa MAX mục, mới nhất trước. */
public class RecentSearchStore {

    private static final String PREFS = "roomie_recent_search";
    private static final String KEY = "items";
    private static final int MAX = 5;

    public static class Entry {
        public String title;
        public String subtitle;
        public String query;
        public String area;
        public Integer landmarkId;
        /** Địa chỉ đã tra: tên + tọa độ */
        public String placeName;
        public String wardCode;
        public Double lat;
        public Double lng;

        boolean sameTarget(Entry o) {
            return Objects.equals(query, o.query) && Objects.equals(area, o.area)
                    && Objects.equals(landmarkId, o.landmarkId) && Objects.equals(wardCode, o.wardCode)
                    && Objects.equals(placeName, o.placeName) && Objects.equals(lat, o.lat);
        }

        /** Áp mục này vào điều kiện tìm kiếm (giữ nguyên bộ lọc) */
        public void applyTo(SearchCriteria c) {
            c.clearLocation();
            c.query = query;
            c.area = area;
            c.landmarkId = landmarkId;
            c.landmarkName = landmarkId != null ? title : null;
            c.placeName = placeName;
            c.wardCode = wardCode;
            c.wardName = wardCode != null ? title : null;
            c.centerLat = lat;
            c.centerLng = lng;
            if (c.hasDistanceOrigin()) c.sort = SearchCriteria.SORT_DISTANCE;
        }
    }

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public RecentSearchStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Entry> list() {
        String json = prefs.getString(KEY, null);
        if (json == null) return new ArrayList<>();
        try {
            List<Entry> items = gson.fromJson(json, new TypeToken<List<Entry>>() {}.getType());
            return items != null ? items : new ArrayList<>();
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    public void add(Entry entry) {
        List<Entry> items = list();
        items.removeIf(entry::sameTarget);
        items.add(0, entry);
        while (items.size() > MAX) items.remove(items.size() - 1);
        prefs.edit().putString(KEY, gson.toJson(items)).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY).apply();
    }
}
