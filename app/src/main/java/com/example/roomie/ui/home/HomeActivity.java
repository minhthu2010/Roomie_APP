package com.example.roomie.ui.home;

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
import com.example.roomie.ui.filter.BoLocActivity;
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.search.SearchActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Filter button click opens "Bộ lọc phòng" screen
        View btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, BoLocActivity.class);
                startActivity(intent);
            });
        }

        // Search bar click opens "Tìm kiếm phòng" screen
        View searchBar = findViewById(R.id.layout_search_bar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
                startActivity(intent);
            });
        }

        // "Xem bản đồ" button click opens "Bản đồ giá phòng" screen
        View btnViewMap = findViewById(R.id.btn_view_map);
        if (btnViewMap != null) {
            btnViewMap.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, BanDoGiaPhongActivity.class);
                startActivity(intent);
            });
        }

        // Clicking any room card opens "Chi tiết phòng" screen.
        // TODO (C): thay 3 thẻ viết cứng bằng feed thật; tạm gắn 3 phòng mẫu trong seed để test tương thích.
        int[] cardIds = {R.id.card_room1, R.id.card_room2, R.id.card_room3};
        for (int i = 0; i < cardIds.length; i++) {
            View card = findViewById(cardIds[i]);
            String postId = DEMO_POST_IDS[i];
            if (card != null) {
                card.setOnClickListener(v -> startActivity(ChiTietPhongActivity.intent(this, postId)));
            }
        }
    }

    private static final String[] DEMO_POST_IDS = {
            "10000000-0000-0000-0000-000000000001",
            "10000000-0000-0000-0000-000000000002",
            "10000000-0000-0000-0000-000000000003",
    };
}