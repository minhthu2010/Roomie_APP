package com.example.roomie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.common.BottomNavHelper;

public class PhongDaLuuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phong_da_luu);
        BottomNavHelper.bind(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View.OnClickListener openRoom = view -> startActivity(new Intent(this, ChiTietPhongActivity.class));
        findViewById(R.id.saved_room1).setOnClickListener(openRoom);
        findViewById(R.id.saved_room2).setOnClickListener(openRoom);
        findViewById(R.id.saved_room3).setOnClickListener(openRoom);

        bindSavedRoom(R.id.saved_room1, R.drawable.img_room1, R.string.match_rate_94,
                R.string.room_title, R.string.saved_room1_address, R.string.room_price);
        bindSavedRoom(R.id.saved_room2, R.drawable.img_room2, R.string.match_rate_89,
                R.string.room2_title, R.string.saved_room2_address, R.string.room2_price);
        bindSavedRoom(R.id.saved_room3, R.drawable.img_room3, R.string.saved_match_84,
                R.string.saved_room3_title, R.string.saved_room3_address, R.string.saved_room3_price);
    }

    private void bindSavedRoom(int cardId, int imageRes, int matchRes, int titleRes, int addressRes, int priceRes) {
        View card = findViewById(cardId);
        ((ShapeableImageView) card.findViewById(R.id.img_saved_room)).setImageResource(imageRes);
        ((TextView) card.findViewById(R.id.tv_saved_match)).setText(matchRes);
        ((TextView) card.findViewById(R.id.tv_saved_title)).setText(titleRes);
        ((TextView) card.findViewById(R.id.tv_saved_address)).setText(addressRes);
        ((TextView) card.findViewById(R.id.tv_saved_price)).setText(priceRes);
    }
}
