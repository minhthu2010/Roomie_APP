package com.example.roomie.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.roomie.BuildConfig;
import com.example.roomie.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView;

import java.io.File;

/** Cấu hình osmdroid và vẽ marker dạng nhãn (giá phòng, tên địa điểm). */
public final class MapSupport {

    private MapSupport() {}

    /**
     * Gọi trước setContentView. OpenStreetMap yêu cầu user-agent riêng của app;
     * cache ô bản đồ để trong bộ nhớ của app nên không cần quyền bộ nhớ.
     */
    public static void init(Context context) {
        IConfigurationProvider config = Configuration.getInstance();
        config.setUserAgentValue(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);
        File base = new File(context.getCacheDir(), "osmdroid");
        config.setOsmdroidBasePath(base);
        config.setOsmdroidTileCache(new File(base, "tiles"));
    }

    /**
     * Ô bản đồ OpenStreetMap từ máy chủ của cộng đồng OSM Đức (cùng dữ liệu, tên đường tiếng Việt,
     * không cần API key; phải gửi user-agent riêng và ghi nguồn).
     * Không dùng tile.openstreetmap.org (TileSourceFactory.MAPNIK) vì một số nhà mạng ở VN chặn
     * tên miền này (DNS trả 127.0.0.1) làm bản đồ trắng. CARTO cũng không dùng được vì cần API key.
     */
    public static final OnlineTileSourceBase TILE_SOURCE = new XYTileSource(
            "OsmDe", 1, 19, 256, ".png",
            new String[]{"https://tile.openstreetmap.de/"},
            "© OpenStreetMap contributors");

    public static void setupMap(MapView map) {
        map.setTileSource(TILE_SOURCE);
        // Phóng ô theo mật độ màn hình, không thì chữ trên bản đồ rất nhỏ ở máy độ phân giải cao
        map.setTilesScaledToDpi(true);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(5.0);
        map.setMaxZoomLevel(19.0);
        // Ẩn nút +/- mặc định, dùng cử chỉ 2 ngón
        map.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
    }

    /** Nhãn giá "3,2 tr": nổi bật (nền đỏ đậm) khi đang chọn hoặc phù hợp cao */
    public static Drawable priceLabel(Context ctx, String text, boolean highlighted) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(ctx, highlighted ? R.color.white : R.color.text_primary));
        tv.setBackgroundResource(highlighted ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        int h = dp(ctx, 12);
        int v = dp(ctx, 5);
        tv.setPadding(h, v, h, v);
        return toDrawable(ctx, tv);
    }

    /** Nhãn địa điểm: icon + tên ngắn */
    public static Drawable placeLabel(Context ctx, int iconRes, String text) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_chip_unselected);
        row.setPadding(dp(ctx, 10), dp(ctx, 5), dp(ctx, 10), dp(ctx, 5));
        ImageView icon = new ImageView(ctx);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.primary_maroon)));
        row.addView(icon, new LinearLayout.LayoutParams(dp(ctx, 14), dp(ctx, 14)));
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
        tv.setPadding(dp(ctx, 5), 0, 0, 0);
        row.addView(tv);
        return toDrawable(ctx, row);
    }

    public static int placeIcon(String type) {
        if (type == null) return R.drawable.ic_location;
        switch (type) {
            case "university":
                return R.drawable.ic_school;
            case "supermarket":
                return R.drawable.ic_store;
            case "bus_stop":
                return R.drawable.ic_bus;
            case "hospital":
                return R.drawable.ic_shield;
            default:
                return R.drawable.ic_location;
        }
    }

    /** 0.18 -> "180 m"; 1.25 -> "1,3 km" */
    public static String distanceText(double km) {
        if (km < 1) return Math.max(10, Math.round(km * 100) * 10) + " m";
        return com.example.roomie.ui.search.RoomFormat.km(km) + " km";
    }

    private static Drawable toDrawable(Context ctx, View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    private static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }
}
