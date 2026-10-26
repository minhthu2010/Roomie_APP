package com.example.roomie.ui.saved;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.search.RoomFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phòng đã lưu. Chip lọc: tất cả / quận có nhiều phòng đã lưu nhất / gần trường.
 * Bỏ lưu ngay trên thẻ; phòng đã đủ người hoặc hết hạn hiện nhãn trạng thái.
 */
public class PhongDaLuuActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_DISTRICT = 1;
    private static final int FILTER_SCHOOL = 2;
    private static final int[] PLACEHOLDERS = {R.drawable.img_room1, R.drawable.img_room2, R.drawable.img_room3};

    private final List<RoomModels.SavedRoom> rooms = new ArrayList<>();
    private int filter = FILTER_ALL;
    private String topDistrict;

    private LinearLayout layoutRooms;
    private TextView tvEmpty;
    private TextView chipAll;
    private TextView chipDistrict;
    private TextView chipSchool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phong_da_luu);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutRooms = findViewById(R.id.layout_saved_rooms);
        tvEmpty = findViewById(R.id.tv_saved_empty);
        chipAll = findViewById(R.id.chip_saved_all);
        chipDistrict = findViewById(R.id.chip_saved_district);
        chipSchool = findViewById(R.id.chip_saved_school);

        chipAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        chipDistrict.setOnClickListener(v -> setFilter(FILTER_DISTRICT));
        chipSchool.setOnClickListener(v -> setFilter(FILTER_SCHOOL));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại khi quay về: có thể vừa lưu / bỏ lưu ở màn chi tiết
        load();
    }

    private void load() {
        ApiClient.create(this, DiscoveryApi.class).getSavedRooms(1, 50)
                .enqueue(new ApiCallback<List<RoomModels.SavedRoom>>(this) {
                    @Override
                    public void onSuccess(List<RoomModels.SavedRoom> data) {
                        rooms.clear();
                        if (data != null) rooms.addAll(data);
                        render();
                    }

                    @Override
                    public void onError(String message) {
                        layoutRooms.removeAllViews();
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(getString(R.string.feed_error, message));
                        tvEmpty.setOnClickListener(v -> load());
                    }
                });
    }

    private void setFilter(int f) {
        filter = f;
        render();
    }

    private void render() {
        // Quận có nhiều phòng đã lưu nhất cho chip thứ 2
        Map<String, Integer> byDistrict = new HashMap<>();
        int nearSchool = 0;
        for (RoomModels.SavedRoom r : rooms) {
            if (r.districtName != null) byDistrict.merge(r.districtName, 1, Integer::sum);
            if (r.landmarkName != null) nearSchool++;
        }
        topDistrict = null;
        int topCount = 0;
        for (Map.Entry<String, Integer> e : byDistrict.entrySet()) {
            if (e.getValue() > topCount) {
                topDistrict = e.getKey();
                topCount = e.getValue();
            }
        }
        chipAll.setText(getString(R.string.saved_chip_all, rooms.size()));
        chipDistrict.setVisibility(topDistrict != null ? View.VISIBLE : View.GONE);
        if (topDistrict != null) chipDistrict.setText(getString(R.string.saved_chip_district, topDistrict, topCount));
        chipSchool.setText(getString(R.string.saved_chip_school, nearSchool));
        styleChip(chipAll, filter == FILTER_ALL);
        styleChip(chipDistrict, filter == FILTER_DISTRICT);
        styleChip(chipSchool, filter == FILTER_SCHOOL);

        layoutRooms.removeAllViews();
        for (RoomModels.SavedRoom r : rooms) {
            if (filter == FILTER_DISTRICT && (topDistrict == null || !topDistrict.equals(r.districtName))) continue;
            if (filter == FILTER_SCHOOL && r.landmarkName == null) continue;
            layoutRooms.addView(buildCard(r));
        }
        tvEmpty.setOnClickListener(null);
        tvEmpty.setVisibility(layoutRooms.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        tvEmpty.setText(rooms.isEmpty() ? R.string.saved_empty : R.string.saved_empty_filter);
    }

    private View buildCard(RoomModels.SavedRoom r) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_saved_room_card, layoutRooms, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(lp);

        ImageLoader.load(card.findViewById(R.id.img_saved_room), r.coverImageUrl,
                PLACEHOLDERS[layoutRooms.getChildCount() % PLACEHOLDERS.length]);

        // Nhãn: trạng thái nếu không còn tìm người, ngược lại % phù hợp
        TextView badge = card.findViewById(R.id.tv_saved_match);
        // Quá expires_at nhưng listing_status vẫn "searching" thì cũng là hết hạn
        String status = statusLabel(r.listingStatus);
        if (!r.canContact) {
            if (status == null) status = getString(R.string.saved_status_expired);
            badge.setText(status);
            badge.setBackgroundResource(R.drawable.bg_badge_sage);
            badge.setTextColor(getColor(R.color.text_primary));
            card.setAlpha(0.7f);
        } else if (r.matchPercent != null) {
            badge.setText(getString(R.string.feed_match_badge, r.matchPercent));
        } else {
            badge.setVisibility(View.GONE);
        }

        ((TextView) card.findViewById(R.id.tv_saved_title)).setText(r.title);
        ((TextView) card.findViewById(R.id.tv_saved_address)).setText(r.addressText);
        ((TextView) card.findViewById(R.id.tv_saved_price))
                .setText(getString(R.string.feed_price, RoomFormat.money(r.monthlyRent)));

        ImageView heart = card.findViewById(R.id.btn_saved_heart);
        SaveToggle.bindIcon(heart, true);
        heart.setOnClickListener(v -> SaveToggle.toggle(this, r.id, true, heart, saved -> {
            if (!saved) {
                rooms.remove(r);
                render();
            }
        }));

        card.setOnClickListener(v -> startActivity(ChiTietPhongActivity.intent(this, r.id)));
        return card;
    }

    private String statusLabel(String listingStatus) {
        if ("filled".equals(listingStatus)) return getString(R.string.saved_status_filled);
        if ("hidden".equals(listingStatus)) return getString(R.string.saved_status_hidden);
        if ("expired".equals(listingStatus)) return getString(R.string.saved_status_expired);
        return null;
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(getColor(selected ? R.color.white : R.color.text_primary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        float d = getResources().getDisplayMetrics().density;
        chip.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));
    }
}
