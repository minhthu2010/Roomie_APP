package com.example.roomie;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.common.BottomNavHelper;

public class BoLocActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bo_loc);
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

        // Reset button
        View btnReset = findViewById(R.id.btn_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> finish());
        }

        // Action buttons
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
