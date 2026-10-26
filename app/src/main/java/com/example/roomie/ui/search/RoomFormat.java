package com.example.roomie.ui.search;

import android.content.Context;

import com.example.roomie.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Định dạng tiền, khoảng cách, tên loại phòng... theo kiểu Việt Nam. */
public final class RoomFormat {

    private RoomFormat() {}

    private static final Locale VI = new Locale("vi", "VN");

    /** 3200000 -> "3.200.000" */
    public static String money(long vnd) {
        DecimalFormat f = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(VI));
        return f.format(vnd);
    }

    /** 2500000 -> "2,5"; 3000000 -> "3" (đơn vị triệu) */
    public static String millions(long vnd) {
        DecimalFormat f = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(VI));
        return f.format(vnd / 1_000_000.0);
    }

    /** 0.75 -> "0,8" */
    public static String km(double km) {
        DecimalFormat f = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(VI));
        return f.format(km);
    }

    public static String budgetLabel(Context ctx, Long min, Long max) {
        if (min == null && max == null) return ctx.getString(R.string.filter_budget_any);
        if (min == null) return ctx.getString(R.string.chip_budget_max, millions(max));
        if (max == null) return ctx.getString(R.string.chip_budget_min, millions(min));
        return ctx.getString(R.string.chip_budget, millions(min), millions(max));
    }

    public static int roomTypeLabel(String type) {
        switch (type) {
            case "shared_room":
                return R.string.filter_room_shared;
            case "private_room":
                return R.string.filter_room_private;
            case "apartment":
                return R.string.filter_room_apartment;
            case "whole_house":
                return R.string.filter_room_house;
            default:
                return R.string.filter_room_studio;
        }
    }

    public static int sortLabel(String sort) {
        switch (sort) {
            case SearchCriteria.SORT_NEWEST:
                return R.string.sort_newest;
            case SearchCriteria.SORT_PRICE_ASC:
                return R.string.sort_price_asc;
            case SearchCriteria.SORT_PRICE_DESC:
                return R.string.sort_price_desc;
            case SearchCriteria.SORT_DISTANCE:
                return R.string.sort_distance;
            default:
                return R.string.sort_match;
        }
    }
}
