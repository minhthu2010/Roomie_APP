package com.example.roomie.ui.filter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchCriteria;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bộ lọc phòng: ngân sách, loại chỗ ở, giới tính, tiện ích, ngày chuyển vào...
 * Không chọn địa chỉ ở đây (ô tìm kiếm lo phần "ở đâu"); chỉ có bán kính khi đang tìm quanh 1 điểm.
 * Nhận và trả SearchCriteria (SearchCriteria.EXTRA); "Xem phòng" áp dụng, "Xóa lọc" bỏ hết bộ lọc.
 */
public class BoLocActivity extends AppCompatActivity {

    private static final float BUDGET_MAX = 10_000_000f;

    private SearchCriteria criteria;

    private TextView tvBudget;
    private RangeSlider rangeBudget;
    // chip -> giá trị gửi API
    private final Map<Integer, String> roomTypeChips = new LinkedHashMap<>();
    private final Map<Integer, String> genderChips = new LinkedHashMap<>();
    private final Map<Integer, String> amenityChips = new LinkedHashMap<>();
    /** Tô lại các nhóm chip chọn 1 (bán kính, ngày chuyển vào...) theo criteria */
    private final List<Runnable> choiceBinders = new ArrayList<>();
    private TextView tvRadiusNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bo_loc);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SearchCriteria given = (SearchCriteria) getIntent().getSerializableExtra(SearchCriteria.EXTRA);
        criteria = given != null ? given.copy() : new SearchCriteria();

        roomTypeChips.put(R.id.chip_type_shared, "shared_room");
        roomTypeChips.put(R.id.chip_type_private, "private_room");
        roomTypeChips.put(R.id.chip_type_apartment, "apartment");
        roomTypeChips.put(R.id.chip_type_house, "whole_house");
        roomTypeChips.put(R.id.chip_type_studio, "studio");
        genderChips.put(R.id.chip_gender_female, "female");
        genderChips.put(R.id.chip_gender_male, "male");
        genderChips.put(R.id.chip_gender_any, null);
        amenityChips.put(R.id.img_amenity_ac, "air_conditioning");
        amenityChips.put(R.id.img_amenity_washer, "washing_machine");
        amenityChips.put(R.id.img_amenity_kitchen, "kitchen");
        amenityChips.put(R.id.img_amenity_parking, "parking");

        tvBudget = findViewById(R.id.tv_budget_range);
        rangeBudget = findViewById(R.id.range_budget);
        tvRadiusNote = findViewById(R.id.tv_radius_note);
        rangeBudget.setTickVisible(false);   // bước 500.000đ tạo 20 dấu chấm trên thanh

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // "Xóa lọc": bỏ bộ lọc; vị trí đang tìm (ô tìm kiếm) giữ nguyên
        findViewById(R.id.btn_clear_filter).setOnClickListener(v -> {
            criteria.clearFilters();
            bindAll();
        });

        findViewById(R.id.btn_apply_filter).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(SearchCriteria.EXTRA, criteria));
            finish();
        });

        rangeBudget.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            List<Float> values = slider.getValues();
            float min = values.get(0);
            float max = values.get(1);
            criteria.minRent = min <= 0 ? null : (long) min;
            criteria.maxRent = max >= BUDGET_MAX ? null : (long) max;
            tvBudget.setText(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent));
        });

        for (Map.Entry<Integer, String> e : roomTypeChips.entrySet()) {
            findViewById(e.getKey()).setOnClickListener(v -> {
                if (!criteria.roomTypes.remove(e.getValue())) criteria.roomTypes.add(e.getValue());
                bindChips();
            });
        }
        for (Map.Entry<Integer, String> e : genderChips.entrySet()) {
            findViewById(e.getKey()).setOnClickListener(v -> {
                criteria.gender = e.getValue();
                bindChips();
            });
        }
        for (Map.Entry<Integer, String> e : amenityChips.entrySet()) {
            ((View) findViewById(e.getKey()).getParent()).setOnClickListener(v -> {
                if (!criteria.amenities.remove(e.getValue())) criteria.amenities.add(e.getValue());
                bindChips();
            });
        }

        setupChoiceGroups();
        bindAll();
    }

    /** Các nhóm chip chọn 1 giá trị (null = "Bất kỳ" / mặc định) */
    private void setupChoiceGroups() {
        // 3 km là mặc định của server nên chọn 3 km = để trống
        setupChoice(R.id.group_radius,
                new String[]{km(1), km(2), km(3), km(5), km(10)},
                Arrays.asList(1, 2, null, 5, 10),
                () -> criteria.radiusKm, v -> criteria.radiusKm = v);
        setupChoice(R.id.group_occupation,
                new String[]{getString(R.string.filter_any), getString(R.string.filter_occupation_student),
                        getString(R.string.filter_occupation_worker)},
                Arrays.asList(null, "student", "worker"),
                () -> criteria.occupation, v -> criteria.occupation = v);
        setupChoice(R.id.group_move_in,
                new String[]{getString(R.string.filter_any), getString(R.string.filter_move_in_now),
                        getString(R.string.filter_move_in_2w), getString(R.string.filter_move_in_1m)},
                Arrays.asList(null, 0, 14, 30),
                () -> criteria.moveInDays, v -> criteria.moveInDays = v);
        setupChoice(R.id.group_slots,
                new String[]{getString(R.string.filter_slots_1), getString(R.string.filter_slots_n, 2),
                        getString(R.string.filter_slots_n, 3)},
                Arrays.asList(null, 2, 3),
                () -> criteria.minSlots, v -> criteria.minSlots = v);
        setupChoice(R.id.group_area_min,
                new String[]{getString(R.string.filter_any), getString(R.string.filter_area_m2, 15),
                        getString(R.string.filter_area_m2, 20), getString(R.string.filter_area_m2, 30)},
                Arrays.asList(null, 15, 20, 30),
                () -> criteria.minArea, v -> criteria.minArea = v);
        setupChoice(R.id.group_deposit,
                new String[]{getString(R.string.filter_any), getString(R.string.filter_deposit_max, "1"),
                        getString(R.string.filter_deposit_max, "2"), getString(R.string.filter_deposit_max, "3")},
                Arrays.asList(null, 1_000_000L, 2_000_000L, 3_000_000L),
                () -> criteria.maxDeposit, v -> criteria.maxDeposit = v);
    }

    private String km(int value) {
        return getString(R.string.filter_radius_km, value);
    }

    private <T> void setupChoice(int groupId, String[] labels, List<T> values, Supplier<T> getter, Consumer<T> setter) {
        LinearLayout group = findViewById(groupId);
        List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            TextView chip = new TextView(this);
            chip.setText(labels[i]);
            chip.setTextSize(14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(dp(10));
            T value = values.get(i);
            chip.setOnClickListener(v -> {
                setter.accept(value);
                bindChoices();
            });
            group.addView(chip, lp);
            chips.add(chip);
        }
        choiceBinders.add(() -> {
            T current = getter.get();
            for (int i = 0; i < chips.size(); i++) styleChip(chips.get(i), null, Objects.equals(values.get(i), current));
        });
    }

    private void bindChoices() {
        for (Runnable r : choiceBinders) r.run();
        // Bán kính chỉ có nghĩa khi đang tìm quanh trường / địa chỉ / vị trí của bạn
        int radius = criteria.hasDistanceOrigin() ? View.VISIBLE : View.GONE;
        findViewById(R.id.tv_radius_title).setVisibility(radius);
        findViewById(R.id.scroll_radius).setVisibility(radius);
        tvRadiusNote.setVisibility(radius);
        if (criteria.hasDistanceOrigin()) {
            tvRadiusNote.setText(getString(R.string.filter_radius_note_on, criteria.locationLabel()));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindAll() {
        rangeBudget.setValues(
                criteria.minRent != null ? (float) criteria.minRent : 0f,
                criteria.maxRent != null ? (float) criteria.maxRent : BUDGET_MAX);
        tvBudget.setText(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent));
        bindChips();
        bindChoices();
    }

    private void bindChips() {
        for (Map.Entry<Integer, String> e : roomTypeChips.entrySet()) {
            styleChip(findViewById(e.getKey()), null, criteria.roomTypes.contains(e.getValue()));
        }
        for (Map.Entry<Integer, String> e : genderChips.entrySet()) {
            boolean selected = e.getValue() == null ? criteria.gender == null : e.getValue().equals(criteria.gender);
            styleChip(findViewById(e.getKey()), null, selected);
        }
        for (Map.Entry<Integer, String> e : amenityChips.entrySet()) {
            ImageView icon = findViewById(e.getKey());
            View chip = (View) icon.getParent();
            TextView label = (TextView) ((ViewGroup) chip).getChildAt(1);
            boolean selected = criteria.amenities.contains(e.getValue());
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            styleChip(label, icon, selected);
            restorePadding(chip, 14);
        }
    }

    private void styleChip(TextView chip, ImageView icon, boolean selected) {
        int color = getColor(selected ? R.color.white : R.color.text_primary);
        chip.setTextColor(color);
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        if (icon != null) {
            icon.setImageTintList(ColorStateList.valueOf(color));
            return;
        }
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        restorePadding(chip, 16);
    }

    // setBackgroundResource xóa padding đặt trong XML
    private void restorePadding(View v, int horizontalDp) {
        float d = getResources().getDisplayMetrics().density;
        v.setPadding((int) (horizontalDp * d), (int) (10 * d), (int) (horizontalDp * d), (int) (10 * d));
    }

}
