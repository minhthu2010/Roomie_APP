package com.example.roomie.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.net.Uri;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;
import com.example.roomie.ui.auth.AuthFlow;
import com.example.roomie.ui.auth.ForgotPasswordActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ProfileSubscreenActivity extends AppCompatActivity {
    private static final String EXTRA_LAYOUT = "profile_subscreen_layout";
    private static final String STATE_LAYOUTS = "profile_subscreen_history";
    private final ArrayList<Integer> layoutHistory = new ArrayList<>();
    private AccountsApi api;
    private ImageView editAvatarView;

    private final ActivityResultLauncher<String> pickAvatar = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && editAvatarView != null) {
                    ProfileUi.uploadAvatar(this, uri,
                            profile -> ImageLoader.load(editAvatarView, profile.avatarUrl, R.drawable.ic_avatar_profile));
                }
            });

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
        api = ApiClient.create(this, AccountsApi.class);
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
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> finish());
        }
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

    private void renderCurrentLayout() {
        if (layoutHistory.isEmpty() || layoutHistory.get(layoutHistory.size() - 1) == 0) {
            finish();
            return;
        }
        FrameLayout content = findViewById(R.id.profileSubscreenContent);
        int layout = layoutHistory.get(layoutHistory.size() - 1);
        TextView title = findViewById(R.id.tv_header_title);
        if (title != null) {
            title.setText(titleFor(layout));
        }
        View bottomNavigation = findViewById(R.id.bottomNavigationContainer);
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
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
        if (layout == R.layout.layout_change_password) return R.string.menu_change_password;
        return R.string.title_my_profile;
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
        bindEditProfile(screen);
        bindChangePassword(screen);
        showUnavailable(screen, R.id.btn_submit_feedback_final);
        showUnavailable(screen, R.id.btn_reverify_phone);
        showUnavailable(screen, R.id.btn_send_email_code);
        View forgotPass = screen.findViewById(R.id.btn_forgot_pass_link);
        if (forgotPass != null) forgotPass.setOnClickListener(ignored ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        int attachmentId = screen.getResources().getIdentifier("area_feedback_attachment", "id", getPackageName());
        if (attachmentId != 0) showUnavailable(screen, attachmentId);

        bindNotificationSettings(screen);
        confirmLogout(screen, R.id.btn_confirm_logout);
        confirmLogout(screen, R.id.btn_logout_settings);
        showUnavailable(screen, R.id.menu_terms_item);
        showUnavailable(screen, R.id.menu_privacy_policy_item);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq2);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq3);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq4);
        showUnavailableOnParent(screen, R.id.iv_chevron_faq5);
        showAccountName(screen);
        NotificationsScreen.bind(this, screen);
        SettingsSwitches.bind(this, screen);
        bindBack(screen, R.id.btn_stay_logged_in);
    }

    // ---------- Chỉnh sửa hồ sơ (layout_edit_profile) ----------

    private void bindEditProfile(View root) {
        View save = root.findViewById(R.id.btn_save_profile_changes);
        if (save == null) return;
        EditText etName = root.findViewById(R.id.et_edit_display_name);
        EditText etOccupation = root.findViewById(R.id.et_edit_occupation);
        EditText etSchool = root.findViewById(R.id.et_edit_school);
        EditText etAbout = root.findViewById(R.id.et_edit_about_me);
        EditText etArea = root.findViewById(R.id.et_edit_living_area);
        editAvatarView = root.findViewById(R.id.iv_edit_avatar);

        // Bỏ chữ mẫu trong layout, chờ dữ liệu thật
        for (EditText field : new EditText[]{etName, etOccupation, etSchool, etAbout, etArea}) field.setText("");

        editAvatarView.setOnClickListener(ignored -> pickAvatar.launch("image/*"));

        // Nghề nghiệp chọn từ danh sách cố định (DB chỉ nhận student/worker/other)
        etOccupation.setFocusable(false);
        etOccupation.setOnClickListener(ignored -> new AlertDialog.Builder(this)
                .setTitle("Chọn nghề nghiệp")
                .setItems(ProfileUi.OCCUPATION_LABELS, (dialog, which) -> etOccupation.setText(ProfileUi.OCCUPATION_LABELS[which]))
                .show());

        View changeAvatar = root.findViewById(R.id.tv_edit_change_avatar);
        if (changeAvatar != null) changeAvatar.setOnClickListener(ignored -> pickAvatar.launch("image/*"));

        // Giới tính, năm sinh, ngày dọn vào, nhu cầu: giữ giá trị API trong mảng 1 phần tử để lambda sửa được
        TextView tvGender = root.findViewById(R.id.tv_edit_gender);
        TextView tvBirthYear = root.findViewById(R.id.tv_edit_birth_year);
        EditText etBudget = root.findViewById(R.id.et_edit_budget);
        TextView tvMoveIn = root.findViewById(R.id.tv_edit_move_in_date);
        TextView pillFindRoom = root.findViewById(R.id.btn_edit_pill_find_room);
        TextView pillFindRoommate = root.findViewById(R.id.btn_edit_pill_find_roommate);
        String[] gender = {null};
        Integer[] birthYear = {null};
        String[] goal = {null};
        PlacePickers.Area[] area = {null};
        // landmarkId: null = chưa đổi, 0 = nơi làm tự ghi / bỏ chọn, > 0 = trường trong danh sách
        Integer[] landmarkId = {null};

        // Khu vực muốn tìm phòng: tỉnh/thành -> phường/xã thật
        etArea.setFocusable(false);
        etArea.setOnClickListener(ignored -> PlacePickers.pickArea(this,
                area[0] != null ? area[0].provinceCode : null, picked -> {
                    area[0] = picked;
                    etArea.setText(picked.label());
                }));

        // Trường / nơi làm: chọn từ gợi ý trường thật, hoặc ghi nơi làm khác
        etSchool.setFocusable(false);
        etSchool.setOnClickListener(ignored -> PlacePickers.pickSchool(this,
                area[0] != null ? area[0].provinceCode : null, school -> {
                    landmarkId[0] = school.landmarkId;
                    etSchool.setText(school.name != null ? school.name : "");
                }));

        tvGender.setOnClickListener(ignored -> new AlertDialog.Builder(this)
                .setTitle("Chọn giới tính")
                .setItems(ProfileUi.GENDER_LABELS, (dialog, which) -> {
                    gender[0] = ProfileUi.GENDER_VALUES[which];
                    tvGender.setText(ProfileUi.GENDER_LABELS[which]);
                })
                .show());

        String[] years = ProfileUi.birthYears();
        tvBirthYear.setOnClickListener(ignored -> new AlertDialog.Builder(this)
                .setTitle("Chọn năm sinh")
                .setItems(years, (dialog, which) -> {
                    birthYear[0] = Integer.parseInt(years[which]);
                    tvBirthYear.setText(years[which]);
                })
                .show());

        tvMoveIn.setOnClickListener(ignored -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(this,
                    (view, year, month, day) -> tvMoveIn.setText(
                            String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(c.getTimeInMillis() - 1000);
            picker.show();
        });

        Runnable renderGoal = () -> {
            boolean room = "find_room".equals(goal[0]) || "both".equals(goal[0]);
            boolean mate = "find_roommate".equals(goal[0]) || "both".equals(goal[0]);
            stylePill(pillFindRoom, room);
            stylePill(pillFindRoommate, mate);
        };
        pillFindRoom.setOnClickListener(ignored -> {
            goal[0] = "find_room";
            renderGoal.run();
        });
        pillFindRoommate.setOnClickListener(ignored -> {
            goal[0] = "find_roommate";
            renderGoal.run();
        });

        api.getMyProfile().enqueue(new ApiCallback<AccountModels.Profile>(this) {
            @Override
            public void onSuccess(AccountModels.Profile p) {
                if (p == null) return;
                etName.setText(p.displayName);
                String occ = ProfileUi.label(ProfileUi.OCCUPATION_LABELS, ProfileUi.OCCUPATION_VALUES, p.occupation);
                etOccupation.setText(occ != null ? occ : "");
                etSchool.setText(p.schoolOrWorkplace != null ? p.schoolOrWorkplace : "");
                etAbout.setText(p.bio != null ? p.bio : "");
                area[0] = ProfileUi.areaOf(p);
                etArea.setText(area[0] != null ? area[0].label() : "");
                ImageLoader.load(editAvatarView, p.avatarUrl, R.drawable.ic_avatar_profile);

                gender[0] = p.gender;
                tvGender.setText(ProfileUi.label(ProfileUi.GENDER_LABELS, ProfileUi.GENDER_VALUES, p.gender));
                birthYear[0] = p.birthYear;
                tvBirthYear.setText(p.birthYear != null ? String.valueOf(p.birthYear) : null);
                String budget = ProfileUi.formatBudget(p.budgetMin, p.budgetMax);
                etBudget.setText(budget != null ? budget : "");
                tvMoveIn.setText(ProfileUi.toUiDate(p.moveInDate));
                goal[0] = p.onboardingGoal;
                renderGoal.run();
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(ProfileSubscreenActivity.this, message);
            }
        });

        save.setOnClickListener(ignored -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                AppNotificationHelper.showError(this, "Vui lòng nhập tên hiển thị");
                return;
            }
            AccountModels.UpdateProfileRequest body = new AccountModels.UpdateProfileRequest();
            body.displayName = name;
            body.occupation = ProfileUi.value(ProfileUi.OCCUPATION_LABELS, ProfileUi.OCCUPATION_VALUES,
                    etOccupation.getText().toString());
            body.schoolOrWorkplace = etSchool.getText().toString().trim();
            body.landmarkId = landmarkId[0];
            body.bio = etAbout.getText().toString().trim();
            if (area[0] != null) {
                body.livingProvinceCode = area[0].provinceCode;
                body.livingWardCode = area[0].wardCode != null ? area[0].wardCode : "";
            }
            body.gender = gender[0];
            body.birthYear = birthYear[0];
            body.onboardingGoal = goal[0];
            body.moveInDate = ProfileUi.toApiDate(tvMoveIn.getText().toString());

            // Ô ngân sách để trống: giữ nguyên giá trị cũ
            String budgetText = etBudget.getText().toString().trim();
            if (!budgetText.isEmpty()) {
                long[] budget = ProfileUi.parseBudget(budgetText);
                if (budget == null) {
                    AppNotificationHelper.showError(this, "Ngân sách chưa đúng, ví dụ: 2–4 triệu");
                    return;
                }
                body.budgetMin = budget[0];
                body.budgetMax = budget[1];
            }

            save.setEnabled(false);
            api.updateMyProfile(body).enqueue(new ApiCallback<AccountModels.Profile>(this) {
                @Override
                public void onSuccess(AccountModels.Profile profile) {
                    AppNotificationHelper.showSuccess(ProfileSubscreenActivity.this, "Đã lưu thay đổi");
                    new SessionManager(ProfileSubscreenActivity.this).saveDisplayName(profile.displayName);
                    save.setEnabled(true);
                    onBackPressed();
                }

                @Override
                public void onError(String message) {
                    save.setEnabled(true);
                    AppNotificationHelper.showError(ProfileSubscreenActivity.this, message);
                }
            });
        });
    }

    // Tên người đang đăng nhập ở màn Xác minh & an toàn và cuối màn Quyền riêng tư
    private void showAccountName(View root) {
        String name = new SessionManager(this).getDisplayName();
        if (name == null) return;
        TextView verifiedSub = root.findViewById(R.id.tv_verified_sub);
        if (verifiedSub != null) verifiedSub.setText(getString(R.string.verified_sub_name, name));
        TextView footer = root.findViewById(R.id.tv_footer_account);
        if (footer != null) footer.setText(getString(R.string.footer_account_name, name));
    }

    private void stylePill(TextView pill, boolean selected) {
        pill.setBackgroundResource(selected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getColor(selected ? R.color.text_white : R.color.text_title));
    }

    // ---------- Đổi mật khẩu (layout_change_password) ----------

    private void bindChangePassword(View root) {
        View save = root.findViewById(R.id.btn_save_password);
        if (save == null) return;
        EditText etCurrent = root.findViewById(R.id.et_current_pass);
        EditText etNew = root.findViewById(R.id.et_new_pass);
        EditText etConfirm = root.findViewById(R.id.et_confirm_pass);
        for (EditText field : new EditText[]{etCurrent, etNew, etConfirm}) field.setText("");
        bindPasswordToggle(root.findViewById(R.id.iv_eye1), etCurrent);
        bindPasswordToggle(root.findViewById(R.id.iv_eye2), etNew);
        bindPasswordToggle(root.findViewById(R.id.iv_eye3), etConfirm);

        save.setOnClickListener(ignored -> {
            String current = etCurrent.getText().toString();
            String next = etNew.getText().toString();
            String error = null;
            if (current.isEmpty()) error = "Vui lòng nhập mật khẩu hiện tại";
            else if (next.length() < 8) error = "Mật khẩu mới tối thiểu 8 ký tự";
            else if (!next.matches(".*[a-z].*") || !next.matches(".*[A-Z].*") || !next.matches(".*\\d.*"))
                error = "Mật khẩu mới cần có chữ hoa, chữ thường và số";
            else if (!next.equals(etConfirm.getText().toString())) error = "Hai mật khẩu mới không trùng nhau";
            if (error != null) {
                AppNotificationHelper.showError(this, error);
                return;
            }

            save.setEnabled(false);
            api.changePassword(new AccountModels.ChangePasswordRequest(current, next))
                    .enqueue(new ApiCallback<AccountModels.AuthResult>(this) {
                        @Override
                        public void onSuccess(AccountModels.AuthResult result) {
                            // Server đã đăng xuất các thiết bị khác và cấp token mới cho máy này
                            new SessionManager(ProfileSubscreenActivity.this)
                                    .saveTokens(result.accessToken, result.refreshToken);
                            AppNotificationHelper.showSuccess(ProfileSubscreenActivity.this, "Đã đổi mật khẩu");
                            save.setEnabled(true);
                            onBackPressed();
                        }

                        @Override
                        public void onError(String message) {
                            save.setEnabled(true);
                            AppNotificationHelper.showError(ProfileSubscreenActivity.this, message);
                        }
                    });
        });
    }

    private void bindPasswordToggle(View eye, EditText field) {
        if (eye == null || field == null) return;
        eye.setOnClickListener(ignored -> {
            boolean hidden = (field.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD;
            field.setInputType(InputType.TYPE_CLASS_TEXT | (hidden
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            field.setSelection(field.getText().length());
        });
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
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> AuthFlow.logout(this))
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
}