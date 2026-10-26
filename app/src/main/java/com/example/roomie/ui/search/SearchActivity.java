package com.example.roomie.ui.search;

import android.content.Intent;
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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSuggest;
    private Call<?> suggestCall;

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

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_search_room).setOnClickListener(v -> searchByText());
        findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            recentStore.clear();
            renderRecent();
        });

        String current = criteria.landmarkName != null ? criteria.landmarkName
                : criteria.area != null ? criteria.area : criteria.query;
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
                pendingSuggest = () -> loadSchools(s.toString().trim());
                handler.postDelayed(pendingSuggest, SUGGEST_DELAY_MS);
            }
        });

        bindAreaChip(R.id.chip_area_q10);
        bindAreaChip(R.id.chip_area_q3);
        bindAreaChip(R.id.chip_area_binh_thanh);
        bindAreaChip(R.id.chip_area_phu_nhuan);

        renderRecent();
        loadSchools(null);
    }

    @Override
    protected void onDestroy() {
        if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
        if (suggestCall != null) suggestCall.cancel();
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
        criteria.sort = SearchCriteria.SORT_DISTANCE;
        RecentSearchStore.Entry e = new RecentSearchStore.Entry();
        e.title = name;
        e.subtitle = getString(R.string.search_recent_school_sub);
        e.landmarkId = school.id;
        recentStore.add(e);
        finishWithCriteria();
    }

    private void finishWithCriteria() {
        setResult(RESULT_OK, new Intent().putExtra(SearchCriteria.EXTRA, criteria));
        finish();
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
                if (item.landmarkId != null) criteria.sort = SearchCriteria.SORT_DISTANCE;
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
