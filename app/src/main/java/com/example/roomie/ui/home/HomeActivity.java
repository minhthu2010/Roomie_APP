package com.example.roomie.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.SearchModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.filter.BoLocActivity;
import com.example.roomie.ui.search.ProvinceStore;
import com.example.roomie.ui.search.RoomCardBinder;
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchActivity;
import com.example.roomie.ui.search.SearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Khám phá: feed "Phòng hợp với bạn" (sắp theo % phù hợp) và kết quả tìm kiếm / bộ lọc.
 * Màn Tìm kiếm và Bộ lọc trả SearchCriteria về đây qua Activity Result.
 */
public class HomeActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10;
    private static final String STATE_CRITERIA = "criteria";
    /** Bán kính mặc định của server khi không gửi radiusKm, và mức mở rộng tối đa */
    private static final int DEFAULT_RADIUS_KM = 3;
    private static final int MAX_RADIUS_KM = 10;

    private DiscoveryApi api;
    private SearchCriteria criteria = new SearchCriteria();
    private int page = 1;
    private int loadedCount = 0;
    private boolean loading = false;

    private TextView tvHeaderCity;
    private TextView tvSearchText;
    private View scrollChips;
    private LinearLayout layoutChips;
    private TextView tvFeedTitle;
    private TextView tvFeedSubtitle;
    private TextView tvSortLabel;
    private LinearLayout layoutRooms;
    private TextView tvEmpty;
    private LinearLayout layoutEmptyActions;
    private TextView btnLoadMore;

    private final ActivityResultLauncher<Intent> criteriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    SearchCriteria c = (SearchCriteria) result.getData().getSerializableExtra(SearchCriteria.EXTRA);
                    if (c != null) {
                        criteria = c;
                        reload();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);
        if (savedInstanceState != null) {
            SearchCriteria saved = (SearchCriteria) savedInstanceState.getSerializable(STATE_CRITERIA);
            if (saved != null) criteria = saved;
        }

        tvHeaderCity = findViewById(R.id.tv_header_city);
        tvHeaderCity.setOnClickListener(v -> ProvinceStore.pick(this, province -> {
            // Phường / quận đã chọn thuộc tỉnh cũ -> bỏ
            if (criteria.wardCode != null || criteria.area != null) criteria.clearLocation();
            reload();
        }));
        tvSearchText = findViewById(R.id.tv_search_text);
        scrollChips = findViewById(R.id.scroll_active_chips);
        layoutChips = findViewById(R.id.layout_active_chips);
        tvFeedTitle = findViewById(R.id.tv_feed_title);
        tvFeedSubtitle = findViewById(R.id.tv_feed_subtitle);
        tvSortLabel = findViewById(R.id.tv_sort_label);
        layoutRooms = findViewById(R.id.layout_feed_rooms);
        tvEmpty = findViewById(R.id.tv_feed_empty);
        layoutEmptyActions = findViewById(R.id.layout_empty_actions);
        btnLoadMore = findViewById(R.id.btn_load_more);

        // Filter button click opens "Bộ lọc phòng" screen
        findViewById(R.id.btn_filter).setOnClickListener(v -> criteriaLauncher.launch(
                new Intent(this, BoLocActivity.class).putExtra(SearchCriteria.EXTRA, criteria)));

        // Search bar click opens "Tìm kiếm phòng" screen
        findViewById(R.id.layout_search_bar).setOnClickListener(v -> criteriaLauncher.launch(
                new Intent(this, SearchActivity.class).putExtra(SearchCriteria.EXTRA, criteria)));

        // "Xem bản đồ" button click opens "Bản đồ giá phòng" screen
        View btnViewMap = findViewById(R.id.btn_view_map);
        if (btnViewMap != null) {
            // Bản đồ trả lại điều kiện đã đổi (giá, bộ lọc, vị trí) để danh sách khớp với bản đồ
            btnViewMap.setOnClickListener(v -> criteriaLauncher.launch(BanDoGiaPhongActivity.intent(this, criteria)));
        }

        findViewById(R.id.layout_sort).setOnClickListener(v -> showSortDialog());
        btnLoadMore.setOnClickListener(v -> loadPage());

        reload();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_CRITERIA, criteria);
    }

    // ---------- Tải dữ liệu ----------

    private void reload() {
        page = 1;
        loadedCount = 0;
        layoutRooms.removeAllViews();
        tvEmpty.setVisibility(View.GONE);
        layoutEmptyActions.setVisibility(View.GONE);
        btnLoadMore.setVisibility(View.GONE);
        tvFeedSubtitle.setText(R.string.feed_loading);
        bindHeader();
        loadPage();
    }

    private void loadPage() {
        if (loading) return;
        loading = true;
        btnLoadMore.setEnabled(false);
        Map<String, String> query = criteria.toQuery(page, PAGE_SIZE);
        // Feed / từ khóa chỉ trong tỉnh đang chọn (backend bỏ qua khi tìm quanh 1 điểm hoặc theo phường)
        query.put("provinceCode", ProvinceStore.code(this));
        api.searchRooms(query)
                .enqueue(new ApiCallback<List<SearchModels.RoomCard>>(this) {
                    @Override
                    public void onSuccess(List<SearchModels.RoomCard> rooms) {
                        loading = false;
                        btnLoadMore.setEnabled(true);
                        int total = getMeta() != null ? getMeta().total : rooms.size();
                        for (SearchModels.RoomCard room : rooms) addRoomCard(room);
                        loadedCount += rooms.size();
                        page++;

                        boolean searching = criteria.hasLocation() || criteria.hasFilters();
                        tvFeedSubtitle.setText(getString(searching ? R.string.feed_count_search : R.string.feed_count, total));
                        btnLoadMore.setVisibility(loadedCount < total ? View.VISIBLE : View.GONE);
                        if (total == 0) showEmpty();
                    }

                    @Override
                    public void onError(String message) {
                        loading = false;
                        btnLoadMore.setEnabled(true);
                        tvFeedSubtitle.setText("");
                        if (loadedCount == 0) {
                            tvEmpty.setText(getString(R.string.feed_error, message));
                            tvEmpty.setOnClickListener(v -> reload());
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /**
     * Không có phòng: gợi ý cách tìm rộng hơn thay vì chỉ báo trống.
     * Đang tìm quanh 1 điểm -> mở rộng bán kính; có bộ lọc -> bỏ bộ lọc; tìm theo khu vực -> mọi khu vực.
     */
    private void showEmpty() {
        layoutEmptyActions.removeAllViews();
        String text = getString(R.string.feed_empty);
        if (!criteria.hasLocation() && !criteria.hasFilters()) {
            // Tỉnh đang chọn chưa có bài đăng nào
            text = getString(R.string.feed_empty_province, ProvinceStore.shortName(ProvinceStore.get(this).name));
            addEmptyAction(getString(R.string.feed_empty_change_province), null);
        }
        if (criteria.hasDistanceOrigin()) {
            int radius = criteria.radiusKm != null ? criteria.radiusKm : DEFAULT_RADIUS_KM;
            text = getString(R.string.feed_empty_radius, radius);
            if (radius < MAX_RADIUS_KM) {
                int wider = radius < 5 ? 5 : MAX_RADIUS_KM;
                addEmptyAction(getString(R.string.feed_empty_widen, wider), () -> criteria.radiusKm = wider);
            }
        }
        if (criteria.hasFilters()) {
            addEmptyAction(getString(R.string.feed_empty_clear_filters), () -> {
                Integer radius = criteria.radiusKm;   // bán kính thuộc về vị trí, giữ lại
                criteria.clearFilters();
                criteria.radiusKm = radius;
            });
        } else if (criteria.hasLocation() && !criteria.hasDistanceOrigin()) {
            addEmptyAction(getString(R.string.feed_empty_clear_location), () -> criteria.clearLocation());
        }
        tvEmpty.setText(text);
        tvEmpty.setOnClickListener(null);
        tvEmpty.setVisibility(View.VISIBLE);
        layoutEmptyActions.setVisibility(layoutEmptyActions.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    /** change = null: nút "Đổi tỉnh/thành" (mở hộp chọn tỉnh, chọn xong mới tải lại) */
    private void addEmptyAction(String label, Runnable change) {
        TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_filter_chip, layoutEmptyActions, false);
        chip.setText(label);
        chip.setOnClickListener(v -> {
            if (change == null) {
                tvHeaderCity.performClick();
                return;
            }
            change.run();
            reload();
        });
        layoutEmptyActions.addView(chip);
    }

    // ---------- Hiển thị ----------

    private void addRoomCard(SearchModels.RoomCard room) {
        layoutRooms.addView(RoomCardBinder.create(this, layoutRooms, room, layoutRooms.getChildCount()));
    }

    private void bindHeader() {
        tvHeaderCity.setText(getString(R.string.header_city_pick,
                ProvinceStore.get(this).name.toUpperCase(new Locale("vi", "VN"))));
        boolean searching = criteria.hasLocation() || criteria.hasFilters();
        tvFeedTitle.setText(searching ? R.string.feed_title_search : R.string.feed_section_title);
        tvSortLabel.setText(sortLabel(criteria.sort));

        String location = criteria.locationLabel();
        tvSearchText.setText(location != null ? location : getString(R.string.hint_search));
        tvSearchText.setTextColor(getColor(location != null ? R.color.text_primary : R.color.text_secondary));

        // Mỗi điều kiện là một chip, chạm để bỏ điều kiện đó
        layoutChips.removeAllViews();
        if (criteria.landmarkId != null) {
            addChip(getString(R.string.chip_near, criteria.landmarkName), () -> criteria.clearLocation());
        } else if (criteria.nearMe) {
            addChip(getString(R.string.chip_near_me), () -> criteria.clearLocation());
        } else if (criteria.placeName != null) {
            addChip(getString(R.string.chip_near_place, criteria.placeName), () -> criteria.clearLocation());
        } else if (criteria.wardName != null) {
            addChip(criteria.wardName, () -> criteria.clearLocation());
        } else if (criteria.area != null) {
            addChip(criteria.area, () -> criteria.clearLocation());
        } else if (criteria.query != null) {
            addChip(getString(R.string.chip_query, criteria.query), () -> criteria.clearLocation());
        }
        if (criteria.radiusKm != null && criteria.hasDistanceOrigin()) {
            addChip(getString(R.string.chip_radius, criteria.radiusKm), () -> criteria.radiusKm = null);
        }
        if (criteria.minRent != null || criteria.maxRent != null) {
            addChip(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent), () -> {
                criteria.minRent = null;
                criteria.maxRent = null;
            });
        }
        for (String type : new ArrayList<>(criteria.roomTypes)) {
            addChip(getString(RoomFormat.roomTypeLabel(type)), () -> criteria.roomTypes.remove(type));
        }
        if (criteria.gender != null) {
            addChip(getString("female".equals(criteria.gender) ? R.string.chip_gender_female : R.string.chip_gender_male),
                    () -> criteria.gender = null);
        }
        if (!criteria.amenities.isEmpty()) {
            addChip(getString(R.string.chip_amenities, criteria.amenities.size()), () -> criteria.amenities.clear());
        }
        if (criteria.occupation != null) {
            addChip(getString("student".equals(criteria.occupation)
                    ? R.string.filter_occupation_student : R.string.filter_occupation_worker), () -> criteria.occupation = null);
        }
        if (criteria.moveInDays != null) {
            addChip(criteria.moveInDays == 0 ? getString(R.string.chip_move_in_now)
                    : getString(R.string.chip_move_in_days, criteria.moveInDays), () -> criteria.moveInDays = null);
        }
        if (criteria.minSlots != null) {
            addChip(getString(R.string.chip_slots, criteria.minSlots), () -> criteria.minSlots = null);
        }
        if (criteria.minArea != null) {
            addChip(getString(R.string.chip_area_min, criteria.minArea), () -> criteria.minArea = null);
        }
        if (criteria.maxDeposit != null) {
            addChip(getString(R.string.chip_deposit_max, RoomFormat.millions(criteria.maxDeposit)),
                    () -> criteria.maxDeposit = null);
        }
        scrollChips.setVisibility(layoutChips.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    // "Gần trường nhất" hay "Gần địa chỉ nhất" tùy điểm gốc
    private int sortLabel(String sort) {
        if (SearchCriteria.SORT_DISTANCE.equals(sort) && criteria.nearMe) return R.string.sort_distance_me;
        if (SearchCriteria.SORT_DISTANCE.equals(sort) && criteria.placeName != null) return R.string.sort_distance_place;
        return RoomFormat.sortLabel(sort);
    }

    private void addChip(String label, Runnable onRemove) {
        TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_filter_chip, layoutChips, false);
        chip.setText(getString(R.string.chip_remove, label));
        chip.setOnClickListener(v -> {
            onRemove.run();
            reload();
        });
        layoutChips.addView(chip);
    }

    private void showSortDialog() {
        List<String> sorts = new ArrayList<>();
        sorts.add(SearchCriteria.SORT_MATCH);
        sorts.add(SearchCriteria.SORT_NEWEST);
        sorts.add(SearchCriteria.SORT_PRICE_ASC);
        sorts.add(SearchCriteria.SORT_PRICE_DESC);
        if (criteria.hasDistanceOrigin()) sorts.add(SearchCriteria.SORT_DISTANCE);

        String[] labels = new String[sorts.size()];
        for (int i = 0; i < sorts.size(); i++) labels[i] = getString(sortLabel(sorts.get(i)));
        new AlertDialog.Builder(this)
                .setTitle(R.string.sort_title)
                .setSingleChoiceItems(labels, Math.max(0, sorts.indexOf(criteria.sort)), (dialog, which) -> {
                    dialog.dismiss();
                    if (!sorts.get(which).equals(criteria.sort)) {
                        criteria.sort = sorts.get(which);
                        reload();
                    }
                })
                .show();
    }
}
