package com.example.roomie.ui.matching;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.roomie.R;
import com.example.roomie.data.discovery.MatchingModels;

import java.util.List;
import java.util.Locale;

/** Hiển thị điểm tương thích dùng chung cho các màn matching. */
final class MatchUi {

    private MatchUi() {}

    static String percentText(Integer percent) {
        return percent == null ? "--%" : String.format(Locale.getDefault(), "%d%%", percent);
    }

    /** Vòng điểm: "74%" + "Khá phù hợp" */
    static void bindScore(TextView tvPercent, TextView tvLevel, MatchingModels.Comparison c) {
        tvPercent.setText(percentText(c.overall));
        tvLevel.setText(c.level != null ? c.level : tvLevel.getContext().getString(R.string.match_no_data));
    }

    /** Danh sách nhóm: tên, %, thanh độ dài theo % */
    static void fillGroups(LayoutInflater inflater, LinearLayout container, List<MatchingModels.Group> groups) {
        container.removeAllViews();
        for (MatchingModels.Group g : groups) {
            View row = inflater.inflate(R.layout.item_match_group_row, container, false);
            ((TextView) row.findViewById(R.id.tv_group_name)).setText(g.name);
            ((TextView) row.findViewById(R.id.tv_group_percent)).setText(percentText(g.percent));
            int p = g.percent == null ? 0 : Math.max(0, Math.min(100, g.percent));
            setWeight(row.findViewById(R.id.view_bar_fill), p);
            setWeight(row.findViewById(R.id.view_bar_rest), 100 - p);
            container.addView(row);
        }
    }

    private static void setWeight(View view, int weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.weight = weight;
        view.setLayoutParams(lp);
    }
}
