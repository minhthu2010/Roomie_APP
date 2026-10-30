package com.example.roomie.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lưu token đăng nhập. Người A gọi saveTokens() sau khi đăng nhập thành công
 * và clear() khi đăng xuất; AuthInterceptor tự đọc token từ đây.
 */
public class SessionManager {

    private static final String PREFS = "roomie_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_DISPLAY_NAME = "display_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    /** Tên hiển thị của người đang đăng nhập, cập nhật khi đăng nhập và khi tải/sửa hồ sơ. */
    public void saveDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return;
        prefs.edit().putString(KEY_DISPLAY_NAME, displayName.trim()).apply();
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }

    /** Tên gọi để chào: chữ cuối của tên hiển thị ("Nguyễn Mai Linh" -> "Linh"); null nếu chưa có. */
    public String getGreetingName() {
        String name = getDisplayName();
        if (name == null) return null;
        String[] parts = name.split("\\s+");
        return parts[parts.length - 1];
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
