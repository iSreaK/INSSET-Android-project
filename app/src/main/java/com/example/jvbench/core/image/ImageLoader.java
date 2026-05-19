package com.example.jvbench.core.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageLoader() {
    }

    public static void loadInto(String url, ImageView target) {
        if (target == null) {
            return;
        }
        if (url == null || url.isBlank()) {
            target.setImageDrawable(null);
            return;
        }
        Object tag = url;
        target.setTag(tag);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(url);
            MAIN.post(() -> {
                if (tag.equals(target.getTag())) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap downloadBitmap(String url) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.connect();
            if (connection.getResponseCode() != 200) {
                return null;
            }
            input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception exception) {
            return null;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception ignored) {
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
