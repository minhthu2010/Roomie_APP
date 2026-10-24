package com.example.roomie.ui.report;

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
import com.example.roomie.ui.home.HomeActivity;
import com.example.roomie.ui.room.ChiTietPhongActivity;

public class DaGuiBaoCaoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_da_gui_bao_cao);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View.OnClickListener openPostListener = v -> {
            Intent intent = new Intent(DaGuiBaoCaoActivity.this, ChiTietPhongActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        View headerBar = findViewById(R.id.header_bar);
        if (headerBar != null) {
            TextView tvTitle = headerBar.findViewById(R.id.tv_header_title);
            if (tvTitle != null) {
                tvTitle.setText(R.string.title_report_submitted);
            }
            View btnBack = headerBar.findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(openPostListener);
            }
        }

        View btnBackToPost = findViewById(R.id.btn_back_to_post);
        if (btnBackToPost != null) {
            btnBackToPost.setOnClickListener(openPostListener);
        }

        View btnBackToExplore = findViewById(R.id.btn_back_to_explore);
        if (btnBackToExplore != null) {
            btnBackToExplore.setOnClickListener(v -> {
                Intent intent = new Intent(DaGuiBaoCaoActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }
}