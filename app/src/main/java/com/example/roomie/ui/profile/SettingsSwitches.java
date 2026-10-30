package com.example.roomie.ui.profile;

import android.view.View;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Công tắc ở màn Cài đặt thông báo và Quyền riêng tư: tải từ GET /me/settings,
 * bật/tắt là PATCH ngay một trường; lỗi thì trả công tắc về như cũ.
 */
final class SettingsSwitches {

    private interface Setter {
        void set(AccountModels.Settings s, Boolean value);
    }

    private interface Getter {
        Boolean get(AccountModels.Settings s);
    }

    private static final class Field {
        final Getter getter;
        final Setter setter;

        Field(Getter getter, Setter setter) {
            this.getter = getter;
            this.setter = setter;
        }
    }

    private static final Map<Integer, Field> FIELDS = new LinkedHashMap<>();

    static {
        FIELDS.put(R.id.switch_matching, new Field(s -> s.notifyRoomMatches, (s, v) -> s.notifyRoomMatches = v));
        FIELDS.put(R.id.switch_post_activity, new Field(s -> s.notifyPostActivity, (s, v) -> s.notifyPostActivity = v));
        FIELDS.put(R.id.switch_post_expiring, new Field(s -> s.notifyPostExpiring, (s, v) -> s.notifyPostExpiring = v));
        FIELDS.put(R.id.switch_co_living_tips, new Field(s -> s.notifyTips, (s, v) -> s.notifyTips = v));
        FIELDS.put(R.id.switch_show_profile, new Field(s -> s.showProfile, (s, v) -> s.showProfile = v));
        FIELDS.put(R.id.switch_show_contact, new Field(s -> s.showContactOnPosts, (s, v) -> s.showContactOnPosts = v));
    }

    private SettingsSwitches() {}

    static void bind(AppCompatActivity activity, View root) {
        Map<CompoundButton, Field> present = new LinkedHashMap<>();
        for (Map.Entry<Integer, Field> e : FIELDS.entrySet()) {
            View view = root.findViewById(e.getKey());
            if (view instanceof CompoundButton) present.put((CompoundButton) view, e.getValue());
        }
        if (present.isEmpty()) return;

        AccountsApi api = ApiClient.create(activity, AccountsApi.class);
        // Khóa công tắc tới khi có giá trị thật, tránh lưu nhầm theo giá trị mẫu trong layout
        for (CompoundButton sw : present.keySet()) sw.setEnabled(false);

        api.getSettings().enqueue(new ApiCallback<AccountModels.Settings>(activity) {
            @Override
            public void onSuccess(AccountModels.Settings settings) {
                for (Map.Entry<CompoundButton, Field> e : present.entrySet()) {
                    CompoundButton sw = e.getKey();
                    Field field = e.getValue();
                    sw.setChecked(Boolean.TRUE.equals(field.getter.get(settings)));
                    sw.setEnabled(true);
                    sw.setOnCheckedChangeListener((button, checked) -> save(activity, api, button, field, checked));
                }
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(activity, message);
            }
        });
    }

    private static void save(AppCompatActivity activity, AccountsApi api, CompoundButton button,
                             Field field, boolean checked) {
        AccountModels.Settings body = new AccountModels.Settings();
        field.setter.set(body, checked);
        button.setEnabled(false);
        api.updateSettings(body).enqueue(new ApiCallback<AccountModels.Settings>(activity) {
            @Override
            public void onSuccess(AccountModels.Settings saved) {
                button.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                // Trả công tắc về trạng thái cũ mà không gọi lại API
                CompoundButton.OnCheckedChangeListener listener =
                        (b, c) -> save(activity, api, b, field, c);
                button.setOnCheckedChangeListener(null);
                button.setChecked(!checked);
                button.setOnCheckedChangeListener(listener);
                button.setEnabled(true);
                AppNotificationHelper.showError(activity, message);
            }
        });
    }
}
