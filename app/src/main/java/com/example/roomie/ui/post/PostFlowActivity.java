package com.example.roomie.ui.post;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;

public class PostFlowActivity extends AppCompatActivity {

    public static final String EXTRA_STEP = "EXTRA_STEP";

    private int currentStep = 3;

    private ViewFlipper viewFlipper;
    private TextView tvHeaderCaption;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;
    private AppCompatButton btnReadAll;
    private LinearLayout progressSection;
    private LinearLayout bottomBar;
    private TextView tvStepTitle;
    private TextView tvStepPercent;
    private ProgressBar progressBar;
    private AppCompatButton btnBack;
    private AppCompatButton btnNext;
    private ImageButton btnBackIcon;

    private AppCompatButton btnManageStatus;
    private AppCompatButton btnEditPost;
    private AppCompatButton btnEditPreview;
    private AppCompatButton btnPublishRoom;

    private RelativeLayout optStatusSearching;
    private RelativeLayout optStatusFound;
    private RelativeLayout optStatusHidden;

    private AppCompatButton btnCancelDelete;
    private AppCompatButton btnConfirmDelete;

    private AppCompatButton btnContinueDraft1;
    private AppCompatButton btnContinueDraft2;

    private RelativeLayout cardUserProfile;
    private RelativeLayout menuVerificationSafety;
    private RelativeLayout menuNotifications;
    private RelativeLayout menuSettings;
    private RelativeLayout btnLogoutRow;

    private AppCompatButton btnCancelEditProfile;
    private AppCompatButton btnSaveProfileChanges;

    private AppCompatButton btnReverifyPhone;
    private AppCompatButton btnSendEmailCode;

    private RelativeLayout menuChangePasswordItem;
    private RelativeLayout menuNotifSettingsItem;
    private RelativeLayout menuPrivacyAccountItem;
    private RelativeLayout menuHelpItem;
    private RelativeLayout menuFeedbackItem;
    private RelativeLayout btnLogoutSettings;

    private AppCompatButton btnCancelChangePass;
    private AppCompatButton btnSavePassword;

    private RelativeLayout btnDeviceNotifSettings;
    private AppCompatButton btnContactSupport;
    private TextView btnLinkSendFeedback;

    private AppCompatButton btnCancelFeedback;
    private AppCompatButton btnSubmitFeedbackFinal;

    private RelativeLayout menuChangePassPrivItem;
    private AppCompatButton btnStayLoggedIn;
    private AppCompatButton btnConfirmLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_flow);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent() != null && getIntent().hasExtra(EXTRA_STEP)) {
            currentStep = getIntent().getIntExtra(EXTRA_STEP, 3);
        }

        initViews();
        setupListeners();
        setupBottomNavListeners();
        updateStepUI();
    }

    private void initViews() {
        viewFlipper = findViewById(R.id.view_flipper);
        tvHeaderCaption = findViewById(R.id.tv_header_caption);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle);
        btnReadAll = findViewById(R.id.btn_read_all);
        progressSection = findViewById(R.id.progress_section);
        bottomBar = findViewById(R.id.bottom_bar);
        tvStepTitle = findViewById(R.id.tv_step_title);
        tvStepPercent = findViewById(R.id.tv_step_percent);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next);
        btnBackIcon = findViewById(R.id.btn_back_icon);

        btnManageStatus = findViewById(R.id.btn_manage_status);
        btnEditPost = findViewById(R.id.btn_edit_post);
        btnEditPreview = findViewById(R.id.btn_edit_preview);
        btnPublishRoom = findViewById(R.id.btn_publish_room);

        optStatusSearching = findViewById(R.id.opt_status_searching);
        optStatusFound = findViewById(R.id.opt_status_found);
        optStatusHidden = findViewById(R.id.opt_status_hidden);

        btnCancelDelete = findViewById(R.id.btn_cancel_delete);
        btnConfirmDelete = findViewById(R.id.btn_confirm_delete);

        btnContinueDraft1 = findViewById(R.id.btn_continue_draft1);
        btnContinueDraft2 = findViewById(R.id.btn_continue_draft2);

        cardUserProfile = findViewById(R.id.card_user_profile);
        menuVerificationSafety = findViewById(R.id.menu_verification_safety);
        menuNotifications = findViewById(R.id.menu_notifications);
        menuSettings = findViewById(R.id.menu_settings);
        btnLogoutRow = findViewById(R.id.btn_logout_row);

        btnCancelEditProfile = findViewById(R.id.btn_cancel_edit_profile);
        btnSaveProfileChanges = findViewById(R.id.btn_save_profile_changes);

        btnReverifyPhone = findViewById(R.id.btn_reverify_phone);
        btnSendEmailCode = findViewById(R.id.btn_send_email_code);

        menuChangePasswordItem = findViewById(R.id.menu_change_password_item);
        menuNotifSettingsItem = findViewById(R.id.menu_notif_settings_item);
        menuPrivacyAccountItem = findViewById(R.id.menu_privacy_account_item);
        menuHelpItem = findViewById(R.id.menu_help_item);
        menuFeedbackItem = findViewById(R.id.menu_feedback_item);
        btnLogoutSettings = findViewById(R.id.btn_logout_settings);

        btnCancelChangePass = findViewById(R.id.btn_cancel_change_pass);
        btnSavePassword = findViewById(R.id.btn_save_password);

        btnDeviceNotifSettings = findViewById(R.id.btn_device_notif_settings);
        btnContactSupport = findViewById(R.id.btn_contact_support);
        btnLinkSendFeedback = findViewById(R.id.btn_link_send_feedback);

        btnCancelFeedback = findViewById(R.id.btn_cancel_feedback);
        btnSubmitFeedbackFinal = findViewById(R.id.btn_submit_feedback_final);

        menuChangePassPrivItem = findViewById(R.id.menu_change_pass_priv_item);
        btnStayLoggedIn = findViewById(R.id.btn_stay_logged_in);
        btnConfirmLogout = findViewById(R.id.btn_confirm_logout);
    }

    private void setupBottomNavListeners() {
        int[] profileNavIds = {
            R.id.nav_item_profile_1,
            R.id.nav_item_profile_2,
            R.id.nav_item_profile_4,
            R.id.nav_item_profile_5,
            R.id.nav_item_profile_6,
            R.id.nav_item_profile_7,
            R.id.nav_item_profile_8,
            R.id.nav_item_profile_9,
            R.id.nav_item_profile_10
        };
        for (int id : profileNavIds) {
            View navProfile = findViewById(id);
            if (navProfile != null) {
                navProfile.setOnClickListener(v -> {
                    currentStep = 13;
                    updateStepUI();
                });
            }
        }

        int[] postsNavIds = {
            R.id.nav_item_posts_1,
            R.id.nav_item_posts_2,
            R.id.nav_item_posts_4,
            R.id.nav_item_posts_5,
            R.id.nav_item_posts_6,
            R.id.nav_item_posts_7,
            R.id.nav_item_posts_8,
            R.id.nav_item_posts_9,
            R.id.nav_item_posts_10
        };
        for (int id : postsNavIds) {
            View navPosts = findViewById(id);
            if (navPosts != null) {
                navPosts.setOnClickListener(v -> {
                    currentStep = 8;
                    updateStepUI();
                });
            }
        }
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (currentStep < 8) {
                currentStep++;
                updateStepUI();
            } else if (currentStep == 10) {
                currentStep = 8;
                updateStepUI();
                Toast.makeText(this, "Đã lưu trạng thái bài đăng!", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> handleBackNavigation());
        btnBackIcon.setOnClickListener(v -> handleBackNavigation());

        if (btnManageStatus != null) {
            btnManageStatus.setOnClickListener(v -> {
                currentStep = 10;
                updateStepUI();
            });
        }

        if (btnEditPost != null) {
            btnEditPost.setOnClickListener(v -> {
                currentStep = 9;
                updateStepUI();
            });
        }

        if (btnEditPreview != null) {
            btnEditPreview.setOnClickListener(v -> {
                currentStep = 6;
                updateStepUI();
            });
        }

        if (btnPublishRoom != null) {
            btnPublishRoom.setOnClickListener(v -> {
                currentStep = 7;
                updateStepUI();
                Toast.makeText(this, "Đăng phòng thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        if (optStatusHidden != null) {
            optStatusHidden.setOnClickListener(v -> {
                currentStep = 11;
                updateStepUI();
            });
        }

        if (optStatusFound != null) {
            optStatusFound.setOnClickListener(v -> {
                currentStep = 11;
                updateStepUI();
            });
        }

        if (optStatusSearching != null) {
            optStatusSearching.setOnClickListener(v ->
                Toast.makeText(this, "Trạng thái hiện tại: Đang tìm", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnCancelDelete != null) {
            btnCancelDelete.setOnClickListener(v -> {
                currentStep = 10;
                updateStepUI();
            });
        }

        if (btnConfirmDelete != null) {
            btnConfirmDelete.setOnClickListener(v -> {
                currentStep = 12;
                updateStepUI();
                Toast.makeText(this, "Đã xóa bài đăng thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnContinueDraft1 != null) {
            btnContinueDraft1.setOnClickListener(v -> {
                currentStep = 9;
                updateStepUI();
            });
        }

        if (btnContinueDraft2 != null) {
            btnContinueDraft2.setOnClickListener(v -> {
                currentStep = 9;
                updateStepUI();
            });
        }

        if (cardUserProfile != null) {
            cardUserProfile.setOnClickListener(v -> {
                currentStep = 15;
                updateStepUI();
            });
        }

        if (menuVerificationSafety != null) {
            menuVerificationSafety.setOnClickListener(v -> {
                currentStep = 16;
                updateStepUI();
            });
        }

        if (menuNotifications != null) {
            menuNotifications.setOnClickListener(v -> {
                currentStep = 14;
                updateStepUI();
            });
        }

        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                currentStep = 17;
                updateStepUI();
            });
        }

        if (btnLogoutRow != null) {
            btnLogoutRow.setOnClickListener(v ->
                Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnLogoutSettings != null) {
            btnLogoutSettings.setOnClickListener(v ->
                Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnReadAll != null) {
            btnReadAll.setOnClickListener(v ->
                Toast.makeText(this, "Đã đánh dấu đọc tất cả thông báo!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnCancelEditProfile != null) {
            btnCancelEditProfile.setOnClickListener(v -> {
                currentStep = 13;
                updateStepUI();
            });
        }

        if (btnSaveProfileChanges != null) {
            btnSaveProfileChanges.setOnClickListener(v -> {
                currentStep = 13;
                updateStepUI();
                Toast.makeText(this, "Lưu thay đổi hồ sơ thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnReverifyPhone != null) {
            btnReverifyPhone.setOnClickListener(v ->
                Toast.makeText(this, "Gửi yêu cầu xác minh lại số điện thoại!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnSendEmailCode != null) {
            btnSendEmailCode.setOnClickListener(v ->
                Toast.makeText(this, "Đã gửi mã xác minh đến email!", Toast.LENGTH_SHORT).show()
            );
        }

        if (menuChangePasswordItem != null) {
            menuChangePasswordItem.setOnClickListener(v -> {
                currentStep = 18;
                updateStepUI();
            });
        }

        if (menuNotifSettingsItem != null) {
            menuNotifSettingsItem.setOnClickListener(v -> {
                currentStep = 19;
                updateStepUI();
            });
        }

        if (menuPrivacyAccountItem != null) {
            menuPrivacyAccountItem.setOnClickListener(v -> {
                currentStep = 22;
                updateStepUI();
            });
        }

        if (menuHelpItem != null) {
            menuHelpItem.setOnClickListener(v -> {
                currentStep = 20;
                updateStepUI();
            });
        }

        if (menuFeedbackItem != null) {
            menuFeedbackItem.setOnClickListener(v -> {
                currentStep = 21;
                updateStepUI();
            });
        }

        if (btnCancelChangePass != null) {
            btnCancelChangePass.setOnClickListener(v -> {
                currentStep = 17;
                updateStepUI();
            });
        }

        if (btnSavePassword != null) {
            btnSavePassword.setOnClickListener(v -> {
                currentStep = 17;
                updateStepUI();
                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDeviceNotifSettings != null) {
            btnDeviceNotifSettings.setOnClickListener(v ->
                Toast.makeText(this, "Mở cài đặt thông báo của thiết bị!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnContactSupport != null) {
            btnContactSupport.setOnClickListener(v ->
                Toast.makeText(this, "Mở kênh Liên hệ hỗ trợ!", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnLinkSendFeedback != null) {
            btnLinkSendFeedback.setOnClickListener(v -> {
                currentStep = 21;
                updateStepUI();
            });
        }

        if (btnCancelFeedback != null) {
            btnCancelFeedback.setOnClickListener(v -> {
                currentStep = 17;
                updateStepUI();
            });
        }

        if (btnSubmitFeedbackFinal != null) {
            btnSubmitFeedbackFinal.setOnClickListener(v -> {
                currentStep = 17;
                updateStepUI();
                Toast.makeText(this, "Gửi phản hồi thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        if (menuChangePassPrivItem != null) {
            menuChangePassPrivItem.setOnClickListener(v -> {
                currentStep = 18;
                updateStepUI();
            });
        }

        if (btnStayLoggedIn != null) {
            btnStayLoggedIn.setOnClickListener(v -> {
                currentStep = 22;
                updateStepUI();
            });
        }

        if (btnConfirmLogout != null) {
            btnConfirmLogout.setOnClickListener(v ->
                Toast.makeText(this, "Đã đăng xuất tài khoản thành công!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void handleBackNavigation() {
        if (currentStep > 3) {
            if (currentStep == 9 || currentStep == 10 || currentStep == 12 || currentStep == 13) {
                currentStep = 8;
            } else if (currentStep == 11) {
                currentStep = 10;
            } else if (currentStep == 14 || currentStep == 15 || currentStep == 16 || currentStep == 17) {
                currentStep = 13;
            } else if (currentStep == 18 || currentStep == 19 || currentStep == 21 || currentStep == 22) {
                currentStep = 17;
            } else if (currentStep == 20) {
                currentStep = 17;
            } else {
                currentStep--;
            }
            updateStepUI();
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void updateStepUI() {
        btnReadAll.setVisibility(View.GONE);
        View bottomNavEdit = findViewById(R.id.bottom_nav_bar_edit);
        if (bottomNavEdit != null) {
            bottomNavEdit.setVisibility(View.GONE);
        }

        if (currentStep == 3) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_create_post);
            tvHeaderSubtitle.setText(R.string.auto_save_draft);
            tvHeaderSubtitle.setVisibility(View.VISIBLE);
            progressSection.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tvStepTitle.setText(R.string.step_progress_text);
            tvStepPercent.setText(R.string.step_progress_percent);
            progressBar.setProgress(60);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setText(R.string.btn_back);
            btnNext.setText(R.string.btn_next);
            viewFlipper.setDisplayedChild(0);
        } else if (currentStep == 4) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_create_post);
            tvHeaderSubtitle.setText(R.string.auto_save_draft);
            tvHeaderSubtitle.setVisibility(View.VISIBLE);
            progressSection.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tvStepTitle.setText(R.string.step4_progress_text);
            tvStepPercent.setText(R.string.step4_progress_percent);
            progressBar.setProgress(80);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setText(R.string.btn_back);
            btnNext.setText(R.string.btn_next);
            viewFlipper.setDisplayedChild(1);
        } else if (currentStep == 5) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_create_post);
            tvHeaderSubtitle.setText(R.string.auto_save_draft);
            tvHeaderSubtitle.setVisibility(View.VISIBLE);
            progressSection.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tvStepTitle.setText(R.string.step5_progress_text);
            tvStepPercent.setText(R.string.step5_progress_percent);
            progressBar.setProgress(100);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setText(R.string.btn_back);
            btnNext.setText(R.string.btn_preview);
            viewFlipper.setDisplayedChild(2);
        } else if (currentStep == 6) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_preview_post);
            tvHeaderSubtitle.setText(R.string.subtitle_preview_post);
            tvHeaderSubtitle.setVisibility(View.VISIBLE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setText(R.string.btn_edit);
            btnNext.setText(R.string.btn_publish);
            viewFlipper.setDisplayedChild(3);
        } else if (currentStep == 7) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_post_success_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.GONE);
            btnNext.setText(R.string.btn_my_posts);
            viewFlipper.setDisplayedChild(4);
        } else if (currentStep == 8) {
            tvHeaderCaption.setVisibility(View.VISIBLE);
            tvHeaderCaption.setText(R.string.caption_posted);
            tvHeaderTitle.setText(R.string.title_my_post_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(5);
        } else if (currentStep == 9) {
            tvHeaderCaption.setVisibility(View.VISIBLE);
            tvHeaderCaption.setText(R.string.caption_draft_saved);
            tvHeaderTitle.setText(R.string.title_edit_post);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            if (bottomNavEdit != null) {
                bottomNavEdit.setVisibility(View.VISIBLE);
            }
            viewFlipper.setDisplayedChild(6);
        } else if (currentStep == 10) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_manage_status);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setText(R.string.btn_cancel);
            btnNext.setText(R.string.btn_save_status);
            viewFlipper.setDisplayedChild(7);
        } else if (currentStep == 11) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_delete_post_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(8);
        } else if (currentStep == 12) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_drafts_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(9);
        } else if (currentStep == 13) {
            tvHeaderCaption.setVisibility(View.VISIBLE);
            tvHeaderCaption.setText(R.string.caption_member_since);
            tvHeaderTitle.setText(R.string.title_my_profile);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(10);
        } else if (currentStep == 14) {
            tvHeaderCaption.setVisibility(View.VISIBLE);
            tvHeaderCaption.setText(R.string.caption_inbox);
            tvHeaderTitle.setText(R.string.title_notifications);
            tvHeaderSubtitle.setVisibility(View.GONE);
            btnReadAll.setVisibility(View.VISIBLE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(11);
        } else if (currentStep == 15) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_edit_profile);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(12);
        } else if (currentStep == 16) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_verification_safety);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(13);
        } else if (currentStep == 17) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_settings);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(14);
        } else if (currentStep == 18) {
            tvHeaderCaption.setVisibility(View.VISIBLE);
            tvHeaderCaption.setText(R.string.caption_account_security);
            tvHeaderTitle.setText(R.string.menu_change_password);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(15);
        } else if (currentStep == 19) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_notif_settings_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(16);
        } else if (currentStep == 20) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_help_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(17);
        } else if (currentStep == 21) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_send_feedback_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(18);
        } else if (currentStep == 22) {
            tvHeaderCaption.setVisibility(View.GONE);
            tvHeaderTitle.setText(R.string.title_privacy_account_header);
            tvHeaderSubtitle.setVisibility(View.GONE);
            progressSection.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(19);
        }
    }
}
