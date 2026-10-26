package com.example.roomie.data.remote;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bóc { "data": ... } và đọc thông báo lỗi { "error": { "message" } } của backend.
 * Truyền activity vào để bỏ qua kết quả khi màn hình đã đóng.
 */
public abstract class ApiCallback<T> implements Callback<ApiResponse<T>> {

    private final Activity activity;
    private ApiResponse.Meta meta;

    protected ApiCallback(Activity activity) {
        this.activity = activity;
    }

    public abstract void onSuccess(T data);

    /** Thông tin phân trang (page, limit, total) của API danh sách; null với API khác. */
    protected ApiResponse.Meta getMeta() {
        return meta;
    }

    public abstract void onError(String message);

    @Override
    public void onResponse(@NonNull Call<ApiResponse<T>> call, @NonNull Response<ApiResponse<T>> response) {
        if (isGone()) return;
        if (response.isSuccessful() && response.body() != null) {
            meta = response.body().meta;
            onSuccess(response.body().data);
        } else {
            onError(readErrorMessage(response));
        }
    }

    @Override
    public void onFailure(@NonNull Call<ApiResponse<T>> call, @NonNull Throwable t) {
        if (isGone() || call.isCanceled()) return;
        onError("Không kết nối được máy chủ. Kiểm tra mạng hoặc server rồi thử lại.");
    }

    private boolean isGone() {
        return activity != null && (activity.isFinishing() || activity.isDestroyed());
    }

    private static String readErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                JsonObject root = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                JsonObject error = root.getAsJsonObject("error");
                if (error != null && error.has("details") && error.get("details").isJsonArray()
                        && error.getAsJsonArray("details").size() > 0) {
                    JsonObject first = error.getAsJsonArray("details").get(0).getAsJsonObject();
                    if (first.has("message")) return first.get("message").getAsString();
                }
                if (error != null && error.has("message")) return error.get("message").getAsString();
            }
        } catch (Exception ignored) {
            // body không phải JSON của backend
        }
        return "Có lỗi xảy ra (mã " + response.code() + ")";
    }
}
