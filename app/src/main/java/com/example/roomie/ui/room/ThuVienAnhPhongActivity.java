package com.example.roomie.ui.room;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roomie.R;
import com.example.roomie.common.AppNotificationHelper;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.DiscoveryApi;
import com.example.roomie.data.discovery.RoomModels;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;

import java.util.Collections;
import java.util.List;

/** Thư viện ảnh phòng. Extra: EXTRA_POST_ID (bắt buộc), EXTRA_START_INDEX. */
public class ThuVienAnhPhongActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";
    public static final String EXTRA_START_INDEX = "START_INDEX";

    public static Intent intent(Context context, String postId, int startIndex) {
        return new Intent(context, ThuVienAnhPhongActivity.class)
                .putExtra(EXTRA_POST_ID, postId)
                .putExtra(EXTRA_START_INDEX, startIndex);
    }

    private List<RoomModels.Image> images = Collections.emptyList();
    private int current = 0;

    private ImageView mainPhoto;
    private TextView pageIndicator;
    private TextView photoCaption;
    private LinearLayout layoutThumbs;

    @SuppressLint("ClickableViewAccessibility")
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
        layoutThumbs = findViewById(R.id.layout_thumbs);

        findViewById(R.id.btn_prev_photo).setOnClickListener(view -> showPhoto(current - 1));
        findViewById(R.id.btn_next_photo).setOnClickListener(view -> showPhoto(current + 1));

        // Vuốt ngang trên ảnh lớn để chuyển ảnh
        GestureDetector swipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || Math.abs(velocityX) < Math.abs(velocityY)) return false;
                showPhoto(e2.getX() < e1.getX() ? current + 1 : current - 1);
                return true;
            }
        });
        mainPhoto.setOnTouchListener((v, event) -> swipe.onTouchEvent(event));

        String postId = getIntent().getStringExtra(EXTRA_POST_ID);
        current = getIntent().getIntExtra(EXTRA_START_INDEX, 0);
        if (postId == null) {
            finish();
            return;
        }
        load(postId);
    }

    private void load(String postId) {
        ApiClient.create(this, DiscoveryApi.class).getRoomImages(postId)
                .enqueue(new ApiCallback<RoomModels.RoomImages>(this) {
                    @Override
                    public void onSuccess(RoomModels.RoomImages data) {
                        if (data == null) return;
                        ((TextView) findViewById(R.id.tv_gallery_title)).setText(data.title);
                        ((TextView) findViewById(R.id.tv_gallery_subtitle)).setText(
                                getString(R.string.gallery_subtitle, data.addressText, data.ownerName));
                        images = data.images;
                        buildThumbs();
                        showPhoto(current);
                    }

                    @Override
                    public void onError(String message) {
                        AppNotificationHelper.showError(ThuVienAnhPhongActivity.this, message);
                    }
                });
    }

    private void buildThumbs() {
        layoutThumbs.removeAllViews();
        for (int i = 0; i < images.size(); i++) {
            View thumb = LayoutInflater.from(this).inflate(R.layout.item_gallery_thumb, layoutThumbs, false);
            ImageLoader.load(thumb.findViewById(R.id.img_thumb), images.get(i).url, R.drawable.img_room_placeholder);
            final int index = i;
            thumb.setOnClickListener(v -> showPhoto(index));
            layoutThumbs.addView(thumb);
        }
    }

    private void showPhoto(int index) {
        int total = images.size();
        if (total == 0) {
            pageIndicator.setText("");
            photoCaption.setText(R.string.gallery_empty);
            mainPhoto.setImageResource(R.drawable.img_detail_room);
            return;
        }
        // Đi vòng: qua ảnh cuối thì về ảnh đầu
        current = ((index % total) + total) % total;
        RoomModels.Image image = images.get(current);
        ImageLoader.load(mainPhoto, image.url, R.drawable.img_detail_room);
        pageIndicator.setText(getString(R.string.gallery_page, current + 1, total));
        String caption = image.caption != null ? image.caption : getString(R.string.gallery_caption_default);
        photoCaption.setText(getString(R.string.gallery_caption, caption, current + 1, total));
        for (int i = 0; i < layoutThumbs.getChildCount(); i++) {
            View thumb = layoutThumbs.getChildAt(i);
            if (i == current) thumb.setBackgroundResource(R.drawable.bg_card_outline);
            else thumb.setBackground(null);
            thumb.setAlpha(i == current ? 1f : 0.6f);
        }
    }
}
