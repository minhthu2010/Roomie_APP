package com.example.roomie.ui.quiz;

import com.example.roomie.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Hàm hiển thị dùng chung cho các màn bài test thói quen. */
public final class QuizUi {

    private QuizUi() {}

    public static int iconFor(String questionKey) {
        if (questionKey == null) return R.drawable.ic_sparkles;
        switch (questionKey) {
            case "sleep_time":
                return R.drawable.ic_moon;
            case "wake_time":
                return R.drawable.ic_clock;
            case "chores":
                return R.drawable.ic_checklist;
            case "smoking":
                return R.drawable.ic_no_smoking;
            case "cooking":
                return R.drawable.ic_kitchen;
            case "pets":
                return R.drawable.ic_pet;
            case "bills_split":
                return R.drawable.ic_wallet;
            case "air_conditioning":
                return R.drawable.ic_ac;
            case "guests":
                return R.drawable.ic_people;
            case "overnight_guests":
                return R.drawable.ic_home;
            case "communication":
                return R.drawable.ic_chat;
            default:
                return R.drawable.ic_sparkles;
        }
    }

    /** "2026-10-26T02:12:51.674Z" (UTC từ backend) -> "26/10/2026" theo giờ máy. */
    public static String formatDate(String isoUtc) {
        if (isoUtc == null) return null;
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        in.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = in.parse(isoUtc);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
