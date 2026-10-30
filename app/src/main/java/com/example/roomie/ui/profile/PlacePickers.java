package com.example.roomie.ui.profile;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.PlaceModels;
import com.example.roomie.data.discovery.SearchModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.search.ProvinceStore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hộp chọn cho hồ sơ, dùng API tra cứu của người C:
 * - Khu vực muốn tìm phòng: tỉnh/thành -> phường/xã (hoặc cả tỉnh)
 * - Trường / nơi làm: gợi ý trường thật, hoặc tự ghi nơi làm khác
 */
final class PlacePickers {

    private PlacePickers() {}

    // ---------- Khu vực ----------

    static final class Area {
        final String provinceCode;
        final String provinceName;
        /** null = cả tỉnh */
        final String wardCode;
        final String wardName;

        Area(String provinceCode, String provinceName, String wardCode, String wardName) {
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
            this.wardCode = wardCode;
            this.wardName = wardName;
        }

        /** "Phường Bàn Cờ, TP. Hồ Chí Minh" hoặc "TP. Hồ Chí Minh" */
        String label() {
            String province = ProvinceStore.shortName(provinceName);
            return wardName != null ? wardName + ", " + province : province;
        }
    }

    interface AreaListener {
        void onArea(Area area);
    }

    static void pickArea(AppCompatActivity activity, String currentProvinceCode, AreaListener listener) {
        DiscoveryApi api = ApiClient.create(activity, DiscoveryApi.class);
        api.getProvinces().enqueue(new ApiCallback<List<PlaceModels.Province>>(activity) {
            @Override
            public void onSuccess(List<PlaceModels.Province> provinces) {
                if (provinces == null || provinces.isEmpty()) return;
                String[] names = new String[provinces.size()];
                int checked = -1;
                for (int i = 0; i < provinces.size(); i++) {
                    names[i] = provinces.get(i).name;
                    if (provinces.get(i).code.equals(currentProvinceCode)) checked = i;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.province_pick_title)
                        .setSingleChoiceItems(names, checked, (dialog, which) -> {
                            dialog.dismiss();
                            pickWard(activity, api, provinces.get(which), listener);
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(activity, message);
            }
        });
    }

    private static void pickWard(AppCompatActivity activity, DiscoveryApi api, PlaceModels.Province province,
                                 AreaListener listener) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_ward_picker, null, false);
        EditText edtFilter = content.findViewById(R.id.edt_ward_filter);
        TextView tvStatus = content.findViewById(R.id.tv_ward_status);
        ListView list = content.findViewById(R.id.list_wards);
        edtFilter.setHint(R.string.ward_search_hint);

        List<PlaceModels.Ward> all = new ArrayList<>();
        List<PlaceModels.Ward> shown = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);

        String provinceShort = ProvinceStore.shortName(province.name);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.area_pick_ward_title, provinceShort))
                .setView(content)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        // Mục đầu "Toàn bộ <tỉnh>" (ward = null), sau đó các phường khớp chữ đang gõ (không dấu)
        Runnable refresh = () -> {
            String q = normalize(edtFilter.getText().toString());
            shown.clear();
            adapter.clear();
            shown.add(null);
            adapter.add(activity.getString(R.string.area_whole_province, provinceShort));
            for (PlaceModels.Ward w : all) {
                if (!q.isEmpty() && !normalize(w.name).contains(q)) continue;
                shown.add(w);
                adapter.add(w.name);
            }
            adapter.notifyDataSetChanged();
        };
        edtFilter.addTextChangedListener(afterChange(refresh));
        list.setOnItemClickListener((parent, view, position, id) -> {
            PlaceModels.Ward w = shown.get(position);
            dialog.dismiss();
            listener.onArea(new Area(province.code, province.name,
                    w != null ? w.code : null, w != null ? w.name : null));
        });

        dialog.show();
        refresh.run();
        tvStatus.setText(R.string.loading);
        tvStatus.setVisibility(View.VISIBLE);
        api.getWards(province.code, null, 400).enqueue(new ApiCallback<List<PlaceModels.Ward>>(activity) {
            @Override
            public void onSuccess(List<PlaceModels.Ward> wards) {
                if (wards != null) all.addAll(wards);
                tvStatus.setVisibility(View.GONE);
                refresh.run();
            }

            @Override
            public void onError(String message) {
                tvStatus.setText(message);
            }
        });
    }

    // ---------- Trường / nơi làm ----------

    static final class School {
        /** 0 = nơi làm tự ghi (không thuộc danh sách gợi ý) */
        final int landmarkId;
        final String name;

        School(int landmarkId, String name) {
            this.landmarkId = landmarkId;
            this.name = name;
        }
    }

    interface SchoolListener {
        void onSchool(School school);
    }

    /** provinceCode: ưu tiên trường trong khu vực muốn tìm phòng (có thể null) */
    static void pickSchool(AppCompatActivity activity, String provinceCode, SchoolListener listener) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_ward_picker, null, false);
        EditText edtFilter = content.findViewById(R.id.edt_ward_filter);
        TextView tvStatus = content.findViewById(R.id.tv_ward_status);
        ListView list = content.findViewById(R.id.list_wards);
        edtFilter.setHint(R.string.school_pick_hint);

        List<School> shown = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.school_pick_title)
                .setView(content)
                .setNegativeButton(R.string.btn_cancel, null)
                .setNeutralButton(R.string.school_none, (d, w) -> listener.onSchool(new School(0, null)))
                .create();

        DiscoveryApi api = ApiClient.create(activity, DiscoveryApi.class);
        Handler handler = new Handler(Looper.getMainLooper());
        int[] requestNo = {0};

        Runnable search = () -> {
            String typed = edtFilter.getText().toString().trim();
            int myRequest = ++requestNo[0];
            tvStatus.setText(R.string.loading);
            tvStatus.setVisibility(View.VISIBLE);
            api.suggestLandmarks(typed.isEmpty() ? null : typed, "university", 20, provinceCode)
                    .enqueue(new ApiCallback<List<SearchModels.Landmark>>(activity) {
                        @Override
                        public void onSuccess(List<SearchModels.Landmark> landmarks) {
                            if (myRequest != requestNo[0]) return; // đã có lần gõ mới hơn
                            shown.clear();
                            adapter.clear();
                            if (landmarks != null) {
                                for (SearchModels.Landmark l : landmarks) {
                                    shown.add(new School(l.id, l.name));
                                    adapter.add(l.shortName != null ? l.shortName + " · " + l.name : l.name);
                                }
                            }
                            // Người đi làm: cho ghi nơi làm không có trong danh sách
                            if (!typed.isEmpty()) {
                                shown.add(new School(0, typed));
                                adapter.add(activity.getString(R.string.school_other, typed));
                            }
                            adapter.notifyDataSetChanged();
                            if (shown.isEmpty()) {
                                tvStatus.setText(R.string.no_result);
                            } else {
                                tvStatus.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (myRequest == requestNo[0]) tvStatus.setText(message);
                        }
                    });
        };
        // Chờ người dùng ngừng gõ 300ms rồi mới gọi API
        edtFilter.addTextChangedListener(afterChange(() -> {
            handler.removeCallbacks(search);
            handler.postDelayed(search, 300);
        }));
        list.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            listener.onSchool(shown.get(position));
        });
        dialog.setOnDismissListener(d -> handler.removeCallbacks(search));

        dialog.show();
        search.run();
    }

    // ---------- tiện ích ----------

    private static TextWatcher afterChange(Runnable action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                action.run();
            }
        };
    }

    /** "Phường Diên Hồng" -> "phuong dien hong" để lọc không dấu */
    private static String normalize(String s) {
        String noMarks = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noMarks.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
