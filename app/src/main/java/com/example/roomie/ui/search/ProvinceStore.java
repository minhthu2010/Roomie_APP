package com.example.roomie.ui.search;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

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
