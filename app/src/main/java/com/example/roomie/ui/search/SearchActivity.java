package com.example.roomie.ui.search;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.PlaceModels;
import com.example.roomie.data.discovery.SearchModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.ApiResponse;

import java.util.List;

import retrofit2.Call;

/**
 * Tìm kiếm phòng: từ khóa (không dấu), khu vực, gần trường học, tìm kiếm gần đây.
 * Nhận và trả SearchCriteria (SearchCriteria.EXTRA) cho màn gọi; bộ lọc giữ nguyên.
 */
public class SearchActivity extends AppCompatActivity {

    private static final long SUGGEST_DELAY_MS = 300;

    private DiscoveryApi api;
    private RecentSearchStore recentStore;
    private SearchCriteria criteria;

    private EditText edtSearch;
    private View layoutRecentHeader;
    private LinearLayout layoutRecent;
    private LinearLayout layoutSchools;
    private LinearLayout layoutPlaces;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSuggest;
    private Call<?> suggestCall;
    private Call<?> placesCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tim_kiem_phong);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);
        recentStore = new RecentSearchStore(this);
        SearchCriteria given = (SearchCriteria) getIntent().getSerializableExtra(SearchCriteria.EXTRA);
        criteria = given != null ? given.copy() : new SearchCriteria();

        edtSearch = findViewById(R.id.edt_search);
        layoutRecentHeader = findViewById(R.id.layout_recent_header);
        layoutRecent = findViewById(R.id.layout_recent);
        layoutSchools = findViewById(R.id.layout_schools);
        layoutPlaces = findViewById(R.id.layout_places);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_search_room).setOnClickListener(v -> searchByText());
        findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            recentStore.clear();
            renderRecent();
        });

        String current = criteria.locationLabel();
        if (current != null) {
            edtSearch.setText(current);
            edtSearch.setSelection(current.length());
        }
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByText();
                return true;
            }
            return false;
        });
        // Gõ đến đâu gợi ý trường đến đó (chờ 300ms để không gọi API mỗi phím)
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
                String q = s.toString().trim();
                pendingSuggest = () -> {
                    loadSchools(q);
                    loadPlaces(q);
                };
                handler.postDelayed(pendingSuggest, SUGGEST_DELAY_MS);
            }
        });

        bindAreaChip(R.id.chip_area_q10);
        bindAreaChip(R.id.chip_area_q3);
        bindAreaChip(R.id.chip_area_binh_thanh);
        bindAreaChip(R.id.chip_area_phu_nhuan);
        loadTopWards();

        renderRecent();
        loadSchools(null);
    }

    @Override
    protected void onDestroy() {
        if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
        if (suggestCall != null) suggestCall.cancel();
        if (placesCall != null) placesCall.cancel();
        super.onDestroy();
    }

    // ---------- Chọn điều kiện ----------

    private void searchByText() {
        String text = edtSearch.getText().toString().trim();
        criteria.clearLocation();
        if (!text.isEmpty()) {
            criteria.query = text;
            RecentSearchStore.Entry e = new RecentSearchStore.Entry();
            e.title = text;
            e.subtitle = getString(R.string.search_recent_query_sub);
            e.query = text;
            recentStore.add(e);
        }
        finishWithCriteria();
    }

    private void bindAreaChip(int chipId) {
        TextView chip = findViewById(chipId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            String area = chip.getText().toString();
            criteria.clearLocation();
            criteria.area = area;
            RecentSearchStore.Entry e = new RecentSearchStore.Entry();
            e.title = area;
            e.subtitle = getString(R.string.search_recent_area_sub);
            e.area = area;
            recentStore.add(e);
            finishWithCriteria();
        });
    }

    private void chooseSchool(SearchModels.Landmark school) {
        String name = school.shortName != null ? school.shortName : school.name;
        criteria.clearLocation();
        criteria.landmarkId = school.id;
        criteria.landmarkName = name;
        criteria.centerLat = school.latitude;
        criteria.centerLng = school.longitude;
        criteria.sort = SearchCriteria.SORT_DISTANCE;
        RecentSearchStore.Entry e = new RecentSearchStore.Entry();
        e.title = name;
        e.subtitle = getString(R.string.search_recent_school_sub);
        e.landmarkId = school.id;
        e.lat = school.latitude;
        e.lng = school.longitude;
        recentStore.add(e);
        finishWithCriteria();
    }

    private void finishWithCriteria() {
        setResult(RESULT_OK, new Intent().putExtra(SearchCriteria.EXTRA, criteria));
        finish();
    }

    /** Phường/xã thật: lọc đúng mã phường; tâm bản đồ tra sau ở màn Bản đồ */
    private void chooseWard(PlaceModels.Ward ward) {
        criteria.clearLocation();
        criteria.wardCode = ward.code;
        criteria.wardName = ward.name;
        RecentSearchStore.Entry e = new RecentSearchStore.Entry();
        e.title = ward.name;
        e.subtitle = getString(R.string.search_recent_ward_sub, ward.provinceName);
        e.wardCode = ward.code;
        recentStore.add(e);
        finishWithCriteria();
    }

    /** Địa chỉ cụ thể: tìm phòng quanh tọa độ đó, sắp theo khoảng cách */
    private void choosePlace(PlaceModels.Address place) {
        criteria.clearLocation();
        criteria.placeName = place.title;
        criteria.centerLat = place.latitude;
        criteria.centerLng = place.longitude;
        criteria.sort = SearchCriteria.SORT_DISTANCE;
        RecentSearchStore.Entry e = new RecentSearchStore.Entry();
        e.title = place.title;
        e.subtitle = getString(R.string.search_recent_place_sub);
        e.placeName = place.title;
        e.lat = place.latitude;
        e.lng = place.longitude;
        recentStore.add(e);
        finishWithCriteria();
    }

    // ---------- Gợi ý phường/xã + địa chỉ khi gõ ----------

    private void loadPlaces(String q) {
        if (placesCall != null) placesCall.cancel();
        if (q == null || q.length() < 2) {
            layoutPlaces.setVisibility(View.GONE);
            return;
        }
        layoutPlaces.setVisibility(View.VISIBLE);
        layoutPlaces.removeAllViews();
        layoutPlaces.addView(sectionNote(getString(R.string.places_searching)));
        Call<ApiResponse<PlaceModels.Suggest>> call = api.suggestPlaces(q);
        placesCall = call;
        call.enqueue(new ApiCallback<PlaceModels.Suggest>(this) {
            @Override
            public void onSuccess(PlaceModels.Suggest data) {
                layoutPlaces.removeAllViews();
                if (data == null || (data.wards.isEmpty() && data.addresses.isEmpty())) {
                    layoutPlaces.addView(sectionNote(getString(R.string.places_empty)));
                    return;
                }
                if (!data.wards.isEmpty()) {
                    layoutPlaces.addView(sectionTitle(getString(R.string.places_wards_title)));
                    for (PlaceModels.Ward w : data.wards) {
                        View row = inflateRow(layoutPlaces, w.name,
                                getString(R.string.places_ward_sub, w.provinceName, w.roomCount), R.drawable.ic_location);
                        row.setOnClickListener(v -> chooseWard(w));
                        layoutPlaces.addView(row);
                    }
                }
                if (!data.addresses.isEmpty()) {
                    layoutPlaces.addView(sectionTitle(getString(R.string.places_addresses_title)));
                    for (PlaceModels.Address a : data.addresses) {
                        View row = inflateRow(layoutPlaces, a.title, a.subtitle, R.drawable.ic_location_on);
                        row.setOnClickListener(v -> choosePlace(a));
                        layoutPlaces.addView(row);
                    }
                    layoutPlaces.addView(sectionNote(getString(R.string.places_source)));
                }
            }

            @Override
            public void onError(String message) {
                layoutPlaces.removeAllViews();
                layoutPlaces.addView(sectionNote(message));
            }
        });
    }

    /** 4 chip "Khu vực được quan tâm" = 4 phường thật có nhiều phòng nhất; lỗi thì giữ chip quận cũ */
    private void loadTopWards() {
        int[] chipIds = {R.id.chip_area_q10, R.id.chip_area_q3, R.id.chip_area_binh_thanh, R.id.chip_area_phu_nhuan};
        api.getWards("79", null, chipIds.length).enqueue(new ApiCallback<List<PlaceModels.Ward>>(this) {
            @Override
            public void onSuccess(List<PlaceModels.Ward> wards) {
                if (wards == null || wards.size() < chipIds.length) return;
                for (int i = 0; i < chipIds.length; i++) {
                    TextView chip = findViewById(chipIds[i]);
                    PlaceModels.Ward w = wards.get(i);
                    chip.setText(w.name.replaceFirst("^(Phường|Xã) ", ""));
                    chip.setOnClickListener(v -> chooseWard(w));
                }
            }

            @Override
            public void onError(String message) {
                // Giữ chip quận mặc định
            }
        });
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(16), 0, 0);
        return tv;
    }

    private TextView sectionNote(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setTextSize(12);
        tv.setPadding(0, dp(8), 0, 0);
        return tv;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- Tìm kiếm gần đây ----------

    private void renderRecent() {
        List<RecentSearchStore.Entry> items = recentStore.list();
        layoutRecent.removeAllViews();
        layoutRecentHeader.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        for (RecentSearchStore.Entry item : items) {
            View row = inflateRow(layoutRecent, item.title, item.subtitle, R.drawable.ic_history);
            row.setOnClickListener(v -> {
                item.applyTo(criteria);
                recentStore.add(item);
                finishWithCriteria();
            });
            layoutRecent.addView(row);
        }
    }

    // ---------- Gợi ý trường học ----------

    private void loadSchools(String q) {
        if (suggestCall != null) suggestCall.cancel();
        Call<ApiResponse<List<SearchModels.Landmark>>> call =
                api.suggestLandmarks(q == null || q.isEmpty() ? null : q, "university", 5);
        suggestCall = call;
        call.enqueue(new ApiCallback<List<SearchModels.Landmark>>(this) {
            @Override
            public void onSuccess(List<SearchModels.Landmark> schools) {
                layoutSchools.removeAllViews();
                if (schools.isEmpty()) {
                    TextView empty = new TextView(SearchActivity.this);
                    empty.setText(R.string.search_schools_empty);
                    empty.setTextColor(getColor(R.color.text_secondary));
                    empty.setPadding(0, 16, 0, 0);
                    layoutSchools.addView(empty);
                }
                for (SearchModels.Landmark school : schools) {
                    String sub = getString(R.string.search_school_sub,
                            school.addressText != null ? school.addressText : "", school.roomCount);
                    View row = inflateRow(layoutSchools, school.name, sub, R.drawable.ic_school);
                    row.setOnClickListener(v -> chooseSchool(school));
                    layoutSchools.addView(row);
                }
            }

            @Override
            public void onError(String message) {
                // Không có gợi ý vẫn tìm theo từ khóa được
                layoutSchools.removeAllViews();
            }
        });
    }

    private View inflateRow(LinearLayout parent, String title, String subtitle, int iconRes) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_search_suggestion, parent, false);
        ((TextView) row.findViewById(R.id.tv_title)).setText(title);
        ((TextView) row.findViewById(R.id.tv_subtitle)).setText(subtitle);
        ((ImageView) row.findViewById(R.id.img_icon)).setImageResource(iconRes);
        return row;
    }
}
