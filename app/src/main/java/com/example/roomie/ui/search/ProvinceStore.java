package com.example.roomie.ui.search;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.PlaceModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tỉnh/thành người dùng đang tìm phòng (lưu trên máy). Feed Home, ô chọn phường, gợi ý trường /
 * địa chỉ và tâm bản đồ đi theo tỉnh này. Mặc định TP.HCM.
 */
public final class ProvinceStore {

    private static final String PREFS = "roomie_province";
    private static final String KEY_CODE = "code";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";

    // Mặc định: TP.HCM, tâm hành chính theo OpenStreetMap
    private static final String DEFAULT_CODE = "79";
    private static final String DEFAULT_NAME = "Thành phố Hồ Chí Minh";
    private static final double DEFAULT_LAT = 10.77370;
    private static final double DEFAULT_LNG = 106.71660;

    private ProvinceStore() {}

    public static PlaceModels.Province get(Context context) {
        SharedPreferences p = prefs(context);
        PlaceModels.Province province = new PlaceModels.Province();
        province.code = p.getString(KEY_CODE, DEFAULT_CODE);
        province.name = p.getString(KEY_NAME, DEFAULT_NAME);
        province.latitude = (double) p.getFloat(KEY_LAT, (float) DEFAULT_LAT);
        province.longitude = (double) p.getFloat(KEY_LNG, (float) DEFAULT_LNG);
        return province;
    }

    /** Đã có tỉnh (người dùng chọn, hoặc lấy từ hồ sơ / GPS) hay vẫn đang dùng mặc định TP.HCM */
    public static boolean isSet(Context context) {
        return prefs(context).contains(KEY_CODE);
    }

    /**
     * Lần đầu mở app: đặt tỉnh theo hồ sơ (khu vực / trường); hồ sơ chưa có thì theo vị trí GPS
     * gần đúng (chỉ khi đã có quyền vị trí, không hỏi quyền ở đây). Xong thì gọi onSet.
     */
    public static void initDefault(Activity activity, Runnable onSet) {
        if (isSet(activity)) return;
        DiscoveryApi api = ApiClient.create(activity, DiscoveryApi.class);
        api.getSearchDefaults().enqueue(new ApiCallback<PlaceModels.SearchDefaults>(activity) {
            @Override
            public void onSuccess(PlaceModels.SearchDefaults data) {
                if (data != null && data.province != null) {
                    save(activity, data.province);
                    onSet.run();
                } else {
                    fromLocation(activity, api, onSet);
                }
            }

            @Override
            public void onError(String message) {
                fromLocation(activity, api, onSet);
            }
        });
    }

    /** Tỉnh có tâm hành chính gần vị trí cuối cùng của máy nhất (đủ dùng để chọn tỉnh mặc định) */
    @SuppressLint("MissingPermission")
    private static void fromLocation(Activity activity, DiscoveryApi api, Runnable onSet) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        Location last = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (last == null || l.getTime() > last.getTime())) last = l;
        }
        if (last == null) return;
        double lat = last.getLatitude();
        double lng = last.getLongitude();
        api.getProvinces().enqueue(new ApiCallback<List<PlaceModels.Province>>(activity) {
            @Override
            public void onSuccess(List<PlaceModels.Province> provinces) {
                PlaceModels.Province best = null;
                double bestD = Double.MAX_VALUE;
                for (PlaceModels.Province p : provinces) {
                    if (p.latitude == null || p.longitude == null) continue;
                    double dLat = p.latitude - lat;
                    double dLng = (p.longitude - lng) * Math.cos(Math.toRadians(lat));
                    double d = dLat * dLat + dLng * dLng;
                    if (d < bestD) { bestD = d; best = p; }
                }
                // Xa hơn ~2 độ (ngoài Việt Nam, ví dụ máy ảo ở Mỹ) thì giữ mặc định
                if (best != null && bestD < 4) {
                    save(activity, best);
                    onSet.run();
                }
            }

            @Override
            public void onError(String message) {
                // Giữ mặc định TP.HCM
            }
        });
    }

    public static String code(Context context) {
        return prefs(context).getString(KEY_CODE, DEFAULT_CODE);
    }

    public static void save(Context context, PlaceModels.Province province) {
        SharedPreferences.Editor e = prefs(context).edit()
                .putString(KEY_CODE, province.code)
                .putString(KEY_NAME, province.name);
        if (province.latitude != null && province.longitude != null) {
            e.putFloat(KEY_LAT, province.latitude.floatValue()).putFloat(KEY_LNG, province.longitude.floatValue());
        }
        e.apply();
    }

    /** "Thành phố Hồ Chí Minh" -> "TP. Hồ Chí Minh", "Tỉnh Lâm Đồng" -> "Lâm Đồng" (nhãn ngắn) */
    public static String shortName(String fullName) {
        if (fullName == null) return "";
        return fullName.replaceFirst("^Thành phố ", "TP. ").replaceFirst("^Tỉnh ", "");
    }

    /** Hộp chọn 1 trong 34 tỉnh/thành; chọn xong thì lưu và gọi onPicked */
    public static void pick(Activity activity, Consumer<PlaceModels.Province> onPicked) {
        ApiClient.create(activity, DiscoveryApi.class).getProvinces()
                .enqueue(new ApiCallback<List<PlaceModels.Province>>(activity) {
                    @Override
                    public void onSuccess(List<PlaceModels.Province> provinces) {
                        if (provinces == null || provinces.isEmpty() || activity.isFinishing()) return;
                        String current = code(activity);
                        String[] names = new String[provinces.size()];
                        int checked = 0;
                        for (int i = 0; i < provinces.size(); i++) {
                            names[i] = provinces.get(i).name;
                            if (provinces.get(i).code.equals(current)) checked = i;
                        }
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.province_pick_title)
                                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                                    dialog.dismiss();
                                    PlaceModels.Province picked = provinces.get(which);
                                    if (picked.code.equals(current)) return;
                                    save(activity, picked);
                                    onPicked.accept(picked);
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

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
