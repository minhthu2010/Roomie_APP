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
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchActivity;
import com.example.roomie.ui.search.SearchCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Khám phá: feed "Phòng hợp với bạn" (sắp theo % phù hợp) và kết quả tìm kiếm / bộ lọc.
 * Màn Tìm kiếm và Bộ lọc trả SearchCriteria về đây qua Activity Result.
 */
public class HomeActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10;
    private static final String STATE_CRITERIA = "criteria";
    // Chưa có ảnh thật (B làm upload ảnh) thì dùng ảnh minh họa xoay vòng
    private static final int[] PLACEHOLDERS = {R.drawable.img_room1, R.drawable.img_room2, R.drawable.img_room3};

    private DiscoveryApi api;
    private SearchCriteria criteria = new SearchCriteria();
    private int page = 1;
    private int loadedCount = 0;
    private boolean loading = false;

    private TextView tvSearchText;
    private View scrollChips;
    private LinearLayout layoutChips;
    private TextView tvFeedTitle;
    private TextView tvFeedSubtitle;
    private TextView tvSortLabel;
    private LinearLayout layoutRooms;
    private TextView tvEmpty;
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

        tvSearchText = findViewById(R.id.tv_search_text);
        scrollChips = findViewById(R.id.scroll_active_chips);
        layoutChips = findViewById(R.id.layout_active_chips);
        tvFeedTitle = findViewById(R.id.tv_feed_title);
        tvFeedSubtitle = findViewById(R.id.tv_feed_subtitle);
        tvSortLabel = findViewById(R.id.tv_sort_label);
        layoutRooms = findViewById(R.id.layout_feed_rooms);
        tvEmpty = findViewById(R.id.tv_feed_empty);
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
            btnViewMap.setOnClickListener(v -> startActivity(new Intent(this, BanDoGiaPhongActivity.class)));
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
        btnLoadMore.setVisibility(View.GONE);
        tvFeedSubtitle.setText(R.string.feed_loading);
        bindHeader();
        loadPage();
    }

    private void loadPage() {
        if (loading) return;
        loading = true;
        btnLoadMore.setEnabled(false);
        api.searchRooms(criteria.toQuery(page, PAGE_SIZE))
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
                        if (total == 0) {
                            tvEmpty.setText(R.string.feed_empty);
                            tvEmpty.setOnClickListener(null);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
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

    // ---------- Hiển thị ----------

    private void addRoomCard(SearchModels.RoomCard room) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_feed_room_card, layoutRooms, false);
        ((ImageView) card.findViewById(R.id.img_room))
                .setImageResource(PLACEHOLDERS[layoutRooms.getChildCount() % PLACEHOLDERS.length]);

        TextView badge = card.findViewById(R.id.tv_match_badge);
        if (room.matchPercent != null) {
            badge.setText(getString(R.string.feed_match_badge, room.matchPercent));
        } else {
            badge.setVisibility(View.GONE);
        }
        ((ImageView) card.findViewById(R.id.img_saved))
                .setImageResource(room.isSaved ? R.drawable.ic_heart : R.drawable.ic_heart_outline);

        ((TextView) card.findViewById(R.id.tv_room_title)).setText(room.title);
        String address = room.addressText != null ? room.addressText : "";
        if (room.distanceKm != null) address = getString(R.string.feed_distance, address, RoomFormat.km(room.distanceKm));
        ((TextView) card.findViewById(R.id.tv_room_address)).setText(address);
        ((TextView) card.findViewById(R.id.tv_room_price))
                .setText(getString(R.string.feed_price, RoomFormat.money(room.monthlyRent)));

        card.setOnClickListener(v -> startActivity(ChiTietPhongActivity.intent(this, room.id)));
        layoutRooms.addView(card);
    }

    private void bindHeader() {
        boolean searching = criteria.hasLocation() || criteria.hasFilters();
        tvFeedTitle.setText(searching ? R.string.feed_title_search : R.string.feed_section_title);
        tvSortLabel.setText(RoomFormat.sortLabel(criteria.sort));

        String location = criteria.landmarkName != null ? criteria.landmarkName
                : criteria.area != null ? criteria.area : criteria.query;
        tvSearchText.setText(location != null ? location : getString(R.string.hint_search));
        tvSearchText.setTextColor(getColor(location != null ? R.color.text_primary : R.color.text_secondary));

        // Mỗi điều kiện là một chip, chạm để bỏ điều kiện đó
        layoutChips.removeAllViews();
        if (criteria.landmarkId != null) {
            addChip(getString(R.string.chip_near, criteria.landmarkName), () -> criteria.clearLocation());
        } else if (criteria.area != null) {
            addChip(criteria.area, () -> criteria.clearLocation());
        } else if (criteria.query != null) {
            addChip(getString(R.string.chip_query, criteria.query), () -> criteria.clearLocation());
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
        scrollChips.setVisibility(layoutChips.getChildCount() > 0 ? View.VISIBLE : View.GONE);
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
        if (criteria.landmarkId != null) sorts.add(SearchCriteria.SORT_DISTANCE);

        String[] labels = new String[sorts.size()];
        for (int i = 0; i < sorts.size(); i++) labels[i] = getString(RoomFormat.sortLabel(sorts.get(i)));
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
