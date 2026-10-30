package com.example.roomie.ui.profile;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Màn Thông báo (layout_notifications): danh sách, đánh dấu đã đọc, đọc tất cả, xóa. */
final class NotificationsScreen {

    private static final int PAGE_SIZE = 20;

    private final AppCompatActivity activity;
    private final AccountsApi api;
    private final LinearLayout list;
    private final TextView tvUnread;
    private final TextView tvEmpty;
    private final TextView btnLoadMore;
    private int page = 1;
    private int unreadCount;
    private int shownCount;
    /** Nhóm đang thêm vào: 0 = chưa có, 1 = Hôm nay, 2 = Trước đó */
    private int currentSection;

    private NotificationsScreen(AppCompatActivity activity, View root) {
        this.activity = activity;
        this.api = ApiClient.create(activity, AccountsApi.class);
        this.list = root.findViewById(R.id.notification_list);
        this.tvUnread = root.findViewById(R.id.tv_count_unread);
        this.tvEmpty = root.findViewById(R.id.tv_notifications_empty);
        this.btnLoadMore = root.findViewById(R.id.btn_load_more_notifications);
        root.findViewById(R.id.btn_mark_all_read).setOnClickListener(v -> markAllRead());
        btnLoadMore.setOnClickListener(v -> load(page + 1));
    }

    /** Gắn vào màn nếu đang hiển thị layout_notifications. */
    static void bind(AppCompatActivity activity, View root) {
        if (root.findViewById(R.id.notification_list) == null) return;
        new NotificationsScreen(activity, root).load(1);
    }

    private void load(int pageToLoad) {
        btnLoadMore.setEnabled(false);
        api.getNotifications(pageToLoad, PAGE_SIZE)
                .enqueue(new ApiCallback<List<AccountModels.Notification>>(activity) {
                    @Override
                    public void onSuccess(List<AccountModels.Notification> items) {
                        btnLoadMore.setEnabled(true);
                        if (pageToLoad == 1) {
                            list.removeAllViews();
                            shownCount = 0;
                            currentSection = 0;
                        }
                        page = pageToLoad;
                        for (AccountModels.Notification n : items) addItem(n);
                        shownCount += items.size();

                        int total = getMeta() != null ? getMeta().total : shownCount;
                        unreadCount = getMeta() != null ? getMeta().unreadCount : 0;
                        updateUnread();
                        tvEmpty.setVisibility(shownCount == 0 ? View.VISIBLE : View.GONE);
                        btnLoadMore.setVisibility(shownCount < total ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        btnLoadMore.setEnabled(true);
                        AppNotificationHelper.showError(activity, message);
                    }
                });
    }

    private void addItem(AccountModels.Notification n) {
        Date created = parse(n.createdAt);
        int section = created != null && DateUtils.isToday(created.getTime()) ? 1 : 2;
        if (section != currentSection) {
            list.addView(sectionHeader(section == 1 ? R.string.section_today : R.string.section_earlier));
            currentSection = section;
        }

        View card = LayoutInflater.from(activity).inflate(R.layout.item_notification, list, false);
        ((TextView) card.findViewById(R.id.tv_notif_title)).setText(n.title);
        ((TextView) card.findViewById(R.id.tv_notif_body)).setText(n.body);
        ((TextView) card.findViewById(R.id.tv_notif_time)).setText(formatTime(created));
        ((ImageView) card.findViewById(R.id.iv_notif_icon)).setImageResource(iconFor(n.type));

        View dot = card.findViewById(R.id.view_unread_dot);
        View markRead = card.findViewById(R.id.btn_mark_notification_read);
        dot.setVisibility(n.isRead ? View.GONE : View.VISIBLE);
        markRead.setVisibility(n.isRead ? View.GONE : View.VISIBLE);

        Runnable markReadAction = () -> {
            if (n.isRead) return;
            n.isRead = true;
            dot.setVisibility(View.GONE);
            markRead.setVisibility(View.GONE);
            unreadCount = Math.max(0, unreadCount - 1);
            updateUnread();
            api.markNotificationRead(n.notificationId).enqueue(silent());
        };
        markRead.setOnClickListener(v -> markReadAction.run());
        card.setOnClickListener(v -> markReadAction.run());

        card.findViewById(R.id.btn_delete_notification).setOnClickListener(v -> {
            api.deleteNotification(n.notificationId).enqueue(new ApiCallback<Object>(activity) {
                @Override
                public void onSuccess(Object data) {
                    list.removeView(card);
                    if (!n.isRead) unreadCount = Math.max(0, unreadCount - 1);
                    shownCount--;
                    updateUnread();
                    if (shownCount == 0) load(1);
                }

                @Override
                public void onError(String message) {
                    AppNotificationHelper.showError(activity, message);
                }
            });
        });
        list.addView(card);
    }

    private void markAllRead() {
        if (unreadCount == 0) return;
        api.markAllNotificationsRead().enqueue(new ApiCallback<Object>(activity) {
            @Override
            public void onSuccess(Object data) {
                load(1);
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(activity, message);
            }
        });
    }

    private void updateUnread() {
        tvUnread.setText(unreadCount > 0
                ? activity.getString(R.string.count_unread_format, unreadCount)
                : activity.getString(R.string.count_all_read));
    }

    private TextView sectionHeader(int textRes) {
        TextView header = new TextView(activity);
        header.setText(textRes);
        header.setTextColor(activity.getColor(R.color.roomie_text_primary));
        header.setTextSize(15);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = list.getChildCount() == 0 ? 0 : dp(4);
        lp.bottomMargin = dp(12);
        header.setLayoutParams(lp);
        return header;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static int iconFor(String type) {
        if (type == null) return R.drawable.ic_bell;
        switch (type) {
            case "room_match": return R.drawable.ic_heart;
            case "post_activity":
            case "post_moderation": return R.drawable.ic_check_circle;
            case "post_expiring": return R.drawable.ic_clock;
            case "report_update": return R.drawable.ic_flag;
            case "feedback_reply": return R.drawable.ic_chat;
            default: return R.drawable.ic_bell;
        }
    }

    private static Date parse(String isoUtc) {
        if (isoUtc == null) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(isoUtc);
        } catch (ParseException e) {
            return null;
        }
    }

    /** Hôm nay: "10:24"; ngày khác: "28/10" */
    private static String formatTime(Date date) {
        if (date == null) return "";
        String pattern = DateUtils.isToday(date.getTime()) ? "HH:mm" : "dd/MM";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    private ApiCallback<AccountModels.Notification> silent() {
        return new ApiCallback<AccountModels.Notification>(activity) {
            @Override
            public void onSuccess(AccountModels.Notification data) {}

            @Override
            public void onError(String message) {}
        };
    }
}
