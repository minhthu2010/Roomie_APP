package com.example.roomie.ui.auth;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.roomie.R;

/** Biến "Điều khoản sử dụng" và "Chính sách quyền riêng tư" trong dòng chữ thành link mở popup nội dung. */
final class PolicyLinks {

    private PolicyLinks() {}

    static void bind(TextView textView) {
        Context context = textView.getContext();
        SpannableString text = new SpannableString(textView.getText());
        addLink(context, text, R.string.policy_terms_link, R.string.policy_terms_body);
        addLink(context, text, R.string.policy_privacy_link, R.string.policy_privacy_body);
        textView.setText(text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);
    }

    private static void addLink(Context context, SpannableString text, int linkRes, int bodyRes) {
        String link = context.getString(linkRes);
        int start = text.toString().indexOf(link);
        if (start < 0) return;
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                new AlertDialog.Builder(widget.getContext())
                        .setTitle(link)
                        .setMessage(bodyRes)
                        .setPositiveButton(R.string.policy_close, null)
                        .show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint paint) {
                paint.setColor(ContextCompat.getColor(context, R.color.primary_maroon));
                paint.setUnderlineText(true);
            }
        }, start, start + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
