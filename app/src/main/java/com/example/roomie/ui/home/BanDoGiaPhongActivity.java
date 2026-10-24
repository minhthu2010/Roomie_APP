package com.example.roomie.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.ui.filter.BoLocActivity;
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.search.SearchActivity;

public class BanDoGiaPhongActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ban_do_gia_phong);

        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button returns to HomeActivity (Khám phá)
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Search bar click opens SearchActivity
        View searchBar = findViewById(R.id.layout_search_bar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                Intent intent = new Intent(BanDoGiaPhongActivity.this, SearchActivity.class);
                startActivity(intent);
            });
        }

        // Filter button click opens BoLocActivity
        View btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(BanDoGiaPhongActivity.this, BoLocActivity.class);
                startActivity(intent);
            });
        }

        // "Xem chi tiết" button and room info card click open ChiTietPhongActivity
        View.OnClickListener openDetailListener = v -> {
            Intent intent = new Intent(BanDoGiaPhongActivity.this, ChiTietPhongActivity.class);
            startActivity(intent);
        };

        View btnRoomDetail = findViewById(R.id.btn_room_detail);
        if (btnRoomDetail != null) {
            btnRoomDetail.setOnClickListener(openDetailListener);
        }

        View layoutRoomInfo = findViewById(R.id.layout_room_info);
        if (layoutRoomInfo != null) {
            layoutRoomInfo.setOnClickListener(openDetailListener);
        }

        // Contact Zalo & Call buttons
        View btnZalo = findViewById(R.id.btn_contact_zalo);
        if (btnZalo != null) {
            btnZalo.setOnClickListener(v ->
                Toast.makeText(BanDoGiaPhongActivity.this, "Mở liên hệ Zalo...", Toast.LENGTH_SHORT).show()
            );
        }

        View btnCall = findViewById(R.id.btn_call_phone);
        if (btnCall != null) {
            btnCall.setOnClickListener(v ->
                Toast.makeText(BanDoGiaPhongActivity.this, "Đang kết nối cuộc gọi...", Toast.LENGTH_SHORT).show()
            );
        }
    }
}