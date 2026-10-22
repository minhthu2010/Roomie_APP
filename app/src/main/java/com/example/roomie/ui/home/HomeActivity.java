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

        // Clicking any room card opens "Chi tiết phòng" screen
        View.OnClickListener openDetailListener = v -> {
            Intent intent = new Intent(HomeActivity.this, ChiTietPhongActivity.class);
            startActivity(intent);
        };

        View card1 = findViewById(R.id.card_room1);
        if (card1 != null) card1.setOnClickListener(openDetailListener);

        View card2 = findViewById(R.id.card_room2);
        if (card2 != null) card2.setOnClickListener(openDetailListener);

        View card3 = findViewById(R.id.card_room3);
        if (card3 != null) card3.setOnClickListener(openDetailListener);
    }
}