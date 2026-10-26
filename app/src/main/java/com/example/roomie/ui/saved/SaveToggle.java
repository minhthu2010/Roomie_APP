package com.example.roomie.ui.saved;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

/** Lưu / bỏ lưu phòng, dùng chung cho Home, Chi tiết phòng, Đã lưu. */
public final class SaveToggle {

    public interface Listener {
        void onChanged(boolean isSaved);
    }

    private SaveToggle() {}

    public static void bindIcon(ImageView icon, boolean isSaved) {
        icon.setImageResource(isSaved ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        // ic_heart vẽ màu trắng (dùng cho thanh điều hướng) nên tô màu chủ đạo khi đã lưu
        icon.setImageTintList(isSaved
                ? ColorStateList.valueOf(icon.getContext().getColor(R.color.primary_maroon)) : null);
    }

    /** Đổi icon ngay cho mượt; lỗi thì trả lại trạng thái cũ. */
    public static void toggle(Activity activity, String postId, boolean currentlySaved, ImageView icon, Listener listener) {
        boolean target = !currentlySaved;
        if (icon != null) bindIcon(icon, target);
        DiscoveryApi api = ApiClient.create(activity, DiscoveryApi.class);
        (target ? api.savePost(postId) : api.unsavePost(postId))
                .enqueue(new ApiCallback<RoomModels.SaveResult>(activity) {
                    @Override
                    public void onSuccess(RoomModels.SaveResult data) {
                        AppNotificationHelper.showSuccess(activity, activity.getString(
                                target ? R.string.saved_added : R.string.saved_removed));
                        if (listener != null) listener.onChanged(target);
                    }

                    @Override
                    public void onError(String message) {
                        if (icon != null) bindIcon(icon, currentlySaved);
                        AppNotificationHelper.showError(activity, message);
                    }
                });
    }
}
