package com.gathin.bingwallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImageView previewImage;
    private TextView titleText;
    private TextView copyrightText;
    private TextView statusText;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewImage = findViewById(R.id.preview_image);
        titleText = findViewById(R.id.title_text);
        copyrightText = findViewById(R.id.copyright_text);
        statusText = findViewById(R.id.status_text);
        refreshButton = findViewById(R.id.refresh_button);

        loadCachedData();
        BingJobService.schedulePeriodic(this);

        refreshButton.setOnClickListener(v -> performRefresh());
        refreshButton.requestFocus();
    }

    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences(BingApi.PREFS_NAME, Context.MODE_PRIVATE);
        String title = prefs.getString("title", getString(R.string.app_name));
        String copyright = prefs.getString("copyright", getString(R.string.daily_description));
        long lastUpdate = prefs.getLong("last_updated", 0);

        titleText.setText(title);
        copyrightText.setText(copyright);
        if (lastUpdate > 0) {
            statusText.setText(android.text.format.DateUtils.getRelativeTimeSpanString(lastUpdate));
        }

        File wall = new File("/sdcard/Android/data/com.gathin.tvhome/files/wallpaper.jpg");
        if (wall.exists()) {
            executor.execute(() -> {
                try {
                    android.graphics.Bitmap b = BitmapFactory.decodeFile(wall.getAbsolutePath());
                    if (b != null) handler.post(() -> previewImage.setImageBitmap(b));
                } catch (Exception ignored) {}
            });
        }
    }

    private void performRefresh() {
        refreshButton.setEnabled(false);
        statusText.setText(R.string.updating);

        executor.execute(() -> {
            boolean success = BingApi.fetchToday(this);
            handler.post(() -> {
                refreshButton.setEnabled(true);
                if (success) {
                    loadCachedData();
                    Toast.makeText(this, R.string.updated_success, Toast.LENGTH_SHORT).show();
                } else {
                    statusText.setText(R.string.update_failed);
                    Toast.makeText(this, R.string.update_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
