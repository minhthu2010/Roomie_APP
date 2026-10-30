package com.example.roomie.ui.filter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.PlaceModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchCriteria;
import com.google.android.material.slider.RangeSlider;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bộ lọc phòng: khu vực, ngân sách, loại chỗ ở, giới tính, tiện ích.
 * Nhận và trả SearchCriteria (SearchCriteria.EXTRA); "Xem phòng" áp dụng, "Xóa lọc" bỏ hết.
 */
public class BoLocActivity extends AppCompatActivity {

    private static final float BUDGET_MAX = 10_000_000f;
    /** Bộ lọc khu vực trong TP.HCM (app đang phục vụ TP.HCM) */
    private static final String PROVINCE_CODE = "79";

    private SearchCriteria criteria;
    /** Phường/xã thật, tải 1 lần khi mở hộp chọn */
    private List<PlaceModels.Ward> wards;

    private TextView tvArea;
    private TextView tvBudget;
    private RangeSlider rangeBudget;
    // chip -> giá trị gửi API
    private final Map<Integer, String> roomTypeChips = new LinkedHashMap<>();
    private final Map<Integer, String> genderChips = new LinkedHashMap<>();
    private final Map<Integer, String> amenityChips = new LinkedHashMap<>();

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

        tvArea = findViewById(R.id.tv_filter_area);
        tvBudget = findViewById(R.id.tv_budget_range);
        rangeBudget = findViewById(R.id.range_budget);
        rangeBudget.setTickVisible(false);   // bước 500.000đ tạo 20 dấu chấm trên thanh

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.row_filter_area).setOnClickListener(v -> showAreaDialog());

        // "Đặt lại" và "Xóa lọc": bỏ bộ lọc (giữ từ khóa / trường đang tìm)
        View.OnClickListener reset = v -> {
            criteria.clearFilters();
            if (criteria.area != null || criteria.wardCode != null) criteria.clearLocation();
            bindAll();
        };
        findViewById(R.id.btn_reset).setOnClickListener(reset);
        findViewById(R.id.btn_clear_filter).setOnClickListener(reset);

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

        bindAll();
    }

    private void bindAll() {
        String area = criteria.wardName != null ? criteria.wardName : criteria.area;
        tvArea.setText(area != null
                ? getString(R.string.filter_area_suffix, area)
                : getString(R.string.filter_area_all));
        rangeBudget.setValues(
                criteria.minRent != null ? (float) criteria.minRent : 0f,
                criteria.maxRent != null ? (float) criteria.maxRent : BUDGET_MAX);
        tvBudget.setText(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent));
        bindChips();
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

    /** Chọn phường/xã thật của TP.HCM (168 phường/xã sau 01/07/2025), gõ để lọc không dấu */
    private void showAreaDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_ward_picker, null, false);
        EditText edtFilter = content.findViewById(R.id.edt_ward_filter);
        TextView tvStatus = content.findViewById(R.id.tv_ward_status);
        ListView list = content.findViewById(R.id.list_wards);

        // Mục đầu "Tất cả khu vực" (ward = null)
        List<PlaceModels.Ward> shown = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.filter_ward_title)
                .setView(content)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        Runnable refresh = () -> {
            String q = normalize(edtFilter.getText().toString());
            shown.clear();
            adapter.clear();
            shown.add(null);
            adapter.add(getString(R.string.filter_area_all));
            if (wards == null) return;
            for (PlaceModels.Ward w : wards) {
                if (!q.isEmpty() && !normalize(w.name).contains(q)) continue;
                shown.add(w);
                // "Phường Diên Hồng · 2 phòng"
                adapter.add(w.roomCount > 0 ? getString(R.string.places_ward_sub, w.name, w.roomCount) : w.name);
            }
            adapter.notifyDataSetChanged();
            tvStatus.setVisibility(View.GONE);
        };
        edtFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                refresh.run();
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            PlaceModels.Ward w = shown.get(position);
            criteria.clearLocation();
            if (w != null) {
                criteria.wardCode = w.code;
                criteria.wardName = w.name;
            }
            dialog.dismiss();
            bindAll();
        });

        dialog.show();
        if (wards != null) {
            refresh.run();
            return;
        }
        ApiClient.create(this, DiscoveryApi.class).getWards(PROVINCE_CODE, null, 400)
                .enqueue(new ApiCallback<List<PlaceModels.Ward>>(this) {
                    @Override
                    public void onSuccess(List<PlaceModels.Ward> data) {
                        wards = data != null ? data : new ArrayList<>();
                        refresh.run();
                    }

                    @Override
                    public void onError(String message) {
                        tvStatus.setText(message);
                    }
                });
    }

    /** "Phường Xuân Hoà" -> "phuong xuan hoa" để lọc không dấu */
    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
