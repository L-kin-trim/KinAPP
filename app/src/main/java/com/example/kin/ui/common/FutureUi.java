package com.example.kin.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.R;
import com.example.kin.model.FutureFeatureDefinition;
import com.example.kin.model.FutureFeatureRecord;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

public final class FutureUi {
    private FutureUi() {
    }

    public static MaterialCardView featureCard(Context context,
                                               String title,
                                               String subtitle,
                                               String badge,
                                               View.OnClickListener listener) {
        MaterialCardView card = KinUi.card(context);
        LinearLayout body = KinUi.sectionContainer(context, 16);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView titleView = KinUi.text(context, title, 17, true);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!TextUtils.isEmpty(badge)) {
            Chip chip = KinUi.chip(context, badge);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(chip);
        }
        body.addView(row);
        TextView summary = KinUi.muted(context, subtitle, 13);
        KinUi.margins(summary, context, 0, 8, 0, 0);
        body.addView(summary);
        card.addView(body);
        card.setOnClickListener(listener);
        return card;
    }

    public static MaterialCardView recordCard(Context context,
                                              FutureFeatureDefinition feature,
                                              FutureFeatureRecord record,
                                              View.OnClickListener open,
                                              View.OnClickListener status,
                                              View.OnClickListener delete) {
        MaterialCardView card = KinUi.card(context);
        LinearLayout body = KinUi.sectionContainer(context, 16);
        body.addView(KinUi.text(context, safe(record.title, feature.title), 17, true));
        TextView meta = KinUi.muted(context,
                feature.section + " · " + safe(record.status, "DRAFT") + " · " + safe(record.ownerUsername, "当前用户"),
                12);
        KinUi.margins(meta, context, 0, 8, 0, 0);
        body.addView(meta);
        TextView summary = KinUi.muted(context, safe(record.summary, feature.summary), 14);
        KinUi.margins(summary, context, 0, 10, 0, 0);
        body.addView(summary);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setWeightSum(3f);
        MaterialButton openButton = KinUi.filledButton(context, "查看/编辑");
        MaterialButton statusButton = KinUi.outlinedButton(context, "流转状态");
        MaterialButton deleteButton = KinUi.outlinedButton(context, "删除");
        openButton.setOnClickListener(open);
        statusButton.setOnClickListener(status);
        deleteButton.setOnClickListener(delete);
        equalButton(context, actions, openButton, 0);
        equalButton(context, actions, statusButton, 8);
        equalButton(context, actions, deleteButton, 8);
        KinUi.margins(actions, context, 0, 14, 0, 0);
        body.addView(actions);
        card.addView(body);
        return card;
    }

    public static MaterialCardView skeletonCard(Context context, String label) {
        MaterialCardView card = KinUi.card(context);
        LinearLayout body = KinUi.sectionContainer(context, 16);
        body.addView(KinUi.text(context, label, 16, true));
        TextView line1 = bar(context, 80);
        TextView line2 = bar(context, 55);
        body.addView(line1);
        body.addView(line2);
        card.addView(body);
        return card;
    }

    public static MaterialCardView statusCard(Context context, String title, String message, View.OnClickListener retry) {
        MaterialCardView card = KinUi.card(context);
        LinearLayout body = KinUi.sectionContainer(context, 16);
        body.addView(KinUi.text(context, title, 17, true));
        TextView text = KinUi.muted(context, message, 14);
        KinUi.margins(text, context, 0, 8, 0, 0);
        body.addView(text);
        if (retry != null) {
            MaterialButton button = KinUi.outlinedButton(context, "重试");
            button.setOnClickListener(retry);
            KinUi.margins(button, context, 0, 12, 0, 0);
            body.addView(button);
        }
        card.addView(body);
        return card;
    }

    private static TextView bar(Context context, int percent) {
        TextView view = KinUi.muted(context, " ", 12);
        view.setBackgroundColor(context.getColor(KinUi.isNight(context) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                Math.max(KinUi.dp(context, 120), context.getResources().getDisplayMetrics().widthPixels * percent / 100),
                KinUi.dp(context, 12)
        );
        params.setMargins(0, KinUi.dp(context, 10), 0, 0);
        view.setLayoutParams(params);
        return view;
    }

    private static void equalButton(Context context, LinearLayout actions, MaterialButton button, int left) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = KinUi.dp(context, left);
        button.setLayoutParams(params);
        button.setMaxLines(1);
        actions.addView(button);
    }

    private static String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
