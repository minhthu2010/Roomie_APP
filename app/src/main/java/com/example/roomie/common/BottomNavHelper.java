package com.example.roomie.common;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomie.R;
import com.example.roomie.ui.home.HomeActivity;
import com.example.roomie.ui.myposts.BaiDangCuaToiActivity;
import com.example.roomie.ui.profile.ProfileActivity;
import com.example.roomie.ui.profile.ProfileSubscreenActivity;
import com.example.roomie.ui.saved.PhongDaLuuActivity;

public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void bind(AppCompatActivity activity) {
        if (activity == null) return;

        int selectedTab = selectedTabFor(activity);
        int[] tabIds = {R.id.nav_explore, R.id.nav_saved, R.id.nav_post, R.id.nav_profile};
        int[] labelIds = {R.id.tv_nav_explore, R.id.tv_nav_saved, R.id.tv_nav_post, R.id.tv_nav_profile};
        int[] iconIds = {R.id.img_nav_explore, R.id.img_nav_saved, R.id.img_nav_post, R.id.img_nav_profile};

        for (int index = 0; index < tabIds.length; index++) {
            boolean selected = index == selectedTab;
            View tab = activity.findViewById(tabIds[index]);
            TextView label = activity.findViewById(labelIds[index]);
            ImageView icon = activity.findViewById(iconIds[index]);
            if (tab == null || label == null || icon == null) continue;

            tab.setBackground(selected ? activity.getDrawable(R.drawable.bg_nav_item_selected) : null);
            int labelColor = activity.getColor(selected ? R.color.white : R.color.nav_inactive);
            label.setTextColor(labelColor);
            label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            icon.setColorFilter(labelColor);
        }

        setClick(activity, R.id.nav_explore, HomeActivity.class);
        setClick(activity, R.id.nav_saved, PhongDaLuuActivity.class);
        setClick(activity, R.id.nav_post, BaiDangCuaToiActivity.class);
        setClick(activity, R.id.nav_profile, ProfileActivity.class);
        bindBack(activity);
    }

    /**
     * Nút quay lại ở 4 tab chính: các tab đều nằm trong cùng ngăn xếp (REORDER_TO_FRONT) nên mặc định
     * "quay lại" sẽ nhảy lần lượt qua các tab đã mở. Ở Đã lưu / Bài đăng / Hồ sơ thì về Khám phá,
     * ở Khám phá thì đưa app về nền (như thoát app) thay vì lộ ra tab mở trước đó.
     */
    private static void bindBack(AppCompatActivity activity) {
        Class<?> type = activity.getClass();
        boolean home = type == HomeActivity.class;
        boolean otherTab = type == PhongDaLuuActivity.class || type == BaiDangCuaToiActivity.class
                || type == ProfileActivity.class;
        if (!home && !otherTab) return;
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (home) activity.moveTaskToBack(true);
                else open(activity, HomeActivity.class);
            }
        });
    }

    /** Mở tab: dùng lại màn đã mở (giữ nguyên nội dung, không tải lại), hiện dần thay vì trượt cả màn hình */
    private static void open(AppCompatActivity activity, Class<?> destination) {
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Bundle options = ActivityOptions.makeCustomAnimation(activity, R.anim.tab_fade_in, R.anim.tab_hold).toBundle();
        activity.startActivity(intent, options);
    }

    public static void bindCustomBar(AppCompatActivity activity) {
        bind(activity);
    }

    private static int selectedTabFor(Activity activity) {
        if (activity instanceof PhongDaLuuActivity) return 1;
        if (activity instanceof BaiDangCuaToiActivity) return 2;
        if (activity instanceof ProfileActivity || activity instanceof ProfileSubscreenActivity) return 3;
        return 0;
    }

    private static void setClick(AppCompatActivity activity, int viewId, Class<?> destination) {
        View tab = activity.findViewById(viewId);
        if (tab == null) return;
        tab.setOnClickListener(view -> {
            if (activity.getClass() == destination) return;
            open(activity, destination);
        });
    }
}