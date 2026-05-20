package com.example.kin.ui;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.kin.R;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URLS = "extra_image_urls";
    public static final String EXTRA_IMAGE_INDEX = "extra_image_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> urls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        int index = getIntent().getIntExtra(EXTRA_IMAGE_INDEX, 0);
        String url = urls == null || urls.isEmpty() ? "" : urls.get(Math.max(0, Math.min(index, urls.size() - 1)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(android.R.color.black));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("图片预览");
        toolbar.setTitleTextColor(getColor(android.R.color.white));
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout stage = new FrameLayout(this);
        ZoomImageView imageView = new ZoomImageView(this);
        imageView.setBackgroundColor(getColor(android.R.color.black));
        stage.addView(imageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.addView(stage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        setContentView(root);

        new RemoteImageLoader().load(imageView, url);
    }

    private static final class ZoomImageView extends AppCompatImageView {
        private final Matrix imageMatrixState = new Matrix();
        private final ScaleGestureDetector scaleDetector;
        private float currentScale = 1f;
        private float minScale = 1f;
        private float maxScale = 5f;
        private float lastX;
        private float lastY;
        private boolean dragging;

        ZoomImageView(android.content.Context context) {
            super(context);
            setScaleType(ScaleType.MATRIX);
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float factor = detector.getScaleFactor();
                    float targetScale = currentScale * factor;
                    if (targetScale < minScale) {
                        factor = minScale / currentScale;
                        targetScale = minScale;
                    } else if (targetScale > maxScale) {
                        factor = maxScale / currentScale;
                        targetScale = maxScale;
                    }
                    imageMatrixState.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                    currentScale = targetScale;
                    setImageMatrix(imageMatrixState);
                    return true;
                }
            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = true;
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragging && !scaleDetector.isInProgress()) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        imageMatrixState.postTranslate(dx, dy);
                        setImageMatrix(imageMatrixState);
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerCount() > 1) {
                        int nextIndex = event.getActionIndex() == 0 ? 1 : 0;
                        lastX = event.getX(nextIndex);
                        lastY = event.getY(nextIndex);
                    }
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public void setImageBitmap(Bitmap bitmap) {
            super.setImageBitmap(bitmap);
            post(this::resetImageMatrix);
        }

        @Override
        public void setImageDrawable(@Nullable Drawable drawable) {
            super.setImageDrawable(drawable);
            post(this::resetImageMatrix);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            resetImageMatrix();
        }

        private void resetImageMatrix() {
            Drawable drawable = getDrawable();
            if (drawable == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }
            int imageWidth = drawable.getIntrinsicWidth();
            int imageHeight = drawable.getIntrinsicHeight();
            if (imageWidth <= 0 || imageHeight <= 0) {
                return;
            }
            float scale = Math.min((float) getWidth() / imageWidth, (float) getHeight() / imageHeight);
            float dx = (getWidth() - imageWidth * scale) / 2f;
            float dy = (getHeight() - imageHeight * scale) / 2f;
            imageMatrixState.reset();
            imageMatrixState.postScale(scale, scale);
            imageMatrixState.postTranslate(dx, dy);
            minScale = scale;
            maxScale = scale * 5f;
            currentScale = scale;
            setImageMatrix(imageMatrixState);
        }
    }
}
