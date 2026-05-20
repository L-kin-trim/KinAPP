package com.example.kin.update;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.kin.util.AppExecutors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GithubReleaseUpdater {
    private static final String RELEASES_API = "https://api.github.com/repos/L-kin-trim/KinAPP/releases";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private final Activity activity;
    private boolean checkInFlight;
    private boolean checkCompleted;
    private boolean dialogShown;

    public GithubReleaseUpdater(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdates() {
        if (checkInFlight || checkCompleted || dialogShown) {
            return;
        }
        checkInFlight = true;
        AppExecutors.io().execute(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                checkInFlight = false;
                checkCompleted = true;
                if (release == null || !release.isNewerThan(localVersionName())) {
                    return;
                }
                AppExecutors.main(() -> showUpdateDialog(release));
            } catch (Exception ignored) {
                checkInFlight = false;
                checkCompleted = true;
            }
        });
    }

    private ReleaseInfo fetchLatestRelease() throws Exception {
        HttpURLConnection connection = openConnection(RELEASES_API);
        String body = readText(connection.getInputStream());
        JSONArray releases = new JSONArray(body);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject item = releases.getJSONObject(i);
            if (item.optBoolean("draft")) {
                continue;
            }
            ReleaseInfo release = ReleaseInfo.fromJson(item);
            if (!TextUtils.isEmpty(release.apkUrl)) {
                return release;
            }
        }
        return null;
    }

    private void showUpdateDialog(ReleaseInfo release) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        dialogShown = true;
        new AlertDialog.Builder(activity)
                .setTitle("发现新版本 " + release.displayName())
                .setMessage(TextUtils.isEmpty(release.body) ? "是否下载并安装最新版本？" : release.body)
                .setPositiveButton("立即更新", (dialog, which) -> downloadAndInstall(release))
                .setNegativeButton("稍后再说", null)
                .show();
    }

    private void downloadAndInstall(ReleaseInfo release) {
        AppExecutors.io().execute(() -> {
            try {
                File apkFile = downloadApk(release);
                AppExecutors.main(() -> installApk(apkFile));
            } catch (Exception exception) {
                AppExecutors.main(() -> new AlertDialog.Builder(activity)
                        .setTitle("更新失败")
                        .setMessage(TextUtils.isEmpty(exception.getMessage()) ? "安装包下载失败，请稍后重试。" : exception.getMessage())
                        .setPositiveButton("知道了", null)
                        .show());
            }
        });
    }

    private File downloadApk(ReleaseInfo release) throws Exception {
        HttpURLConnection connection = openConnection(release.apkUrl);
        File updateDir = new File(activity.getCacheDir(), "updates");
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            throw new IllegalStateException("无法创建更新缓存目录");
        }
        File apkFile = new File(updateDir, "KinAPP-" + release.safeVersionName() + ".apk");
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(apkFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
        return apkFile;
    }

    private void installApk(File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            settingsIntent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settingsIntent);
            return;
        }
        Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, APK_MIME_TYPE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private HttpURLConnection openConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "KinAPP/" + localVersionName());
        return connection;
    }

    private String localVersionName() {
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return packageInfo.versionName == null ? "" : packageInfo.versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String readText(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            byte[] data = out.toByteArray();
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static final class ReleaseInfo {
        final String tagName;
        final String name;
        final String body;
        final String apkUrl;

        private ReleaseInfo(String tagName, String name, String body, String apkUrl) {
            this.tagName = tagName;
            this.name = name;
            this.body = body;
            this.apkUrl = apkUrl;
        }

        static ReleaseInfo fromJson(JSONObject json) {
            JSONArray assets = json.optJSONArray("assets");
            String apkUrl = "";
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) {
                        continue;
                    }
                    String assetName = asset.optString("name");
                    if (!TextUtils.isEmpty(assetName) && assetName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url");
                        break;
                    }
                }
            }
            return new ReleaseInfo(
                    json.optString("tag_name"),
                    json.optString("name"),
                    json.optString("body"),
                    apkUrl
            );
        }

        String displayName() {
            return TextUtils.isEmpty(name) ? tagName : name;
        }

        boolean isNewerThan(String localVersionName) {
            return normalizedVersion(displayName()) > normalizedVersion(localVersionName);
        }

        String safeVersionName() {
            return displayName().replaceAll("[^a-zA-Z0-9._-]+", "_");
        }

        private static int normalizedVersion(String value) {
            if (TextUtils.isEmpty(value)) {
                return 0;
            }
            String[] parts = value.replaceAll("[^0-9.]", "").split("\\.");
            int major = part(parts, 0);
            int minor = part(parts, 1);
            int patch = part(parts, 2);
            return major * 10000 + minor * 100 + patch;
        }

        private static int part(String[] parts, int index) {
            if (index >= parts.length || TextUtils.isEmpty(parts[index])) {
                return 0;
            }
            try {
                return Integer.parseInt(parts[index]);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
