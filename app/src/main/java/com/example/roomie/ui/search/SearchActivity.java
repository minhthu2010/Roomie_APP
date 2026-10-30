package com.example.roomie.ui.search;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
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
    /** Gõ từ 2 ký tự trở lên mới hiện gợi ý (thay phần lối tắt) */
    private static final int MIN_TYPED = 2;
    private static final long FRESH_LOCATION_MS = 5 * 60 * 1000;
    private static final long LOCATE_TIMEOUT_MS = 15 * 1000;
    // Khung bao Việt Nam (gồm Hoàng Sa, Trường Sa): ngoài khung này thì chưa có phòng
    private static final double VN_MIN_LAT = 7.5, VN_MAX_LAT = 23.5;
    private static final double VN_MIN_LNG = 102.1, VN_MAX_LNG = 117.5;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (hasLocationPermission()) locate();
                else AppNotificationHelper.showInfo(this, getString(R.string.search_near_me_denied));
            });
    private LocationListener locationListener;
    private Runnable locateTimeout;
    private TextView tvNearMeSub;

    private DiscoveryApi api;
    private RecentSearchStore recentStore;
    private SearchCriteria criteria;

    private EditText edtSearch;
    private View layoutRecentHeader;
    private LinearLayout layoutRecent;
    private LinearLayout layoutSchools;
    private LinearLayout layoutPlaces;
    private LinearLayout layoutTypedWards;
    private LinearLayout layoutTypedSchools;
    private LinearLayout layoutTypedAddresses;
    private View layoutIdle;
    private LinearLayout layoutPopular;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSuggest;
    private Call<?> suggestCall;
    private Call<?> typedSchoolsCall;
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
        layoutTypedWards = findViewById(R.id.layout_typed_wards);
        layoutTypedSchools = findViewById(R.id.layout_typed_schools);
        layoutTypedAddresses = findViewById(R.id.layout_typed_addresses);
        layoutIdle = findViewById(R.id.layout_idle);
        layoutPopular = findViewById(R.id.layout_popular);

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
        // Đang gõ: một danh sách gợi ý gộp (phường, trường, địa chỉ), ẩn phần lối tắt bên dưới.
        // Chờ 300ms sau phím cuối để không gọi API mỗi phím.
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
                String q = s.toString().trim();
                boolean typing = q.length() >= MIN_TYPED;
                layoutIdle.setVisibility(typing ? View.GONE : View.VISIBLE);
                layoutPlaces.setVisibility(typing ? View.VISIBLE : View.GONE);
                if (!typing) {
                    if (placesCall != null) placesCall.cancel();
                    if (typedSchoolsCall != null) typedSchoolsCall.cancel();
                    return;
                }
                pendingSuggest = () -> {
                    loadTypedSchools(q);
                    loadPlaces(q);
                };
                handler.postDelayed(pendingSuggest, SUGGEST_DELAY_MS);
            }
        });

        loadTopWards();

        renderNearMe();
        renderRecent();
        loadSchools(null);
    }

    @Override
    protected void onDestroy() {
        stopLocating();
        if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
        if (suggestCall != null) suggestCall.cancel();
        if (typedSchoolsCall != null) typedSchoolsCall.cancel();
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

    private void chooseSchool(SearchModels.Landmark school) {
        // Tên đầy đủ: tên viết tắt trên OSM ("ĐHKT", "HUST") khó nhận ra
        String name = school.name;
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

    // ---------- Phòng gần vị trí của tôi (GPS) ----------

    private void renderNearMe() {
        LinearLayout container = findViewById(R.id.layout_near_me);
        container.removeAllViews();
        View row = inflateRow(container, getString(R.string.search_near_me_title),
                getString(R.string.search_near_me_sub), R.drawable.ic_my_location);
        tvNearMeSub = row.findViewById(R.id.tv_subtitle);
        row.setOnClickListener(v -> {
            if (hasLocationPermission()) locate();
            else locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        });
        container.addView(row);
        loadProfileShortcuts(container);
    }

    /**
     * Lối tắt lấy từ hồ sơ: "Gần [trường của bạn]" và "[phường] · khu vực của bạn".
     * Hồ sơ chỉ là gợi ý, bấm vào mới tìm; hồ sơ chưa có thì không hiện gì.
     */
    private void loadProfileShortcuts(LinearLayout container) {
        api.getSearchDefaults().enqueue(new ApiCallback<PlaceModels.SearchDefaults>(this) {
            @Override
            public void onSuccess(PlaceModels.SearchDefaults data) {
                if (data == null || isFinishing()) return;
                if (data.school != null && data.school.latitude != null) {
                    SearchModels.Landmark school = data.school;
                    // Tên đầy đủ: tên viết tắt trên OSM ("ĐHKT") khó nhận ra
                    View row = inflateRow(container, getString(R.string.search_my_school_title, school.name),
                            getString(R.string.search_my_school_sub), R.drawable.ic_school);
                    row.setOnClickListener(v -> chooseSchool(school));
                    container.addView(row);
                }
                if (data.ward != null) {
                    PlaceModels.Ward ward = data.ward;
                    ward.provinceName = data.province != null ? data.province.name : "";
                    View row = inflateRow(container, ward.name,
                            getString(R.string.search_my_area_sub, ProvinceStore.shortName(ward.provinceName)),
                            R.drawable.ic_location);
                    row.setOnClickListener(v -> chooseWard(ward));
                    container.addView(row);
                }
            }

            @Override
            public void onError(String message) {
                // Chưa đăng nhập / chưa có hồ sơ: chỉ hiện "Phòng gần vị trí của tôi"
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Vị trí gần đây (dưới 5 phút) thì dùng luôn; không thì xin 1 lần định vị mới, chờ tối đa 15 giây.
     * Không dùng Google Play services để chạy được cả trên máy không có.
     */
    @SuppressLint("MissingPermission")
    private void locate() {
        if (locationListener != null) return;   // đang lấy vị trí
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location last = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (last == null || l.getTime() > last.getTime())) last = l;
        }
        if (last != null && System.currentTimeMillis() - last.getTime() < FRESH_LOCATION_MS) {
            searchNear(last);
            return;
        }
        String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER
                : lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null;
        if (provider == null) {
            if (last != null) searchNear(last);
            else AppNotificationHelper.showInfo(this, getString(R.string.search_near_me_failed));
            return;
        }
        Location fallback = last;
        tvNearMeSub.setText(R.string.search_near_me_locating);
        // Cài đủ các hàm: Android < 11 chưa có cài đặt mặc định cho onStatusChanged...
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                stopLocating();
                searchNear(location);
            }

            @Override
            public void onStatusChanged(String p, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String p) {}

            @Override
            public void onProviderDisabled(String p) {}
        };
        lm.requestLocationUpdates(provider, 0, 0, locationListener, Looper.getMainLooper());
        locateTimeout = () -> {
            stopLocating();
            if (fallback != null) searchNear(fallback);
            else AppNotificationHelper.showInfo(this, getString(R.string.search_near_me_failed));
        };
        handler.postDelayed(locateTimeout, LOCATE_TIMEOUT_MS);
    }

    private void stopLocating() {
        if (locateTimeout != null) handler.removeCallbacks(locateTimeout);
        locateTimeout = null;
        if (locationListener != null) {
            ((LocationManager) getSystemService(LOCATION_SERVICE)).removeUpdates(locationListener);
            locationListener = null;
        }
        if (tvNearMeSub != null) tvNearMeSub.setText(R.string.search_near_me_sub);
    }

    /** Tìm phòng quanh vị trí hiện tại; không lưu vào tìm kiếm gần đây vì vị trí thay đổi */
    private void searchNear(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        if (lat < VN_MIN_LAT || lat > VN_MAX_LAT || lng < VN_MIN_LNG || lng > VN_MAX_LNG) {
            AppNotificationHelper.showInfo(this, getString(R.string.search_near_me_outside));
            return;
        }
        criteria.clearLocation();
        criteria.nearMe = true;
        criteria.placeName = getString(R.string.search_near_me_label);
        criteria.centerLat = lat;
        criteria.centerLng = lng;
        criteria.sort = SearchCriteria.SORT_DISTANCE;
        finishWithCriteria();
    }

    // ---------- Đang gõ: gợi ý gộp phường/xã, trường học, địa chỉ ----------

    /** Phường/xã (trong tỉnh đang chọn) và địa chỉ cụ thể (Photon, ưu tiên gần tỉnh đang chọn) */
    private void loadPlaces(String q) {
        if (placesCall != null) placesCall.cancel();
        layoutTypedWards.removeAllViews();
        layoutTypedAddresses.removeAllViews();
        layoutTypedAddresses.addView(sectionNote(getString(R.string.places_searching)));
        Call<ApiResponse<PlaceModels.Suggest>> call = api.suggestPlaces(q, ProvinceStore.code(this));
        placesCall = call;
        call.enqueue(new ApiCallback<PlaceModels.Suggest>(this) {
            @Override
            public void onSuccess(PlaceModels.Suggest data) {
                layoutTypedWards.removeAllViews();
                layoutTypedAddresses.removeAllViews();
                if (data == null) return;
                if (!data.wards.isEmpty()) {
                    layoutTypedWards.addView(sectionTitle(getString(R.string.places_wards_title)));
                    for (PlaceModels.Ward w : data.wards) {
                        View row = inflateRow(layoutTypedWards, w.name,
                                getString(R.string.places_ward_sub, w.provinceName, w.roomCount), R.drawable.ic_location);
                        row.setOnClickListener(v -> chooseWard(w));
                        layoutTypedWards.addView(row);
                    }
                }
                if (!data.addresses.isEmpty()) {
                    layoutTypedAddresses.addView(sectionTitle(getString(R.string.places_addresses_title)));
                    for (PlaceModels.Address a : data.addresses) {
                        View row = inflateRow(layoutTypedAddresses, a.title, a.subtitle, R.drawable.ic_location_on);
                        row.setOnClickListener(v -> choosePlace(a));
                        layoutTypedAddresses.addView(row);
                    }
                    layoutTypedAddresses.addView(sectionNote(getString(R.string.places_source)));
                } else if (data.wards.isEmpty() && layoutTypedSchools.getChildCount() == 0) {
                    layoutTypedAddresses.addView(sectionNote(getString(R.string.places_empty)));
                }
            }

            @Override
            public void onError(String message) {
                layoutTypedAddresses.removeAllViews();
                layoutTypedAddresses.addView(sectionNote(message));
            }
        });
    }

    /** Trường khớp chữ đang gõ (cả nước, trường trong tỉnh đang chọn lên trước) */
    private void loadTypedSchools(String q) {
        if (typedSchoolsCall != null) typedSchoolsCall.cancel();
        layoutTypedSchools.removeAllViews();
        Call<ApiResponse<List<SearchModels.Landmark>>> call =
                api.suggestLandmarks(q, "university", 4, ProvinceStore.code(this));
        typedSchoolsCall = call;
        call.enqueue(new ApiCallback<List<SearchModels.Landmark>>(this) {
            @Override
            public void onSuccess(List<SearchModels.Landmark> schools) {
                layoutTypedSchools.removeAllViews();
                if (schools == null || schools.isEmpty()) return;
                layoutTypedSchools.addView(sectionTitle(getString(R.string.places_schools_title)));
                for (SearchModels.Landmark school : schools) {
                    View row = inflateRow(layoutTypedSchools, school.name, schoolSubtitle(school), R.drawable.ic_school);
                    row.setOnClickListener(v -> chooseSchool(school));
                    layoutTypedSchools.addView(row);
                }
            }

            @Override
            public void onError(String message) {
                layoutTypedSchools.removeAllViews();
            }
        });
    }

    /** "Khu vực được quan tâm" = 4 phường có nhiều phòng nhất trong tỉnh đang chọn; không tải được thì ẩn cả mục */
    private void loadTopWards() {
        api.getWards(ProvinceStore.code(this), null, 4).enqueue(new ApiCallback<List<PlaceModels.Ward>>(this) {
            @Override
            public void onSuccess(List<PlaceModels.Ward> wards) {
                layoutPopular.removeAllViews();
                if (wards == null || wards.isEmpty()) return;
                for (PlaceModels.Ward w : wards) {
                    TextView chip = (TextView) LayoutInflater.from(SearchActivity.this)
                            .inflate(R.layout.item_area_chip, layoutPopular, false);
                    chip.setText(w.name.replaceFirst("^(Phường|Xã|Đặc khu) ", ""));
                    chip.setOnClickListener(v -> chooseWard(w));
                    layoutPopular.addView(chip);
                }
                findViewById(R.id.tv_popular_title).setVisibility(View.VISIBLE);
                findViewById(R.id.scroll_popular).setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                // Ẩn mục, các lối tắt khác vẫn dùng được
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
                api.suggestLandmarks(q == null || q.isEmpty() ? null : q, "university", 5, ProvinceStore.code(this));
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
                    View row = inflateRow(layoutSchools, school.name, schoolSubtitle(school), R.drawable.ic_school);
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

    /** "268 Lý Thường Kiệt · 3 phòng trong 3km" (bỏ phần địa chỉ nếu OSM không có) */
    private String schoolSubtitle(SearchModels.Landmark school) {
        String rooms = getString(R.string.search_school_rooms, school.roomCount);
        return school.addressText != null ? school.addressText + " · " + rooms : rooms;
    }

    private View inflateRow(LinearLayout parent, String title, String subtitle, int iconRes) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_search_suggestion, parent, false);
        ((TextView) row.findViewById(R.id.tv_title)).setText(title);
        ((TextView) row.findViewById(R.id.tv_subtitle)).setText(subtitle);
        ((ImageView) row.findViewById(R.id.img_icon)).setImageResource(iconRes);
        return row;
    }
}
