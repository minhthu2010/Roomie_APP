package com.example.roomie.common;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.roomie.BuildConfig;

/**
 * Tải ảnh từ server bằng Glide. Backend lưu đường dẫn tương đối ("/uploads/..."),
 * ở đây ghép với địa chỉ server lấy từ BASE_URL (bỏ phần "/api/v1/").
 */
public final class ImageLoader {

    private ImageLoader() {}

    public static String resolveUrl(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String base = BuildConfig.BASE_URL;
        int apiIndex = base.indexOf("/api/");
        String origin = apiIndex > 0 ? base.substring(0, apiIndex) : base.replaceAll("/+$", "");
        return origin + (path.startsWith("/") ? path : "/" + path);
    }

    /** url null/rỗng thì hiện ảnh placeholder */
    public static void load(ImageView view, String path, int placeholderRes) {
        String url = resolveUrl(path);
        if (url == null) {
            Glide.with(view).clear(view);
            view.setImageResource(placeholderRes);
            return;
        }
        Glide.with(view).load(url).placeholder(placeholderRes).error(placeholderRes).centerCrop().into(view);
    }
}
