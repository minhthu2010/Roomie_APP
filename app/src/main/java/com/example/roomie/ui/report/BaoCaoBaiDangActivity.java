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

public class BaoCaoBaiDangActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bao_cao_bai_dang);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View headerBar = findViewById(R.id.header_bar);
        if (headerBar != null) {
            TextView tvTitle = headerBar.findViewById(R.id.tv_header_title);
            if (tvTitle != null) {
                tvTitle.setText(R.string.title_report_post);
            }
            View btnBack = headerBar.findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
        }

        View btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        View btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoBaiDangActivity.this, DaGuiBaoCaoActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}