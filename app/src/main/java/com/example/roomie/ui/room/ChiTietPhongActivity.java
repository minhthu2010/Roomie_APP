package com.example.roomie.ui.room;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.MatchingModels;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.matching.ChiTietTuongThichPhongActivity;
import com.example.roomie.ui.matching.KetQuaTuongThichActivity;
import com.example.roomie.ui.profile.HoSoNguoiDangActivity;
import com.example.roomie.ui.quiz.QuizUi;
import com.example.roomie.ui.report.BaoCaoBaiDangActivity;
import com.example.roomie.ui.saved.SaveToggle;
import com.example.roomie.ui.search.RoomFormat;

import java.util.Locale;

/** Chi tiết phòng. Extra bắt buộc: EXTRA_POST_ID. */
public class ChiTietPhongActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";

    public static Intent intent(Context context, String postId) {
        return new Intent(context, ChiTietPhongActivity.class).putExtra(EXTRA_POST_ID, postId);
    }

    private String postId;
    private DiscoveryApi api;
    private RoomModels.RoomDetail room;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chi_tiet_phong);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        api = ApiClient.create(this, DiscoveryApi.class);
        postId = getIntent().getStringExtra(EXTRA_POST_ID);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Ảnh bìa và "Xem tất cả ảnh" -> Thư viện ảnh
        View.OnClickListener openGallery = v -> startActivity(ThuVienAnhPhongActivity.intent(this, postId, 0));
        findViewById(R.id.btn_view_all_photos).setOnClickListener(openGallery);
        findViewById(R.id.img_cover).setOnClickListener(openGallery);

        // Nhấn vào địa chỉ phòng -> Mở màn hình Vị trí phòng (bản đồ làm ở nhánh feature/c-map)
        findViewById(R.id.layout_address_location).setOnClickListener(v ->
                startActivity(new Intent(this, ViTriPhongActivity.class).putExtra(EXTRA_POST_ID, postId)));

        // Tương thích với phòng / với chủ phòng
        findViewById(R.id.layout_room_compatibility).setOnClickListener(v ->
                startActivity(ChiTietTuongThichPhongActivity.intent(this, postId)));
        findViewById(R.id.layout_host_compatibility).setOnClickListener(v ->
                startActivity(KetQuaTuongThichActivity.intent(this, postId)));

        // Chủ bài đăng -> Hồ sơ người đăng
        findViewById(R.id.layout_host_card).setOnClickListener(v -> {
            if (room != null && room.owner != null) {
                startActivity(HoSoNguoiDangActivity.intent(this, room.owner.userId));
            }
        });

        // Báo cáo bài đăng
        findViewById(R.id.btn_report).setOnClickListener(v ->
                startActivity(new Intent(this, BaoCaoBaiDangActivity.class).putExtra(EXTRA_POST_ID, postId)));

        // Lưu phòng
        ImageView imgFavorite = findViewById(R.id.img_favorite);
        findViewById(R.id.btn_favorite).setOnClickListener(v -> {
            if (room == null) return;
            SaveToggle.toggle(this, postId, room.isSaved, imgFavorite, saved -> room.isSaved = saved);
        });

        // Liên hệ
        findViewById(R.id.btn_contact_zalo).setOnClickListener(v -> {
            if (room != null) ContactActions.zalo(this, postId, room.contact);
        });
        findViewById(R.id.btn_call_phone).setOnClickListener(v -> {
            if (room != null) ContactActions.call(this, postId, room.contact);
        });

        if (postId == null) {
            AppNotificationHelper.showError(this, getString(R.string.match_missing_post));
            return;
        }
        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompatibility();
    }

    // ---------- Chi tiết ----------

    private void loadDetail() {
        api.getRoomDetail(postId).enqueue(new ApiCallback<RoomModels.RoomDetail>(this) {
            @Override
            public void onSuccess(RoomModels.RoomDetail data) {
                if (data == null) return;
                room = data;
                render();
            }

            @Override
            public void onError(String message) {
                AppNotificationHelper.showError(ChiTietPhongActivity.this, message);
            }
        });
    }

    private void render() {
        // Ảnh
        int photoCount = room.images.size();
        ImageLoader.load(findViewById(R.id.img_cover),
                photoCount > 0 ? room.images.get(0).url : null, R.drawable.img_detail_room);
        TextView tvPhotoCount = findViewById(R.id.tv_photo_count);
        tvPhotoCount.setVisibility(photoCount > 0 ? View.VISIBLE : View.GONE);
        tvPhotoCount.setText(getString(R.string.detail_photo_count, 1, photoCount));
        ((TextView) findViewById(R.id.tv_view_all_photos)).setText(photoCount > 0
                ? getString(R.string.detail_view_all_photos, photoCount) : getString(R.string.gallery_empty));

        SaveToggle.bindIcon(findViewById(R.id.img_favorite), room.isSaved);

        // Giá, tiêu đề, địa chỉ
        setText(R.id.tv_price, getString(R.string.detail_price_month, RoomFormat.money(room.monthlyRent)));
        setText(R.id.tv_title, room.title);
        String address = room.addressText != null ? room.addressText : "";
        if (room.landmark != null) {
            String landmark = room.landmark.shortName != null ? room.landmark.shortName : room.landmark.name;
            address = getString(R.string.detail_address_landmark, address, landmark);
        }
        setText(R.id.tv_address, address);

        // Tiện ích
        LinearLayout amenities = findViewById(R.id.layout_amenities);
        amenities.removeAllViews();
        for (RoomModels.Amenity a : room.amenities) {
            amenities.addView(chip(amenities, a.name, amenityIcon(a.iconKey)));
        }

        // Mô tả và thông tin
        setText(R.id.tv_description, room.description);
        int occupants = room.currentOccupants != null ? room.currentOccupants : 0;
        setText(R.id.tv_val_roommates, occupants > 0
                ? getString(R.string.detail_people_with_current, room.seekingRoommates, occupants)
                : getString(R.string.detail_people, room.seekingRoommates));
        setText(R.id.tv_val_move_in, room.availableFrom != null
                ? formatDate(room.availableFrom) : getString(R.string.detail_not_set));
        setText(R.id.tv_val_area, room.areaM2 != null
                ? getString(R.string.detail_area_value, RoomFormat.km(room.areaM2)) : getString(R.string.detail_not_set));
        setText(R.id.tv_val_deposit, room.depositAmount == null ? getString(R.string.detail_not_set)
                : room.depositAmount == 0 ? getString(R.string.detail_no_deposit)
                : getString(R.string.detail_price, RoomFormat.money(room.depositAmount)));

        showSection(R.id.tv_bills_title, R.id.tv_bills, room.utilityCostNote);
        showSection(R.id.tv_expectations_title, R.id.tv_expectations, room.roommateExpectations);

        // Ưu tiên bạn cùng phòng
        LinearLayout prefs = findViewById(R.id.layout_prefs);
        prefs.removeAllViews();
        prefs.addView(selectedChip(prefs, getString("female".equals(room.preferredGender) ? R.string.chip_gender_female
                : "male".equals(room.preferredGender) ? R.string.chip_gender_male : R.string.pref_any_gender)));
        if ("student".equals(room.preferredOccupation)) {
            prefs.addView(selectedChip(prefs, getString(R.string.pref_occupation_student)));
        } else if ("worker".equals(room.preferredOccupation)) {
            prefs.addView(selectedChip(prefs, getString(R.string.pref_occupation_worker)));
        }
        if (room.smokingAllowed != null) {
            prefs.addView(selectedChip(prefs, getString(room.smokingAllowed ? R.string.pref_smoking_ok : R.string.pref_no_smoking)));
        }
        if (room.petsAllowed != null) {
            prefs.addView(selectedChip(prefs, getString(room.petsAllowed ? R.string.pref_pets_ok : R.string.pref_no_pets)));
        }

        // Chủ bài đăng
        if (room.owner != null) {
            setText(R.id.tv_host_name, room.owner.displayName);
            String since = QuizUi.formatDate(room.owner.memberSince);
            String year = since != null ? since.substring(since.length() - 4) : "";
            String occupation = occupationLabel(room.owner.occupation);
            setText(R.id.tv_host_role, occupation != null
                    ? getString(R.string.detail_host_role, occupation, year)
                    : getString(R.string.detail_host_role_short, year));
            ImageLoader.load(findViewById(R.id.img_avatar), room.owner.avatarUrl, R.drawable.img_avatar_host);
        }
    }

    private void loadCompatibility() {
        TextView tvRoomRate = findViewById(R.id.tv_room_compat_rate);
        TextView tvHostRate = findViewById(R.id.tv_host_compat_rate);
        if (postId == null) return;
        api.getCompatibility(postId).enqueue(new ApiCallback<MatchingModels.Compatibility>(this) {
            @Override
            public void onSuccess(MatchingModels.Compatibility data) {
                if (data == null) return;
                tvRoomRate.setText(percentText(data.room.overall));
                tvHostRate.setText(percentText(data.host.overall));
                ((ProgressBar) findViewById(R.id.pb_room_compat))
                        .setProgress(data.room.overall != null ? data.room.overall : 0);
                ((ProgressBar) findViewById(R.id.pb_host_compat))
                        .setProgress(data.host.overall != null ? data.host.overall : 0);
            }

            @Override
            public void onError(String message) {
                // Giữ "--%"; mở màn tương thích sẽ thấy lỗi chi tiết
            }
        });
    }

    // ---------- Tiện ích hiển thị ----------

    private void setText(int id, String text) {
        ((TextView) findViewById(id)).setText(text != null ? text : "");
    }

    private void showSection(int titleId, int textId, String text) {
        int visibility = text == null || text.isEmpty() ? View.GONE : View.VISIBLE;
        findViewById(titleId).setVisibility(visibility);
        findViewById(textId).setVisibility(visibility);
        setText(textId, text);
    }

    private View chip(LinearLayout parent, String text, int iconRes) {
        View chip = LayoutInflater.from(this).inflate(R.layout.item_detail_chip, parent, false);
        ((TextView) chip.findViewById(R.id.tv_chip_text)).setText(text);
        ImageView icon = chip.findViewById(R.id.img_chip_icon);
        if (iconRes != 0) {
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(iconRes);
            icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_primary)));
        }
        return chip;
    }

    private View selectedChip(LinearLayout parent, String text) {
        View chip = chip(parent, text, 0);
        chip.setBackgroundResource(R.drawable.bg_chip_selected);
        float d = getResources().getDisplayMetrics().density;
        chip.setPadding((int) (16 * d), (int) (8 * d), (int) (16 * d), (int) (8 * d));
        TextView tv = chip.findViewById(R.id.tv_chip_text);
        tv.setTextColor(getColor(R.color.white));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return chip;
    }

    // icon_key trong bảng amenities trùng tên drawable (ic_ac, ic_kitchen...); chưa có thì dùng dấu tích
    private int amenityIcon(String iconKey) {
        int res = iconKey != null ? getResources().getIdentifier(iconKey, "drawable", getPackageName()) : 0;
        return res != 0 ? res : R.drawable.ic_check_small;
    }

    private String occupationLabel(String occupation) {
        if ("student".equals(occupation)) return getString(R.string.occupation_student);
        if ("worker".equals(occupation)) return getString(R.string.occupation_worker);
        return null;
    }

    /** "2026-11-09" -> "09/11/2026" */
    private static String formatDate(String ymd) {
        String[] p = ymd.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : ymd;
    }

    private static String percentText(Integer percent) {
        return percent == null ? "--%" : String.format(Locale.getDefault(), "%d%%", percent);
    }
}
