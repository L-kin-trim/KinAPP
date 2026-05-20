package com.example.kin.ui.future;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import com.example.kin.R;
import com.example.kin.ui.common.KinUi;

import org.json.JSONArray;
import org.json.JSONObject;

public class FutureTacticBoardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final JSONArray points = new JSONArray();

    public FutureTacticBoardView(Context context) {
        super(context);
        setMinimumHeight(KinUi.dp(context, 240));
        setBackgroundColor(context.getColor(KinUi.isNight(context) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(KinUi.dp(getContext(), 2));
        paint.setColor(getContext().getColor(R.color.kin_stroke));
        for (int i = 1; i < 4; i++) {
            float x = width * i / 4f;
            canvas.drawLine(x, 0, x, height, paint);
            float y = height * i / 4f;
            canvas.drawLine(0, y, width, y, paint);
        }

        paint.setColor(Color.argb(180, 17, 24, 39));
        paint.setStrokeWidth(KinUi.dp(getContext(), 4));
        Path path = new Path();
        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.optJSONObject(i);
            if (point == null) {
                continue;
            }
            float x = (float) point.optDouble("x") * width;
            float y = (float) point.optDouble("y") * height;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.optJSONObject(i);
            if (point == null) {
                continue;
            }
            float x = (float) point.optDouble("x") * width;
            float y = (float) point.optDouble("y") * height;
            paint.setColor(i % 2 == 0 ? getContext().getColor(R.color.kin_success) : getContext().getColor(R.color.kin_warning));
            canvas.drawCircle(x, y, KinUi.dp(getContext(), 10), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || getWidth() <= 0 || getHeight() <= 0) {
            return true;
        }
        try {
            JSONObject point = new JSONObject();
            point.put("x", event.getX() / getWidth());
            point.put("y", event.getY() / getHeight());
            point.put("type", points.length() % 2 == 0 ? "player" : "utility");
            points.put(point);
        } catch (Exception ignored) {
        }
        invalidate();
        return true;
    }

    public JSONArray exportPoints() {
        return points;
    }
}
