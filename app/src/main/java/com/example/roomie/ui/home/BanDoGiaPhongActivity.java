package com.example.roomie.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.MapModels;
import com.example.roomie.data.discovery.PlaceModels;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.discovery.SearchModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.ApiResponse;
import com.example.roomie.ui.filter.BoLocActivity;
import com.example.roomie.ui.map.MapSupport;
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.room.ContactActions;
import com.example.roomie.ui.saved.SaveToggle;
import com.example.roomie.ui.search.ProvinceStore;
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchActivity;
import com.example.roomie.ui.search.SearchCriteria;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.RangeSlider;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import retrofit2.Call;

/**
 * Bản đồ giá phòng: nhãn giá đặt tại vị trí (gần đúng) của từng phòng trong vùng đang xem.
 * Kéo / phóng bản đồ thì tải lại; chạm nhãn để xem thẻ phòng. Nhận SearchCriteria từ Home.
 */
public class BanDoGiaPhongActivity extends AppCompatActivity {

    public static Intent intent(Context context, SearchCriteria criteria) {
        return new Intent(context, BanDoGiaPhongActivity.class).putExtra(SearchCriteria.EXTRA, criteria);
    }

    private static final double DEFAULT_ZOOM = 13.0;
    private static final long RELOAD_DELAY_MS = 400;
    private static final int HIGH_MATCH = 80;
    /** Bán kính mặc định của server khi không gửi radiusKm, và mức mở rộng tối đa */
    private static final int DEFAULT_RADIUS_KM = 3;
    private static final int MAX_RADIUS_KM = 10;

    private DiscoveryApi api;
    private SearchCriteria criteria = new SearchCriteria();
    private boolean onlyHighMatch = false;

    private MapView map;
    private MyLocationNewOverlay myLocation;
    private final List<Marker> roomMarkers = new ArrayList<>();
    private SearchModels.RoomCard selected;
    private Marker searchPin;
    private Marker selectedMarker;
    private RoomModels.Contact selectedContact;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reloadRunnable = this::loadRooms;
    private Call<?> pendingCall;

    private View cardSelected;
    private TextView tvStatus;
    private TextView chipCount;
    private TextView chipPrice;
    private TextView chipMatch;
    private TextView chipMoreFilters;
    private View btnMyLocation;

    private final ActivityResultLauncher<Intent> criteriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    SearchCriteria c = (SearchCriteria) result.getData().getSerializableExtra(SearchCriteria.EXTRA);
                    if (c != null) {
                        criteria = c;
                        bindHeader();
                        hideCard();
                        firstLoad = true;
                        moveToSearchLocation();
                        scheduleReload(0);
                    }
                }
            });

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (hasLocationPermission()) showMyLocation();
                else AppNotificationHelper.showInfo(this, getString(R.string.map_location_denied));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapSupport.init(this);   // phải gọi trước khi tạo MapView
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ban_do_gia_phong);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);
        SearchCriteria given = (SearchCriteria) getIntent().getSerializableExtra(SearchCriteria.EXTRA);
        if (given != null) criteria = given.copy();

        cardSelected = findViewById(R.id.card_selected_room);
        tvStatus = findViewById(R.id.tv_map_status);
        chipCount = findViewById(R.id.chip_room_count);
        chipPrice = findViewById(R.id.chip_price_range);
        chipMatch = findViewById(R.id.chip_match_rate);
        chipMoreFilters = findViewById(R.id.chip_more_filters);
        btnMyLocation = findViewById(R.id.btn_my_location);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.layout_search_bar).setOnClickListener(v -> criteriaLauncher.launch(
                new Intent(this, SearchActivity.class).putExtra(SearchCriteria.EXTRA, criteria)));
        View.OnClickListener openFilter = v -> criteriaLauncher.launch(
                new Intent(this, BoLocActivity.class).putExtra(SearchCriteria.EXTRA, criteria));
        findViewById(R.id.btn_filter).setOnClickListener(openFilter);
        chipMoreFilters.setOnClickListener(openFilter);
        chipPrice.setOnClickListener(v -> showPricePicker());
        chipMatch.setOnClickListener(v -> {
            onlyHighMatch = !onlyHighMatch;
            bindHeader();
            hideCard();
            scheduleReload(0);
        });
        btnMyLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) showMyLocation();
            else locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        });

        // Thẻ phòng đang chọn
        View.OnClickListener openDetail = v -> {
            if (selected != null) startActivity(ChiTietPhongActivity.intent(this, selected.id));
        };
        findViewById(R.id.btn_room_detail).setOnClickListener(openDetail);
        findViewById(R.id.layout_room_info).setOnClickListener(openDetail);
        ImageView heart = findViewById(R.id.btn_heart);
        heart.setOnClickListener(v -> {
            if (selected != null) {
                SaveToggle.toggle(this, selected.id, selected.isSaved, heart, saved -> selected.isSaved = saved);
            }
        });
        findViewById(R.id.btn_contact_zalo).setOnClickListener(v -> withContact(
                contact -> ContactActions.zalo(this, selected.id, contact)));
        findViewById(R.id.btn_call_phone).setOnClickListener(v -> withContact(
                contact -> ContactActions.call(this, selected.id, contact)));

        setupMap();
        bindHeader();
    }

    /** Đóng bản đồ (nút quay lại, cử chỉ back) thì trả điều kiện hiện tại về Home */
    @Override
    public void finish() {
        setResult(RESULT_OK, new Intent().putExtra(SearchCriteria.EXTRA, criteria));
        super.finish();
    }

    /** Dòng trạng thái dưới bản đồ: thường là nhãn trắng; có hành động (mở rộng bán kính) thì nút màu đỏ đậm */
    private void styleStatus(View.OnClickListener action) {
        styleChip(tvStatus, action != null);
        tvStatus.setOnClickListener(action);
        tvStatus.setClickable(action != null);
    }

    // ---------- Chọn giá nhanh ----------

    private static final float PRICE_MAX = 10_000_000f;
    /** Mức giá gợi ý {min, max} (đồng); null = không giới hạn đầu đó */
    private static final long[][] PRICE_PRESETS = {
            {0, 2_000_000}, {2_000_000, 3_000_000}, {3_000_000, 4_000_000}, {4_000_000, 5_000_000}, {5_000_000, 0}};

    private void showPricePicker() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.dialog_price_picker, null, false);
        sheet.setContentView(content);
        TextView tvValue = content.findViewById(R.id.tv_price_value);
        RangeSlider slider = content.findViewById(R.id.range_price);
        slider.setTickVisible(false);
        LinearLayout presets = content.findViewById(R.id.group_price_presets);
        // Giá đang chọn trong hộp, chỉ áp dụng khi bấm "Xem phòng"
        Long[] picked = {criteria.minRent, criteria.maxRent};

        Runnable bind = () -> {
            slider.setValues(picked[0] != null ? (float) picked[0] : 0f, picked[1] != null ? (float) picked[1] : PRICE_MAX);
            tvValue.setText(RoomFormat.budgetLabel(this, picked[0], picked[1]));
            for (int i = 0; i < presets.getChildCount(); i++) {
                long[] p = PRICE_PRESETS[i];
                boolean on = Objects.equals(picked[0], p[0] == 0 ? null : p[0])
                        && Objects.equals(picked[1], p[1] == 0 ? null : p[1]);
                styleChip((TextView) presets.getChildAt(i), on);
            }
        };
        for (long[] p : PRICE_PRESETS) {
            TextView chip = new TextView(this);
            String min = RoomFormat.millions(p[0]);
            String max = RoomFormat.millions(p[1]);
            chip.setText(p[0] == 0 ? getString(R.string.map_price_under, max)
                    : p[1] == 0 ? getString(R.string.map_price_over, min)
                    : getString(R.string.map_price_between, min, max));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Math.round(8 * getResources().getDisplayMetrics().density));
            chip.setOnClickListener(v -> {
                picked[0] = p[0] == 0 ? null : p[0];
                picked[1] = p[1] == 0 ? null : p[1];
                bind.run();
            });
            presets.addView(chip, lp);
        }
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (!fromUser) return;
            List<Float> v = s.getValues();
            picked[0] = v.get(0) <= 0 ? null : (long) (float) v.get(0);
            picked[1] = v.get(1) >= PRICE_MAX ? null : (long) (float) v.get(1);
            bind.run();
        });
        content.findViewById(R.id.btn_price_any).setOnClickListener(v -> {
            applyPrice(null, null);
            sheet.dismiss();
        });
        content.findViewById(R.id.btn_price_apply).setOnClickListener(v -> {
            applyPrice(picked[0], picked[1]);
            sheet.dismiss();
        });
        bind.run();
        sheet.show();
    }

    private void applyPrice(Long min, Long max) {
        if (Objects.equals(min, criteria.minRent) && Objects.equals(max, criteria.maxRent)) return;
        criteria.minRent = min;
        criteria.maxRent = max;
        bindHeader();
        hideCard();
        scheduleReload(0);
    }

    // ---------- Bản đồ ----------

    private void setupMap() {
        map = findViewById(R.id.map_view);
        MapSupport.setupMap(map);
        map.getOverlays().add(new CopyrightOverlay(this));
        // Chạm vào chỗ trống trên bản đồ thì ẩn thẻ phòng
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                hideCard();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                scheduleReload(RELOAD_DELAY_MS);
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                scheduleReload(RELOAD_DELAY_MS);
                return false;
            }
        });
        map.getController().setZoom(DEFAULT_ZOOM);
        // Mở ở tâm tỉnh/thành đang chọn
        PlaceModels.Province province = ProvinceStore.get(this);
        map.getController().setCenter(new GeoPoint(province.latitude, province.longitude));
        // Có địa chỉ / trường / phường đang tìm thì nhảy tới đó
        moveToSearchLocation();
        scheduleReload(0);
    }

    /**
     * Đưa bản đồ tới vị trí đang tìm và cắm ghim "Vị trí bạn tìm".
     * Phường/xã chưa có tọa độ thì hỏi backend tâm phường (tra Photon, có cache).
     */
    /**
     * Thu phóng vừa đủ cả vòng bán kính quanh tâm (bản đồ chỉ tải phòng trong vùng đang xem,
     * nên vùng nhìn phải chứa hết vòng tìm kiếm). Gọi sau khi MapView đã đo xong kích thước.
     */
    private void fitRadius(GeoPoint center) {
        double radius = criteria.radiusKm != null ? criteria.radiusKm : DEFAULT_RADIUS_KM;
        double dLat = radius / 111.0;
        double dLng = radius / (111.0 * Math.cos(Math.toRadians(center.getLatitude())));
        BoundingBox box = new BoundingBox(center.getLatitude() + dLat, center.getLongitude() + dLng,
                center.getLatitude() - dLat, center.getLongitude() - dLng);
        map.post(() -> {
            map.zoomToBoundingBox(box, false, Math.round(16 * getResources().getDisplayMetrics().density));
            scheduleReload(0);
        });
    }

    private void moveToSearchLocation() {
        if (searchPin != null) {
            map.getOverlays().remove(searchPin);
            searchPin = null;
        }
        if (criteria.hasCenter()) {
            GeoPoint center = new GeoPoint(criteria.centerLat, criteria.centerLng);
            map.getController().setCenter(center);
            // Quanh địa chỉ / trường / vị trí: thấy hết vòng bán kính; phường: phóng gần
            if (criteria.hasDistanceOrigin()) fitRadius(center);
            else map.getController().setZoom(15.0);
            searchPin = new Marker(map);
            searchPin.setPosition(center);
            searchPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            searchPin.setIcon(MapSupport.searchPin(this));
            searchPin.setTitle(getString(R.string.map_search_pin));
            searchPin.setInfoWindow(null);
            // Ghim nằm dưới nhãn giá để vẫn chạm được nhãn
            map.getOverlays().add(Math.min(1, map.getOverlays().size()), searchPin);
            map.invalidate();
        } else if (criteria.wardCode != null) {
            String wardCode = criteria.wardCode;
            api.getWardCenter(wardCode).enqueue(new ApiCallback<PlaceModels.WardCenter>(this) {
                @Override
                public void onSuccess(PlaceModels.WardCenter data) {
                    if (data == null || data.latitude == null || !wardCode.equals(criteria.wardCode)) return;
                    criteria.centerLat = data.latitude;
                    criteria.centerLng = data.longitude;
                    moveToSearchLocation();
                    scheduleReload(0);
                }

                @Override
                public void onError(String message) {
                    // Không tra được tâm phường: giữ vị trí hiện tại, vẫn lọc đúng phường
                }
            });
        }
    }

    /** Chờ người dùng ngừng kéo rồi mới gọi API, tránh gọi liên tục */
    private void scheduleReload(long delay) {
        handler.removeCallbacks(reloadRunnable);
        handler.postDelayed(reloadRunnable, delay);
    }

    private boolean firstLoad = true;

    private void loadRooms() {
        if (map.getWidth() == 0) {   // MapView chưa đo xong kích thước
            scheduleReload(100);
            return;
        }
        BoundingBox box = map.getBoundingBox();
        Map<String, String> query = criteria.toQuery(1, 1);
        query.remove("page");
        query.remove("limit");
        // Tìm gần trường nhưng chưa biết tọa độ trường (mục tìm gần đây cũ): lần đầu lấy cả thành phố,
        // backend đã giới hạn bán kính quanh trường, rồi đặt tâm vào phòng gần nhất
        if (!(firstLoad && criteria.landmarkId != null && !criteria.hasCenter())) {
            query.put("minLat", String.valueOf(box.getLatSouth()));
            query.put("maxLat", String.valueOf(box.getLatNorth()));
            query.put("minLng", String.valueOf(box.getLonWest()));
            query.put("maxLng", String.valueOf(box.getLonEast()));
        } else {
            query.put("minLat", "-90");
            query.put("maxLat", "90");
            query.put("minLng", "-180");
            query.put("maxLng", "180");
        }
        if (onlyHighMatch) query.put("minMatch", String.valueOf(HIGH_MATCH));

        if (pendingCall != null) pendingCall.cancel();
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.map_loading);
        Call<ApiResponse<MapModels.MapRooms>> call = api.getMapRooms(query);
        pendingCall = call;
        call.enqueue(new ApiCallback<MapModels.MapRooms>(this) {
            @Override
            public void onSuccess(MapModels.MapRooms data) {
                if (data == null) return;
                boolean centerOnResults = firstLoad && criteria.landmarkId != null && !criteria.hasCenter()
                        && !data.items.isEmpty();
                firstLoad = false;
                showRooms(data.items);
                chipCount.setText(getString(R.string.map_room_count, data.items.size()));
                styleStatus(null);
                int radius = criteria.radiusKm != null ? criteria.radiusKm : DEFAULT_RADIUS_KM;
                if (data.truncated) {
                    tvStatus.setText(getString(R.string.map_zoom_in, data.total));
                } else if (data.items.isEmpty() && criteria.hasDistanceOrigin() && radius < MAX_RADIUS_KM) {
                    // Tìm quanh 1 điểm mà trống: bấm để mở rộng bán kính (3 -> 5 -> 10 km)
                    int wider = radius < 5 ? 5 : MAX_RADIUS_KM;
                    tvStatus.setText(getString(R.string.map_empty_widen, radius, wider));
                    styleStatus(v -> {
                        criteria.radiusKm = wider;
                        hideCard();
                        moveToSearchLocation();
                        scheduleReload(0);
                    });
                } else if (data.items.isEmpty()) {
                    tvStatus.setText(onlyHighMatch ? R.string.map_need_quiz : R.string.map_empty);
                } else {
                    tvStatus.setVisibility(View.GONE);
                }
                if (centerOnResults) {
                    SearchModels.RoomCard first = data.items.get(0);
                    if (first.latitude != null) {
                        map.getController().setCenter(new GeoPoint(first.latitude, first.longitude));
                    }
                }
            }

            @Override
            public void onError(String message) {
                tvStatus.setVisibility(View.VISIBLE);
                styleStatus(null);
                tvStatus.setText(message);
            }
        });
    }

    private void showRooms(List<SearchModels.RoomCard> rooms) {
        map.getOverlays().removeAll(roomMarkers);
        roomMarkers.clear();
        selectedMarker = null;
        for (SearchModels.RoomCard room : rooms) {
            if (room.latitude == null || room.longitude == null) continue;
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(room.latitude, room.longitude));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setRelatedObject(room);
            boolean isSelected = selected != null && selected.id.equals(room.id);
            marker.setIcon(priceIcon(room, isSelected));
            marker.setOnMarkerClickListener((m, mapView) -> {
                selectRoom(m);
                return true;
            });
            if (isSelected) selectedMarker = marker;
            roomMarkers.add(marker);
        }
        map.getOverlays().addAll(roomMarkers);
        map.invalidate();
    }

    // Phòng đang chọn hoặc phù hợp ≥ 80% có nhãn nền đỏ đậm
    private Drawable priceIcon(SearchModels.RoomCard room, boolean isSelected) {
        boolean highlight = isSelected || (room.matchPercent != null && room.matchPercent >= HIGH_MATCH);
        return MapSupport.priceLabel(this, getString(R.string.map_price_label, RoomFormat.millions(room.monthlyRent)), highlight);
    }

    // ---------- Thẻ phòng ----------

    private void selectRoom(Marker marker) {
        if (selectedMarker != null && selectedMarker != marker) {
            selectedMarker.setIcon(priceIcon((SearchModels.RoomCard) selectedMarker.getRelatedObject(), false));
        }
        selectedMarker = marker;
        selected = (SearchModels.RoomCard) marker.getRelatedObject();
        selectedContact = null;
        marker.setIcon(priceIcon(selected, true));
        map.invalidate();

        ImageLoader.load(findViewById(R.id.img_room_thumb), selected.coverImageUrl, R.drawable.img_room1);
        TextView match = findViewById(R.id.tv_sel_match);
        match.setVisibility(selected.matchPercent != null ? View.VISIBLE : View.GONE);
        if (selected.matchPercent != null) match.setText(getString(R.string.feed_match_badge, selected.matchPercent));
        ((TextView) findViewById(R.id.tv_sel_title)).setText(selected.title);
        ((TextView) findViewById(R.id.tv_sel_address)).setText(selected.addressText);
        ((TextView) findViewById(R.id.tv_sel_price))
                .setText(getString(R.string.feed_price, RoomFormat.money(selected.monthlyRent)));
        SaveToggle.bindIcon(findViewById(R.id.btn_heart), selected.isSaved);

        cardSelected.setVisibility(View.VISIBLE);
        // Đẩy nút "vị trí của tôi" lên trên thẻ phòng
        cardSelected.post(() -> btnMyLocation.setTranslationY(-cardSelected.getHeight()));
    }

    private void hideCard() {
        if (selectedMarker != null && selected != null) {
            selectedMarker.setIcon(priceIcon(selected, false));
            map.invalidate();
        }
        selected = null;
        selectedMarker = null;
        cardSelected.setVisibility(View.GONE);
        btnMyLocation.setTranslationY(0);
    }

    /** Số điện thoại chỉ có ở API chi tiết (theo quyền riêng tư), tải khi bấm liên hệ */
    private void withContact(Consumer<RoomModels.Contact> action) {
        if (selected == null) return;
        if (selectedContact != null) {
            action.accept(selectedContact);
            return;
        }
        String postId = selected.id;
        api.getRoomDetail(postId).enqueue(new ApiCallback<RoomModels.RoomDetail>(this) {
            @Override
            public void onSuccess(RoomModels.RoomDetail data) {
                if (data == null || selected == null || !postId.equals(selected.id)) return;
                selectedContact = data.contact;
                action.accept(selectedContact);
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(BanDoGiaPhongActivity.this, message);
            }
        });
    }

    // ---------- Thanh trên ----------

    private void bindHeader() {
        String location = criteria.locationLabel();
        TextView tvSearch = findViewById(R.id.tv_map_search_text);
        tvSearch.setText(location != null ? location : getString(R.string.hint_search));
        chipPrice.setText(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent));
        styleChip(chipPrice, criteria.minRent != null || criteria.maxRent != null);
        styleChip(chipMatch, onlyHighMatch);
        int others = criteria.otherFilterCount();
        chipMoreFilters.setVisibility(others > 0 ? View.VISIBLE : View.GONE);
        chipMoreFilters.setText(getString(R.string.map_chip_more_filters, others));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(getColor(selected ? R.color.white : R.color.text_primary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        float d = getResources().getDisplayMetrics().density;
        chip.setPadding((int) (14 * d), (int) (7 * d), (int) (14 * d), (int) (7 * d));
    }

    // ---------- Vị trí của tôi ----------

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void showMyLocation() {
        if (myLocation == null) {
            myLocation = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            map.getOverlays().add(myLocation);
        }
        myLocation.enableMyLocation();
        GeoPoint here = myLocation.getMyLocation();
        // Nút này chỉ đưa bản đồ về chỗ bạn đứng; phòng trong vùng nhìn sẽ tự tải lại
        if (here != null) {
            map.getController().animateTo(here);
            AppNotificationHelper.showInfo(this, getString(R.string.map_my_location_hint));
        } else {
            AppNotificationHelper.showInfo(this, getString(R.string.map_locating));
            myLocation.runOnFirstFix(() -> runOnUiThread(() -> {
                GeoPoint p = myLocation.getMyLocation();
                if (p != null) map.getController().animateTo(p);
            }));
        }
    }

    // ---------- Vòng đời MapView ----------

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (myLocation != null && hasLocationPermission()) myLocation.enableMyLocation();
    }

    @Override
    protected void onPause() {
        map.onPause();
        if (myLocation != null) myLocation.disableMyLocation();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(reloadRunnable);
        if (pendingCall != null) pendingCall.cancel();
        super.onDestroy();
    }

}
