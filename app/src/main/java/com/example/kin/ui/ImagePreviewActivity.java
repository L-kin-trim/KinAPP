package com.example.kin.ui;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.kin.R;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URLS = "extra_image_urls";
    public static final String EXTRA_IMAGE_INDEX = "extra_image_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> urls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        if (urls == null) {
            urls = new ArrayList<>();
        }
        int index = getIntent().getIntExtra(EXTRA_IMAGE_INDEX, 0);
        index = Math.max(0, Math.min(index, Math.max(0, urls.size() - 1)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(android.R.color.black));

        FrameLayout topBar = new FrameLayout(this);
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("图片预览");
        toolbar.setTitleTextColor(getColor(android.R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_nav_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        topBar.addView(toolbar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView indicator = new TextView(this);
        indicator.setTextColor(getColor(android.R.color.white));
        indicator.setTextSize(16f);
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        int marginEnd = Math.round(getResources().getDisplayMetrics().density * 16f);
        indicatorParams.rightMargin = marginEnd;
        topBar.addView(indicator, indicatorParams);

        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ViewPager2 pager = new ViewPager2(this);
        pager.setAdapter(new ImageAdapter(urls, new RemoteImageLoader()));
        root.addView(pager, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        setContentView(root);

        final int total = urls.size();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                indicator.setText((position + 1) + " / " + total);
            }
        });
        indicator.setText(total == 0 ? "" : (index + 1) + " / " + total);
        pager.setCurrentItem(index, false);
    }

    private static final class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.Holder> {
        private final List<String> urls;
        private final RemoteImageLoader loader;

        ImageAdapter(List<String> urls, RemoteImageLoader loader) {
            this.urls = urls;
            this.loader = loader;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomImageView imageView = new ZoomImageView(parent.getContext());
            imageView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setBackgroundColor(parent.getContext().getColor(android.R.color.black));
            return new Holder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.imageView.setImageDrawable(null);
            loader.load(holder.imageView, urls.get(position));
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ZoomImageView imageView;

            Holder(ZoomImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    /**
     * Fit-width image view with bounded pan/zoom. While zoomed it consumes horizontal
     * drags itself, but releases the gesture back to the parent ViewPager2 once the
     * image reaches a horizontal edge so the pager can switch to the next image.
     */
    private static final class ZoomImageView extends AppCompatImageView {
        private final Matrix matrix = new Matrix();
        private final float[] values = new float[9];
        private final ScaleGestureDetector scaleDetector;
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
                    matrix.getValues(values);
                    float current = values[Matrix.MSCALE_X];
                    float target = current * factor;
                    if (target < minScale) {
                        factor = minScale / current;
                    } else if (target > maxScale) {
                        factor = maxScale / current;
                    }
                    matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                    fixTranslation();
                    setImageMatrix(matrix);
                    return true;
                }
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = true;
                    lastX = event.getX();
                    lastY = event.getY();
                    requestDisallowIntercept(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (scaleDetector.isInProgress()) {
                        requestDisallowIntercept(true);
                        lastX = event.getX();
                        lastY = event.getY();
                        return true;
                    }
                    if (dragging) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        requestDisallowIntercept(canPanHorizontally(dx));
                        matrix.postTranslate(dx, dy);
                        fixTranslation();
                        setImageMatrix(matrix);
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

        private void requestDisallowIntercept(boolean disallow) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(disallow);
            }
        }

        private boolean canPanHorizontally(float dx) {
            Drawable drawable = getDrawable();
            if (drawable == null || dx == 0f) {
                return false;
            }
            matrix.getValues(values);
            float scaledWidth = drawable.getIntrinsicWidth() * values[Matrix.MSCALE_X];
            if (scaledWidth <= getWidth() + 1f) {
                return false;
            }
            float transX = values[Matrix.MTRANS_X];
            float minTransX = getWidth() - scaledWidth;
            if (dx > 0) {
                return transX < -1f;
            }
            return transX > minTransX + 1f;
        }

        @Override
        public void setImageDrawable(@Nullable Drawable drawable) {
            super.setImageDrawable(drawable);
            post(this::resetMatrix);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            resetMatrix();
        }

        private void fixTranslation() {
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            matrix.getValues(values);
            float scale = values[Matrix.MSCALE_X];
            float scaledWidth = drawable.getIntrinsicWidth() * scale;
            float scaledHeight = drawable.getIntrinsicHeight() * scale;
            float fixX = fixTrans(values[Matrix.MTRANS_X], getWidth(), scaledWidth);
            float fixY = fixTrans(values[Matrix.MTRANS_Y], getHeight(), scaledHeight);
            matrix.postTranslate(fixX, fixY);
        }

        private float fixTrans(float trans, float viewSize, float contentSize) {
            float minTrans;
            float maxTrans;
            if (contentSize <= viewSize) {
                minTrans = maxTrans = (viewSize - contentSize) / 2f;
            } else {
                minTrans = viewSize - contentSize;
                maxTrans = 0f;
            }
            if (trans < minTrans) {
                return minTrans - trans;
            }
            if (trans > maxTrans) {
                return maxTrans - trans;
            }
            return 0f;
        }

        private void resetMatrix() {
            Drawable drawable = getDrawable();
            if (drawable == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }
            int imageWidth = drawable.getIntrinsicWidth();
            int imageHeight = drawable.getIntrinsicHeight();
            if (imageWidth <= 0 || imageHeight <= 0) {
                return;
            }
            float scale = (float) getWidth() / imageWidth;
            float scaledHeight = imageHeight * scale;
            float dy = (getHeight() - scaledHeight) / 2f;
            matrix.reset();
            matrix.postScale(scale, scale);
            matrix.postTranslate(0f, dy);
            minScale = scale;
            maxScale = scale * 5f;
            setImageMatrix(matrix);
        }
    }
}
