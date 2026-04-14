package com.example.kin.ui.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.kin.util.AppExecutors;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteImageLoader {
    private final LruCache<String, Bitmap> cache;

    public RemoteImageLoader() {
        int maxSize = (int) (Runtime.getRuntime().maxMemory() / 1024L / 8L);
        this.cache = new LruCache<>(maxSize);
    }

    public void load(ImageView imageView, String url) {
        imageView.setTag(url);
        Bitmap cached = cache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(12000);
                try (InputStream stream = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    if (bitmap != null) {
                        cache.put(url, bitmap);
                        AppExecutors.main(() -> {
                            if (url.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }
}
