package com.example.roomie.ui.room;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

/**
 * Nút "Liên hệ Zalo" / "Gọi điện": mở Zalo hoặc màn quay số, đồng thời ghi lượt quan tâm
 * (POST /room-posts/{id}/contact-clicks). Không có số thì báo lý do.
 */
public final class ContactActions {

    private ContactActions() {}

    public static void zalo(Activity activity, String postId, RoomModels.Contact contact) {
        if (!checkAvailable(activity, contact, contact != null && contact.allowZalo)) return;
        logClick(activity, postId, "zalo");
        open(activity, new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + contact.phone)));
    }

    public static void call(Activity activity, String postId, RoomModels.Contact contact) {
        if (!checkAvailable(activity, contact, contact != null && contact.allowCall)) return;
        logClick(activity, postId, "call");
        // ACTION_DIAL chỉ mở màn quay số, không cần quyền CALL_PHONE
        open(activity, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.phone)));
    }

    private static boolean checkAvailable(Activity activity, RoomModels.Contact contact, boolean channelAllowed) {
        if (contact == null || contact.phone == null) {
            String reason = contact != null ? contact.hiddenReason : null;
            int msg = "login".equals(reason) ? R.string.contact_need_login
                    : "not_available".equals(reason) ? R.string.contact_not_available
                    : R.string.contact_hidden;
            AppNotificationHelper.showInfo(activity, activity.getString(msg));
            return false;
        }
        if (!channelAllowed) {
            AppNotificationHelper.showInfo(activity, activity.getString(R.string.contact_channel_off));
            return false;
        }
        return true;
    }

    private static void logClick(Activity activity, String postId, String channel) {
        ApiClient.create(activity, DiscoveryApi.class)
                .logContactClick(postId, new RoomModels.ContactClick(channel))
                .enqueue(new ApiCallback<RoomModels.ContactClick>(activity) {
                    @Override
                    public void onSuccess(RoomModels.ContactClick data) {}

                    @Override
                    public void onError(String message) {
                        // Ghi lượt bấm lỗi không chặn việc liên hệ
                    }
                });
    }

    private static void open(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppNotificationHelper.showError(activity, activity.getString(R.string.contact_no_app));
        }
    }
}
