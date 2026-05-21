package com.example.kin.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class LevelVisuals {
    private static final String[] COLORS = {
            "#6B7280", "#2563EB", "#0F766E", "#16A34A", "#65A30D",
            "#CA8A04", "#EA580C", "#DC2626", "#DB2777", "#9333EA",
            "#7C3AED", "#4F46E5", "#0369A1", "#0E7490", "#047857",
            "#92400E", "#9A3412", "#991B1B", "#701A75", "#111827"
    };

    private LevelVisuals() {
    }

    public static int normalize(int level) {
        return Math.max(1, Math.min(20, level));
    }

    public static int color(int level) {
        return Color.parseColor(COLORS[normalize(level) - 1]);
    }

    public static GradientDrawable badgeBackground(Context context, int level) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(level));
        background.setCornerRadius(KinUi.dp(context, 5));
        return background;
    }

    public static GradientDrawable avatarBackground(int level) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        int safeLevel = normalize(level);
        if (safeLevel >= 15) {
            background.setOrientation(GradientDrawable.Orientation.TL_BR);
            background.setColors(new int[]{color(safeLevel), Color.parseColor("#F6D365")});
        } else {
            background.setColor(color(safeLevel));
        }
        return background;
    }
}
