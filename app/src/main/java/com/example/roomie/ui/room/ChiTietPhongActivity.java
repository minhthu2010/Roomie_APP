package com.example.roomie.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.ui.matching.ChiTietTuongThichPhongActivity;
import com.example.roomie.ui.matching.KetQuaTuongThichActivity;
import com.example.roomie.ui.profile.HoSoNguoiDangActivity;
import com.example.roomie.ui.report.BaoCaoBaiDangActivity;

public class ChiTietPhongActivity extends AppCompatActivity {

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

        // Tương thích với phòng -> Mở màn hình Chi tiết tương thích với phòng
        View roomCompat = findViewById(R.id.layout_room_compatibility);
        if (roomCompat != null) {
            roomCompat.setOnClickListener(v ->
                startActivity(new Intent(this, ChiTietTuongThichPhongActivity.class))
            );
        }

        // Tương thích với chủ phòng -> Mở màn hình Kết quả tương thích
        View hostCompat = findViewById(R.id.layout_host_compatibility);
        if (hostCompat != null) {
            hostCompat.setOnClickListener(v ->
                startActivity(new Intent(this, KetQuaTuongThichActivity.class))
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
}
