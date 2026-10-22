package com.example.roomie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.common.BottomNavHelper;
import com.example.roomie.ui.post.PostFlowActivity;

public class BaiDangCuaToiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bai_dang_cua_toi);
        BottomNavHelper.bind(this);

        View createPostButton = findViewById(R.id.btn_create_post);
        if (createPostButton != null) {
            createPostButton.setOnClickListener(view ->
                    startActivity(new Intent(this, PostFlowActivity.class)));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindPost(R.id.my_post1, R.drawable.img_room1, R.string.my_post_status_searching,
                R.string.my_post1_title, R.string.my_post1_price, R.string.my_post1_address,
                R.string.my_post1_interest);
        bindPost(R.id.my_post2, R.drawable.img_room2, R.string.my_post_status_found,
                R.string.my_post2_title, R.string.my_post2_price, R.string.my_post2_address,
                R.string.my_post2_date);
        bindPost(R.id.my_post3, R.drawable.img_room3, R.string.my_post_status_expired,
                R.string.my_post3_title, R.string.my_post3_price, R.string.my_post3_address,
                R.string.my_post3_date);

        View.OnClickListener openRoom = view -> startActivity(new Intent(this, ChiTietPhongActivity.class));
        findViewById(R.id.my_post1).findViewById(R.id.btn_my_post_view).setOnClickListener(openRoom);
        findViewById(R.id.my_post2).findViewById(R.id.btn_my_post_view).setOnClickListener(openRoom);
        findViewById(R.id.my_post3).findViewById(R.id.btn_my_post_view).setOnClickListener(openRoom);
    }

    private void bindPost(int cardId, int imageRes, int statusRes, int titleRes, int priceRes,
                          int addressRes, int dateRes) {
        View card = findViewById(cardId);
        ((ImageView) card.findViewById(R.id.img_my_post)).setImageResource(imageRes);
        ((TextView) card.findViewById(R.id.tv_my_post_status)).setText(statusRes);
        ((TextView) card.findViewById(R.id.tv_my_post_title)).setText(titleRes);
        ((TextView) card.findViewById(R.id.tv_my_post_price)).setText(priceRes);
        ((TextView) card.findViewById(R.id.tv_my_post_address)).setText(addressRes);
        ((TextView) card.findViewById(R.id.tv_my_post_date)).setText(dateRes);
    }
}
