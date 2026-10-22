package com.example.roomie.common;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.example.roomie.R;

public class AppNotificationHelper {

    private AppNotificationHelper() {}

    public static void showSuccess(Activity activity, String message) {
        showNotification(activity, message, R.drawable.bg_button_primary, Color.WHITE);
    }

    public static void showError(Activity activity, String message) {
        showNotification(activity, message, R.drawable.bg_button_primary, Color.WHITE);
    }

    public static void showInfo(Activity activity, String message) {
        showNotification(activity, message, R.drawable.bg_button_primary, Color.WHITE);
    }

    private static void showNotification(Activity activity, String message, int bgDrawableRes, int textColor) {
        if (activity == null || message == null || message.isEmpty()) return;

        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(bgDrawableRes);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(textColor);
            textView.setTextSize(14f);
            textView.setMaxLines(3);
        }

        snackbar.show();
    }
}