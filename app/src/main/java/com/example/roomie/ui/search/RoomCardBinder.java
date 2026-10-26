package com.example.roomie.ui.search;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.roomie.R;
import com.example.roomie.common.ImageLoader;
import com.example.roomie.data.discovery.SearchModels;
import com.example.roomie.ui.room.ChiTietPhongActivity;
import com.example.roomie.ui.saved.SaveToggle;

/** Tạo thẻ phòng (item_feed_room_card) từ RoomCardDto; dùng ở Home, Hồ sơ người đăng. */
public final class RoomCardBinder {

    // Phòng chưa có ảnh thì dùng ảnh minh họa xoay vòng
    private static final int[] PLACEHOLDERS = {R.drawable.img_room1, R.drawable.img_room2, R.drawable.img_room3};

    private RoomCardBinder() {}

    public static View create(Activity activity, ViewGroup parent, SearchModels.RoomCard room, int position) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_feed_room_card, parent, false);
        ImageLoader.load(card.findViewById(R.id.img_room), room.coverImageUrl,
                PLACEHOLDERS[position % PLACEHOLDERS.length]);

        TextView badge = card.findViewById(R.id.tv_match_badge);
        if (room.matchPercent != null) {
            badge.setText(activity.getString(R.string.feed_match_badge, room.matchPercent));
        } else {
            badge.setVisibility(View.GONE);
        }

        ImageView heart = card.findViewById(R.id.img_saved);
        SaveToggle.bindIcon(heart, room.isSaved);
        heart.setOnClickListener(v -> SaveToggle.toggle(activity, room.id, room.isSaved, heart,
                saved -> room.isSaved = saved));

        ((TextView) card.findViewById(R.id.tv_room_title)).setText(room.title);
        String address = room.addressText != null ? room.addressText : "";
        if (room.distanceKm != null) {
            address = activity.getString(R.string.feed_distance, address, RoomFormat.km(room.distanceKm));
        }
        ((TextView) card.findViewById(R.id.tv_room_address)).setText(address);
        ((TextView) card.findViewById(R.id.tv_room_price))
                .setText(activity.getString(R.string.feed_price, RoomFormat.money(room.monthlyRent)));

        card.setOnClickListener(v -> activity.startActivity(ChiTietPhongActivity.intent(activity, room.id)));
        return card;
    }
}
