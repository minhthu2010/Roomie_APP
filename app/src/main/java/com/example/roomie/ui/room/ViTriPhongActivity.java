package com.example.roomie.ui.room;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.MapModels;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.map.MapSupport;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Vị trí phòng: vùng gần đúng của phòng trên bản đồ (OpenStreetMap) và các địa điểm lân cận
 * (trường, siêu thị, trạm xe buýt, bệnh viện) kèm khoảng cách. Extra bắt buộc: EXTRA_POST_ID.
 */
public class ViTriPhongActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";
    /** Danh sách dưới bản đồ chỉ hiện vài địa điểm gần nhất; bản đồ hiện tất cả */
    private static final int LIST_LIMIT = 4;
    /** Bán kính vòng "khu vực gần đúng" khớp với độ lệch tọa độ ở backend */
    private static final double APPROX_RADIUS_M = 150;

    public static Intent intent(Context context, String postId) {
        return new Intent(context, ViTriPhongActivity.class).putExtra(EXTRA_POST_ID, postId);
    }

    private String postId;
    private MapView map;
    private MapModels.Nearby nearby;
    private RoomModels.Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapSupport.init(this);   // phải gọi trước khi tạo MapView
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vi_tri_phong);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_directions).setOnClickListener(v -> openDirections());
        findViewById(R.id.btn_contact_zalo).setOnClickListener(v -> withContact(c -> ContactActions.zalo(this, postId, c)));
        findViewById(R.id.btn_call_phone).setOnClickListener(v -> withContact(c -> ContactActions.call(this, postId, c)));

        map = findViewById(R.id.map_view);
        MapSupport.setupMap(map);
        map.getOverlays().add(new CopyrightOverlay(this));
        map.getController().setZoom(16.0);

        if (postId == null) {
            finish();
            return;
        }
        load();
    }

    private void load() {
        ApiClient.create(this, DiscoveryApi.class).getNearby(postId)
                .enqueue(new ApiCallback<MapModels.Nearby>(this) {
                    @Override
                    public void onSuccess(MapModels.Nearby data) {
                        if (data == null) return;
                        nearby = data;
                        render();
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(ViTriPhongActivity.this, message);
                    }
                });
    }

    private void render() {
        ((TextView) findViewById(R.id.tv_loc_address)).setText(nearby.addressText);
        ((TextView) findViewById(R.id.tv_loc_sub)).setText(nearby.areaText);

        if (nearby.latitude == null || nearby.longitude == null) {
            ((TextView) findViewById(R.id.tv_loc_note)).setText(R.string.loc_no_location);
            findViewById(R.id.btn_directions).setVisibility(View.GONE);
            return;
        }
        GeoPoint room = new GeoPoint(nearby.latitude, nearby.longitude);

        // Địa điểm lân cận: nhãn trên bản đồ + danh sách
        LinearLayout list = findViewById(R.id.layout_nearby);
        list.removeAllViews();
        for (int i = 0; i < nearby.places.size(); i++) {
            MapModels.Place place = nearby.places.get(i);
            GeoPoint point = new GeoPoint(place.latitude, place.longitude);
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setIcon(MapSupport.placeLabel(this, MapSupport.placeIcon(place.type), place.name));
            marker.setInfoWindow(null);
            marker.setOnMarkerClickListener((m, mv) -> {
                mv.getController().animateTo(m.getPosition());
                return true;
            });
            map.getOverlays().add(marker);

            if (i < LIST_LIMIT) {
                if (i > 0) list.addView(divider());
                list.addView(placeRow(place, point));
            }
        }
        if (nearby.places.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.loc_nearby_empty);
            empty.setTextColor(getColor(R.color.color_text_secondary));
            empty.setPadding(0, 16, 0, 8);
            list.addView(empty);
        }

        // Vòng tròn khu vực gần đúng + ghim phòng: thêm sau cùng để nằm trên nhãn địa điểm
        Polygon area = new Polygon(map);
        area.setPoints(Polygon.pointsAsCircle(room, APPROX_RADIUS_M));
        int maroon = ContextCompat.getColor(this, R.color.primary_maroon);
        area.getFillPaint().setColor(Color.argb(40, Color.red(maroon), Color.green(maroon), Color.blue(maroon)));
        area.getOutlinePaint().setColor(maroon);
        area.getOutlinePaint().setStrokeWidth(3f);
        area.setInfoWindow(null);
        map.getOverlays().add(area);

        Marker roomMarker = new Marker(map);
        roomMarker.setPosition(room);
        roomMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        roomMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_marker));
        roomMarker.setInfoWindow(null);
        map.getOverlays().add(roomMarker);

        map.getController().setCenter(room);
        map.invalidate();
    }

    private View placeRow(MapModels.Place place, GeoPoint point) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_nearby_place, null, false);
        ((ImageView) row.findViewById(R.id.img_place_icon)).setImageResource(MapSupport.placeIcon(place.type));
        ((TextView) row.findViewById(R.id.tv_place_name)).setText(place.name);
        TextView sub = row.findViewById(R.id.tv_place_sub);
        sub.setText(place.subtitle);
        sub.setVisibility(place.subtitle != null ? View.VISIBLE : View.GONE);
        ((TextView) row.findViewById(R.id.tv_place_distance)).setText(MapSupport.distanceText(place.distanceKm));
        // Chạm vào dòng thì đưa bản đồ tới địa điểm đó
        row.setOnClickListener(v -> map.getController().animateTo(point));
        return row;
    }

    private View divider() {
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(getColor(R.color.color_border_light));
        return line;
    }

    /** Mở Google Maps (app hoặc trình duyệt) chỉ đường tới vị trí gần đúng của phòng */
    private void openDirections() {
        if (nearby == null || nearby.latitude == null) return;
        String dest = String.format(Locale.US, "%.6f,%.6f", nearby.latitude, nearby.longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + dest));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppNotificationHelper.showError(this, getString(R.string.contact_no_app));
        }
    }

    /** Số điện thoại chỉ có ở API chi tiết phòng, tải khi bấm liên hệ */
    private void withContact(Consumer<RoomModels.Contact> action) {
        if (contact != null) {
            action.accept(contact);
            return;
        }
        ApiClient.create(this, DiscoveryApi.class).getRoomDetail(postId)
                .enqueue(new ApiCallback<RoomModels.RoomDetail>(this) {
                    @Override
                    public void onSuccess(RoomModels.RoomDetail data) {
                        if (data == null) return;
                        contact = data.contact;
                        action.accept(contact);
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(ViTriPhongActivity.this, message);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }
}
