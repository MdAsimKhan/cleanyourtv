package com.gathin.bingwallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class BingApi {
    private static final String TAG = "BingWallpaper";
    private static final String API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US";
    private static final Uri THEME_URI = Uri.parse("content://com.gathin.tvhome.theme/wallpaper.jpg");
    public static final String PREFS_NAME = "bing_wallpaper";

    private BingApi() {}

    public static boolean fetchToday(Context context) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "CleanYourTV-BingAddon/1.0");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "API response not OK: " + conn.getResponseCode());
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            JSONArray images = json.optJSONArray("images");
            if (images == null || images.length() == 0) return false;

            JSONObject item = images.getJSONObject(0);
            String urlbase = item.getString("urlbase");
            String title = item.optString("title", "Bing Daily");
            String copyright = item.optString("copyright", "");
            String startDate = item.optString("startdate", "");

            String imgUrl = "https://www.bing.com" + urlbase + "_1920x1080.jpg";
            boolean written = downloadImage(context, imgUrl);
            if (written) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("title", title)
                        .putString("copyright", copyright)
                        .putString("startdate", startDate)
                        .putLong("last_updated", System.currentTimeMillis())
                        .apply();
                Log.i(TAG, "Successfully updated Bing daily wallpaper: " + title);
            }
            return written;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching Bing wallpaper: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static boolean downloadImage(Context context, String imgUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(imgUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return false;

            // Try ContentProvider first (works when WRITE_SECURE_SETTINGS is granted)
            try (InputStream in = conn.getInputStream();
                 OutputStream out = context.getContentResolver().openOutputStream(THEME_URI)) {
                if (out != null) {
                    copy(in, out);
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Provider write failed, falling back to direct storage: " + e.getMessage());
            }

            // Fallback: direct path if accessible
            File dir = new File("/sdcard/Android/data/com.gathin.tvhome/files");
            if (!dir.exists()) dir.mkdirs();
            File target = new File(dir, "wallpaper.jpg");
            try (InputStream in = url.openStream();
                 OutputStream out = new FileOutputStream(target)) {
                copy(in, out);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download image stream: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }
}
