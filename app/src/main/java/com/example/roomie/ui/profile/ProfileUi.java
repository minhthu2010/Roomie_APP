package com.example.roomie.ui.profile;

import android.app.Activity;
import android.net.Uri;

import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/** Chuyển giá trị hồ sơ giữa chữ hiển thị trên UI và giá trị API. */
final class ProfileUi {

    private ProfileUi() {}

    static final String[] OCCUPATION_LABELS = {"Sinh viên", "Người đi làm", "Khác"};
    static final String[] OCCUPATION_VALUES = {"student", "worker", "other"};

    static final String[] GENDER_LABELS = {"Nữ", "Nam", "Khác", "Không muốn nói"};
    static final String[] GENDER_VALUES = {"female", "male", "other", "prefer_not_to_say"};

    static final String[] AREAS = {"Quận 1", "Quận 3", "Quận 5", "Quận 7", "Quận 10", "Bình Thạnh",
            "Thủ Đức", "Tân Bình", "Gò Vấp", "Phú Nhuận", "Khác"};

    static String label(String[] labels, String[] values, String value) {
        if (value == null) return null;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return labels[i];
        }
        return null;
    }

    static String value(String[] labels, String[] values, String label) {
        if (label == null) return null;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equalsIgnoreCase(label.trim())) return values[i];
        }
        return null;
    }

    /** Năm sinh hợp lệ theo DB: 1940–2015, mới nhất lên đầu. */
    static String[] birthYears() {
        int max = Math.min(2015, Calendar.getInstance().get(Calendar.YEAR) - 13);
        List<String> years = new ArrayList<>();
        for (int y = max; y >= 1940; y--) years.add(String.valueOf(y));
        return years.toArray(new String[0]);
    }

    /** "Sinh viên · Quận 10" cho thẻ hồ sơ */
    static String subtitle(AccountModels.Profile p) {
        List<String> parts = new ArrayList<>();
        String occ = label(OCCUPATION_LABELS, OCCUPATION_VALUES, p.occupation);
        if (occ != null) parts.add(occ);
        if (p.livingAreaText != null && !p.livingAreaText.isEmpty()) parts.add(p.livingAreaText);
        else if (p.schoolOrWorkplace != null && !p.schoolOrWorkplace.isEmpty()) parts.add(p.schoolOrWorkplace);
        if (parts.isEmpty()) return p.email;
        return android.text.TextUtils.join(" · ", parts);
    }

    // ---------- Ngân sách: "2–4 triệu" <-> budgetMin/budgetMax ----------

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");

    /** Trả về {min, max} theo VND; số nhỏ hơn 1000 được hiểu là triệu. null nếu không đọc được. */
    static long[] parseBudget(String text) {
        if (text == null) return null;
        Matcher m = NUMBER.matcher(text);
        List<Long> values = new ArrayList<>();
        while (m.find() && values.size() < 2) {
            double n = Double.parseDouble(m.group().replace(',', '.'));
            values.add(Math.round(n < 1000 ? n * 1_000_000 : n));
        }
        if (values.isEmpty()) return null;
        if (values.size() == 1) return new long[]{0, values.get(0)};
        return new long[]{Math.min(values.get(0), values.get(1)), Math.max(values.get(0), values.get(1))};
    }

    static String formatBudget(Long min, Long max) {
        if (max == null) return null;
        String maxText = millions(max);
        if (min == null || min == 0) return "Dưới " + maxText + " triệu";
        return millions(min) + "–" + maxText + " triệu";
    }

    private static String millions(long vnd) {
        double m = vnd / 1_000_000.0;
        return m == Math.floor(m) ? String.valueOf((long) m) : String.format(Locale.US, "%.1f", m);
    }

    // ---------- Ngày: dd/MM/yyyy (UI) <-> yyyy-MM-dd (API) ----------

    static String toApiDate(String uiDate) {
        if (uiDate == null) return null;
        String[] p = uiDate.trim().split("/");
        if (p.length != 3) return null;
        try {
            return String.format(Locale.US, "%04d-%02d-%02d",
                    Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String toUiDate(String apiDate) {
        if (apiDate == null) return null;
        String[] p = apiDate.split("-");
        if (p.length != 3) return null;
        return p[2] + "/" + p[1] + "/" + p[0];
    }

    // ---------- Ảnh đại diện ----------

    interface ProfileListener {
        void onProfile(AccountModels.Profile profile);
    }

    /** Đọc ảnh vừa chọn rồi PUT /me/avatar. */
    static void uploadAvatar(Activity activity, Uri uri, ProfileListener listener) {
        String mime = activity.getContentResolver().getType(uri);
        if (mime == null || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) {
            AppNotificationHelper.showError(activity, "Vui lòng chọn ảnh JPG, PNG hoặc WEBP");
            return;
        }
        byte[] bytes;
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > 5 * 1024 * 1024) {
                    AppNotificationHelper.showError(activity, "Ảnh đại diện tối đa 5MB");
                    return;
                }
            }
            bytes = out.toByteArray();
        } catch (IOException e) {
            AppNotificationHelper.showError(activity, "Không đọc được ảnh đã chọn");
            return;
        }

        RequestBody body = RequestBody.create(bytes, MediaType.parse(mime));
        MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", "avatar", body);
        AppNotificationHelper.showInfo(activity, "Đang tải ảnh lên...");
        ApiClient.create(activity, AccountsApi.class).uploadAvatar(part)
                .enqueue(new ApiCallback<AccountModels.Profile>(activity) {
                    @Override
                    public void onSuccess(AccountModels.Profile profile) {
                        AppNotificationHelper.showSuccess(activity, "Đã cập nhật ảnh đại diện");
                        listener.onProfile(profile);
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(activity, message);
                    }
                });
    }
}
