package com.example.roomie.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.net.Uri;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;

import java.util.ArrayList;

public class ProfileSubscreenActivity extends AppCompatActivity {
    private static final String EXTRA_LAYOUT = "profile_subscreen_layout";
    private static final String STATE_LAYOUTS = "profile_subscreen_history";
    private final ArrayList<Integer> layoutHistory = new ArrayList<>();

    public static void open(AppCompatActivity activity, @LayoutRes int layout) {
        Intent intent = new Intent(activity, ProfileSubscreenActivity.class);
        intent.putExtra(EXTRA_LAYOUT, layout);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_subscreen);
        BottomNavHelper.bind(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootProfileSubscreen), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            int[] savedLayouts = savedInstanceState.getIntArray(STATE_LAYOUTS);
            if (savedLayouts != null) {
                for (int layout : savedLayouts) layoutHistory.add(layout);
            }
        }
        if (layoutHistory.isEmpty()) layoutHistory.add(getIntent().getIntExtra(EXTRA_LAYOUT, 0));
        findViewById(R.id.btn_back).setOnClickListener(view -> onBackPressed());
        renderCurrentLayout();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int layout = intent.getIntExtra(EXTRA_LAYOUT, 0);
        if (layout != 0 && (layoutHistory.isEmpty() || layoutHistory.get(layoutHistory.size() - 1) != layout)) {
            layoutHistory.add(layout);
        }
        renderCurrentLayout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        int[] layouts = new int[layoutHistory.size()];
        for (int index = 0; index < layoutHistory.size(); index++) layouts[index] = layoutHistory.get(index);
        outState.putIntArray(STATE_LAYOUTS, layouts);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (layoutHistory.size() > 1) {
            layoutHistory.remove(layoutHistory.size() - 1);
            renderCurrentLayout();
        } else {
            super.onBackPressed();
        }
    }

    private void renderCurrentLayout() {
        if (layoutHistory.isEmpty() || layoutHistory.get(layoutHistory.size() - 1) == 0) {
            finish();
            return;
        }
        FrameLayout content = findViewById(R.id.profileSubscreenContent);
        int layout = layoutHistory.get(layoutHistory.size() - 1);
        android.widget.TextView title = findViewById(R.id.tv_header_title);
        title.setText(titleFor(layout));
        View bottomNavigation = findViewById(R.id.bottomNavigationContainer);
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(hasMainBottomNavigation(layout) ? View.VISIBLE : View.GONE);
        }
        content.removeAllViews();
        View screen = getLayoutInflater().inflate(layout, content, false);
        hideLegacyBottomBar(screen);
        content.addView(screen);
        bindScreenActions(screen);
    }

    private int titleFor(int layout) {
        if (layout == R.layout.layout_edit_profile) return R.string.title_edit_profile;
        if (layout == R.layout.layout_verification_safety) return R.string.title_verification_safety;
        if (layout == R.layout.layout_notifications) return R.string.title_notifications;
        if (layout == R.layout.layout_settings) return R.string.title_settings;
        if (layout == R.layout.layout_privacy_account) return R.string.title_privacy_account_header;
        if (layout == R.layout.layout_notification_settings) return R.string.title_notif_settings_header;
        if (layout == R.layout.layout_help_support) return R.string.title_help_header;
        if (layout == R.layout.layout_send_feedback) return R.string.title_send_feedback_header;
        if (layout == R.layout.layout_change_password) return R.string.title_change_password;
        return R.string.title_my_profile;
    }

    private boolean hasMainBottomNavigation(int layout) {
        return layout == R.layout.layout_verification_safety
                || layout == R.layout.layout_notifications
                || layout == R.layout.layout_settings
                || layout == R.layout.layout_privacy_account
                || layout == R.layout.layout_notification_settings
                || layout == R.layout.layout_help_support;
    }

    private void hideLegacyBottomBar(View screen) {
        int[] legacyBars = {
                R.id.bottom_nav_bar_safety,
                R.id.bottom_nav_bar_notif,
                R.id.bottom_nav_bar_settings,
                R.id.bottom_nav_bar_priv,
                R.id.bottom_nav_bar_notif_set,
                R.id.bottom_nav_bar_help
        };
        for (int id : legacyBars) {
            View legacyBar = screen.findViewById(id);
            if (legacyBar != null) legacyBar.setVisibility(View.GONE);
        }
    }

    private void bindScreenActions(View screen) {
        bind(screen, R.id.menu_change_password_item, R.layout.layout_change_password);
        bind(screen, R.id.menu_notif_settings_item, R.layout.layout_notification_settings);
        bind(screen, R.id.menu_privacy_account_item, R.layout.layout_privacy_account);
        bind(screen, R.id.menu_help_item, R.layout.layout_help_support);
        bind(screen, R.id.menu_feedback_item, R.layout.layout_send_feedback);
        bind(screen, R.id.menu_contact_visibility, R.layout.layout_privacy_account);
        bind(screen, R.id.menu_change_pass_priv_item, R.layout.layout_change_password);
        bind(screen, R.id.btn_link_send_feedback, R.layout.layout_send_feedback);
        bind(screen, R.id.btn_contact_support, R.layout.layout_send_feedback);

        bindBack(screen, R.id.btn_cancel_edit_profile);
        bindBack(screen, R.id.btn_cancel_change_pass);
        bindBack(screen, R.id.btn_cancel_feedback);
        showUnavailable(screen, R.id.btn_save_profile_changes);
        showUnavailable(screen, R.id.btn_save_password);
        showUnavailable(screen, R.id.btn_submit_feedback_final);
        showUnavailable(screen, R.id.btn_reverify_phone);
        showUnavailable(screen, R.id.btn_send_email_code);
        showUnavailable(screen, R.id.iv_edit_avatar);
        showUnavailable(screen, R.id.btn_forgot_pass_link);
        showUnavailable(screen, R.id.area_feedback_attachment);
        bindNotificationSettings(screen);
        confirmLogout(screen, R.id.btn_confirm_logout);
        confirmLogout(screen, R.id.btn_logout_settings);
        showUnavailable(screen, R.id.menu_terms_item);
        showUnavailable(screen, R.id.menu_privacy_policy_item);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq2);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq3);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq4);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq5);
        bindNotificationActions(screen);
        bindBack(screen, R.id.btn_stay_logged_in);
    }

    private void bind(View root, int viewId, @LayoutRes int layout) {
        View view = root.findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> navigateToLayout(layout));
    }

    private void navigateToLayout(@LayoutRes int layout) {
        if (layoutHistory.isEmpty() || layoutHistory.get(layoutHistory.size() - 1) != layout) {
            layoutHistory.add(layout);
            renderCurrentLayout();
        }
    }

    private void bindBack(View root, int viewId) {
        View view = root.findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> onBackPressed());
    }

    private void showUnavailable(View root, int viewId) {
        View view = root.findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> new AlertDialog.Builder(this)
                .setMessage(R.string.profile_action_unavailable)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

    private void showUnavailableOnParent(View root, int viewId) {
        View child = root.findViewById(viewId);
        if (child != null && child.getParent() instanceof View) {
            ((View) child.getParent()).setOnClickListener(ignored -> new AlertDialog.Builder(this)
                    .setMessage(R.string.profile_action_unavailable)
                    .setPositiveButton(android.R.string.ok, null)
                    .show());
        }
    }

    private void confirmLogout(View root, int viewId) {
        View view = root.findViewById(viewId);
        if (view != null) view.setOnClickListener(ignored -> new AlertDialog.Builder(this)
                .setTitle(R.string.box_logout_confirm_title)
                .setMessage(R.string.box_logout_confirm_sub)
                .setNegativeButton(R.string.btn_stay, null)
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> new AlertDialog.Builder(this)
                        .setMessage(R.string.profile_action_unavailable)
                        .setPositiveButton(android.R.string.ok, null)
                        .show())
                .show());
    }

    private void bindNotificationSettings(View root) {
        View view = root.findViewById(R.id.btn_device_notif_settings);
        if (view == null) return;
        view.setOnClickListener(ignored -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
    }

    private void bindNotificationActions(View root) {
        View markRead = root.findViewById(R.id.btn_mark_notification_read);
        View delete = root.findViewById(R.id.btn_delete_notification);
        View unreadCount = root.findViewById(R.id.tv_count_unread);
        View timeBox = root.findViewById(R.id.box_notif1_time);
        View notificationCard = root.findViewById(R.id.box_notif1_icon);
        View card = notificationCard == null ? null : (View) notificationCard.getParent();
        if (markRead != null) markRead.setOnClickListener(ignored -> {
            markRead.setVisibility(View.GONE);
            if (unreadCount instanceof android.widget.TextView) {
                ((android.widget.TextView) unreadCount).setText(R.string.count_unread_one);
            }
            if (timeBox instanceof ViewGroup && ((ViewGroup) timeBox).getChildCount() > 1) {
                ((ViewGroup) timeBox).getChildAt(1).setVisibility(View.GONE);
            }
        });
        if (delete != null) delete.setOnClickListener(ignored -> {
            if (card != null) card.setVisibility(View.GONE);
            if (unreadCount instanceof android.widget.TextView) {
                ((android.widget.TextView) unreadCount).setText(R.string.count_unread_one);
            }
        });
    }
}
