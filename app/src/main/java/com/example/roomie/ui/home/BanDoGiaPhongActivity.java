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
import com.example.roomie.ui.search.RoomFormat;
import com.example.roomie.ui.search.SearchActivity;
import com.example.roomie.ui.search.SearchCriteria;

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

    private static final GeoPoint HCMC_CENTER = new GeoPoint(10.7790, 106.6850);
    private static final double DEFAULT_ZOOM = 13.0;
    private static final long RELOAD_DELAY_MS = 400;
    private static final int HIGH_MATCH = 80;

    private DiscoveryApi api;
    private SearchCriteria criteria = new SearchCriteria();
    private boolean onlyHighMatch = false;

    private MapView map;
    private MyLocationNewOverlay myLocation;
    private final List<Marker> roomMarkers = new ArrayList<>();
    private SearchModels.RoomCard selected;
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
    private View btnMyLocation;

    private final ActivityResultLauncher<Intent> criteriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    SearchCriteria c = (SearchCriteria) result.getData().getSerializableExtra(SearchCriteria.EXTRA);
                    if (c != null) {
                        criteria = c;
                        bindHeader();
                        hideCard();
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
        btnMyLocation = findViewById(R.id.btn_my_location);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.layout_search_bar).setOnClickListener(v -> criteriaLauncher.launch(
                new Intent(this, SearchActivity.class).putExtra(SearchCriteria.EXTRA, criteria)));
        View.OnClickListener openFilter = v -> criteriaLauncher.launch(
                new Intent(this, BoLocActivity.class).putExtra(SearchCriteria.EXTRA, criteria));
        findViewById(R.id.btn_filter).setOnClickListener(openFilter);
        chipPrice.setOnClickListener(openFilter);
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
        map.getController().setCenter(HCMC_CENTER);
        // Đang tìm gần trường: lần tải đầu lấy cả thành phố (backend đã giới hạn bán kính quanh trường)
        // rồi đặt tâm bản đồ vào phòng gần trường nhất
        scheduleReload(0);
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
        // Tìm gần trường thì để cả thành phố, bán kính do backend giới hạn
        if (!(firstLoad && criteria.landmarkId != null)) {
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
                boolean centerOnResults = firstLoad && criteria.landmarkId != null && !data.items.isEmpty();
                firstLoad = false;
                showRooms(data.items);
                chipCount.setText(getString(R.string.map_room_count, data.items.size()));
                if (data.truncated) {
                    tvStatus.setText(getString(R.string.map_zoom_in, data.total));
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
        String location = criteria.landmarkName != null ? criteria.landmarkName
                : criteria.area != null ? criteria.area : criteria.query;
        TextView tvSearch = findViewById(R.id.tv_map_search_text);
        tvSearch.setText(location != null ? location : getString(R.string.hint_search));
        chipPrice.setText(RoomFormat.budgetLabel(this, criteria.minRent, criteria.maxRent));
        styleChip(chipPrice, criteria.minRent != null || criteria.maxRent != null);
        styleChip(chipMatch, onlyHighMatch);
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
        if (here != null) {
            map.getController().animateTo(here);
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
