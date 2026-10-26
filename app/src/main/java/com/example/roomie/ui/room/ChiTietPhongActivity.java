package com.example.roomie.ui.room;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.MatchingModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.ui.matching.ChiTietTuongThichPhongActivity;
import com.example.roomie.ui.matching.KetQuaTuongThichActivity;
import com.example.roomie.ui.profile.HoSoNguoiDangActivity;
import com.example.roomie.ui.report.BaoCaoBaiDangActivity;

import java.util.Locale;

public class ChiTietPhongActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";

    public static Intent intent(Context context, String postId) {
        return new Intent(context, ChiTietPhongActivity.class).putExtra(EXTRA_POST_ID, postId);
    }

    // TODO (tuần 3): tải chi tiết phòng theo postId; hiện mới dùng cho phần tương thích
    private String postId;

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

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View viewAllPhotos = findViewById(R.id.btn_view_all_photos);
        if (viewAllPhotos != null) {
            viewAllPhotos.setOnClickListener(v ->
                startActivity(new Intent(this, ThuVienAnhPhongActivity.class))
            );
        }

        // Nhấn vào địa chỉ phòng -> Mở màn hình Vị trí phòng
        View addressLocation = findViewById(R.id.layout_address_location);
        if (addressLocation != null) {
            addressLocation.setOnClickListener(v ->
                startActivity(new Intent(this, ViTriPhongActivity.class))
            );
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);

        // Tương thích với phòng -> Mở màn hình Chi tiết tương thích với phòng
        View roomCompat = findViewById(R.id.layout_room_compatibility);
        if (roomCompat != null) {
            roomCompat.setOnClickListener(v ->
                startActivity(ChiTietTuongThichPhongActivity.intent(this, postId))
            );
        }

        // Tương thích với chủ phòng -> Mở màn hình Kết quả tương thích
        View hostCompat = findViewById(R.id.layout_host_compatibility);
        if (hostCompat != null) {
            hostCompat.setOnClickListener(v ->
                startActivity(KetQuaTuongThichActivity.intent(this, postId))
            );
        }

        // Chủ bài đăng -> Mở màn hình Hồ sơ bạn cùng phòng
        View hostCard = findViewById(R.id.layout_host_card);
        if (hostCard != null) {
            hostCard.setOnClickListener(v ->
                startActivity(new Intent(this, HoSoNguoiDangActivity.class))
            );
        }

        // Báo cáo bài đăng
        View btnReport = findViewById(R.id.btn_report);
        if (btnReport != null) {
            btnReport.setOnClickListener(v ->
                startActivity(new Intent(this, BaoCaoBaiDangActivity.class))
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompatibility();
    }

    // % tương thích với phòng và với chủ phòng (người C)
    private void loadCompatibility() {
        TextView tvRoomRate = findViewById(R.id.tv_room_compat_rate);
        TextView tvHostRate = findViewById(R.id.tv_host_compat_rate);
        if (postId == null || tvRoomRate == null || tvHostRate == null) return;
        ApiClient.create(this, DiscoveryApi.class).getCompatibility(postId)
                .enqueue(new ApiCallback<MatchingModels.Compatibility>(this) {
                    @Override
                    public void onSuccess(MatchingModels.Compatibility data) {
                        if (data == null) return;
                        tvRoomRate.setText(percentText(data.room.overall));
                        tvHostRate.setText(percentText(data.host.overall));
                    }

                    @Override
                    public void onError(String message) {
                        // Giữ "--%"; mở màn tương thích sẽ thấy lỗi chi tiết
                    }
                });
    }

    private static String percentText(Integer percent) {
        return percent == null ? "--%" : String.format(Locale.getDefault(), "%d%%", percent);
    }
}
