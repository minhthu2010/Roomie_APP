package com.example.roomie.ui.filter;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;

public class BoLocActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bo_loc);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnReset = findViewById(R.id.btn_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> finish());
        }

        View btnClearFilter = findViewById(R.id.btn_clear_filter);
        if (btnClearFilter != null) {
            btnClearFilter.setOnClickListener(v -> finish());
        }

        View btnApplyFilter = findViewById(R.id.btn_apply_filter);
        if (btnApplyFilter != null) {
            btnApplyFilter.setOnClickListener(v -> finish());
        }
    }
}