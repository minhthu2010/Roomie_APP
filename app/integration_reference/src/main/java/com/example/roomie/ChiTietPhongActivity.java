package com.example.roomie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.ui.matching.ChiTietTuongThichPhongActivity;
import com.example.roomie.ui.matching.KetQuaTuongThichActivity;
import com.example.roomie.ui.profile.HoSoNguoiDangActivity;

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

        // Back button
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Flag / Report button opens "Báo cáo bài đăng" screen
        View btnReport = findViewById(R.id.btn_report);
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                Intent intent = new Intent(ChiTietPhongActivity.this, BaoCaoBaiDangActivity.class);
                startActivity(intent);
            });
        }

        // Clicking host card opens "Hồ sơ người đăng" screen
        View hostCard = findViewById(R.id.layout_host_card);
        if (hostCard != null) {
            hostCard.setOnClickListener(v -> {
                Intent intent = new Intent(ChiTietPhongActivity.this, HoSoNguoiDangActivity.class);
                startActivity(intent);
            });
        }

        // Clicking "Tương thích với chủ phòng" card opens "Kết quả tương thích" screen
        View hostCompatibilityCard = findViewById(R.id.layout_host_compatibility);
        if (hostCompatibilityCard != null) {
            hostCompatibilityCard.setOnClickListener(v -> {
                Intent intent = new Intent(ChiTietPhongActivity.this, KetQuaTuongThichActivity.class);
                startActivity(intent);
            });
        }

        View roomCompatibilityCard = findViewById(R.id.layout_room_compatibility);
        if (roomCompatibilityCard != null) {
            roomCompatibilityCard.setOnClickListener(v ->
                    startActivity(new Intent(ChiTietPhongActivity.this, ChiTietTuongThichPhongActivity.class)));
        }
    }
}
