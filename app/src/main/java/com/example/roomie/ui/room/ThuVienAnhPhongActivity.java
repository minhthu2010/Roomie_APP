package com.example.roomie.ui.room;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;

public class ThuVienAnhPhongActivity extends AppCompatActivity {

    private int currentPhotoIndex = 1;
    private final int[] photoResources = {
        R.drawable.img_detail_room,
        R.drawable.img_room1,
        R.drawable.img_room2,
        R.drawable.img_room3,
        R.drawable.img_detail_room,
        R.drawable.img_room1,
        R.drawable.img_room2,
        R.drawable.img_room3
    };
    private ImageView mainPhoto;
    private TextView pageIndicator;
    private TextView photoCaption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_thu_vien_anh_phong);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_back).setOnClickListener(view -> finish());

        mainPhoto = findViewById(R.id.img_main_photo);
        pageIndicator = findViewById(R.id.tv_page_indicator);
        photoCaption = findViewById(R.id.tv_photo_caption);

        findViewById(R.id.btn_prev_photo).setOnClickListener(view -> showPhoto(currentPhotoIndex - 1));
        findViewById(R.id.btn_next_photo).setOnClickListener(view -> showPhoto(currentPhotoIndex + 1));
        findViewById(R.id.thumb1).setOnClickListener(view -> showPhoto(1));
        findViewById(R.id.thumb2).setOnClickListener(view -> showPhoto(2));
        findViewById(R.id.thumb3).setOnClickListener(view -> showPhoto(3));
        findViewById(R.id.thumb4).setOnClickListener(view -> showPhoto(4));
        showPhoto(currentPhotoIndex);
    }

    private void showPhoto(int index) {
        if (index < 1 || index > photoResources.length) {
            return;
        }
        currentPhotoIndex = index;
        mainPhoto.setImageResource(photoResources[index - 1]);
        pageIndicator.setText(index + " / " + photoResources.length);
        photoCaption.setText("Không gian phòng · Ảnh " + index + " trong " + photoResources.length);
    }
}
