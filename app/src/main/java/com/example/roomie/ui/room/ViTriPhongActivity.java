package com.example.roomie.ui.room;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.BottomNavHelper;

public class ViTriPhongActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vi_tri_phong);

        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button returns to ChiTietPhongActivity
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Contact Zalo & Call buttons
        View btnZalo = findViewById(R.id.btn_contact_zalo);
        if (btnZalo != null) {
            btnZalo.setOnClickListener(v ->
                Toast.makeText(ViTriPhongActivity.this, "Mở liên hệ Zalo...", Toast.LENGTH_SHORT).show()
            );
        }

        View btnCall = findViewById(R.id.btn_call_phone);
        if (btnCall != null) {
            btnCall.setOnClickListener(v ->
                Toast.makeText(ViTriPhongActivity.this, "Đang kết nối cuộc gọi...", Toast.LENGTH_SHORT).show()
            );
        }
    }
}