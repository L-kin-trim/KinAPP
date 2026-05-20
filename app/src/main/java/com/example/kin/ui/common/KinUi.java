package com.example.kin.ui.common;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.example.kin.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public final class KinUi {
    private KinUi() {
    }

    public static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    @ColorInt
    public static int color(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static MaterialCardView card(Context context) {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dp(context, 22));
        card.setCardElevation(0f);
        card.setUseCompatPadding(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 14);
        card.setLayoutParams(params);
        return card;
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static TextView text(Context context, String value, float sizeSp, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        textView.setTextColor(color(context, com.google.android.material.R.attr.colorOnSurface));
        textView.setTypeface(Typeface.create("sans-serif-medium", bold ? Typeface.BOLD : Typeface.NORMAL));
        return textView;
    }

    public static TextView muted(Context context, String value, float sizeSp) {
        TextView textView = text(context, value, sizeSp, false);
        textView.setTextColor(context.getColor(R.color.kin_text_muted));
        return textView;
    }

    public static Chip chip(Context context, String label) {
        Chip chip = new Chip(context);
        chip.setText(label);
        chip.setClickable(true);
        chip.setCheckable(false);
        return chip;
    }

    public static MaterialButton filledButton(Context context, String label) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setCornerRadius(dp(context, 18));
        return button;
    }

    public static MaterialButton outlinedButton(Context context, String label) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(label);
        button.setCornerRadius(dp(context, 18));
        return button;
    }

    public static TextInputLayout inputLayout(Context context, String hint, boolean multiLine) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setHint(hint);
        TextInputEditText editText = new TextInputEditText(context);
        editText.setTextColor(color(context, com.google.android.material.R.attr.colorOnSurface));
        editText.setHintTextColor(context.getColor(R.color.kin_text_muted));
        if (multiLine) {
            editText.setMinLines(4);
            editText.setGravity(Gravity.TOP | Gravity.START);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        layout.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return layout;
    }

    public static TextInputEditText edit(TextInputLayout layout) {
        return (TextInputEditText) layout.getEditText();
    }

    public static void pad(View view, Context context, int horizontal, int vertical) {
        view.setPadding(dp(context, horizontal), dp(context, vertical), dp(context, horizontal), dp(context, vertical));
    }

    public static void margins(View view, Context context, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        ViewGroup.MarginLayoutParams marginLayoutParams = params instanceof ViewGroup.MarginLayoutParams
                ? (ViewGroup.MarginLayoutParams) params
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        marginLayoutParams.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        view.setLayoutParams(marginLayoutParams);
    }

    public static LinearLayout buttonRow(Context context, MaterialButton... buttons) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < buttons.length; i++) {
            MaterialButton button = buttons[i];
            stabilizeButton(context, button);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                params.leftMargin = dp(context, 8);
            }
            row.addView(button, params);
        }
        return row;
    }

    public static LinearLayout buttonGrid(Context context, int columns, MaterialButton... buttons) {
        LinearLayout grid = vertical(context);
        int safeColumns = Math.max(1, columns);
        LinearLayout row = null;
        for (int i = 0; i < buttons.length; i++) {
            int column = i % safeColumns;
            if (column == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                if (i > 0) {
                    rowParams.topMargin = dp(context, 8);
                }
                grid.addView(row, rowParams);
            }
            MaterialButton button = buttons[i];
            stabilizeButton(context, button);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (column > 0) {
                params.leftMargin = dp(context, 8);
            }
            row.addView(button, params);
        }
        return grid;
    }

    public static void weightedButton(View view, Context context, int leftMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(context, leftMarginDp);
        view.setLayoutParams(params);
        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            stabilizeButton(context, button);
        }
    }

    private static void stabilizeButton(Context context, MaterialButton button) {
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setMinWidth(0);
        button.setMinHeight(dp(context, 44));
        button.setInsetTop(dp(context, 6));
        button.setInsetBottom(dp(context, 6));
    }

    public static LinearLayout sectionContainer(Context context, int padding) {
        LinearLayout layout = vertical(context);
        pad(layout, context, padding, padding);
        return layout;
    }

    public static View divider(Context context) {
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)));
        divider.setBackgroundColor(isNight(context) ? context.getColor(R.color.kin_stroke_dark) : context.getColor(R.color.kin_stroke));
        return divider;
    }

    public static View imageStrip(Context context, List<String> urls, RemoteImageLoader loader) {
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(context, 8), 0, dp(context, 8));
        scrollView.addView(row);
        for (String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 160), dp(context, 112));
            params.rightMargin = dp(context, 10);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(context, 18));
            background.setColor(isNight(context) ? context.getColor(R.color.kin_dark_panel_alt) : context.getColor(R.color.kin_light_panel_alt));
            imageView.setBackground(background);
            imageView.setClipToOutline(true);
            loader.load(imageView, url);
            row.addView(imageView);
        }
        return scrollView;
    }
}
