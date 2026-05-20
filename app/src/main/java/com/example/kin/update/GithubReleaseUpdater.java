package com.example.kin.update;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.kin.ui.common.KinUi;
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
    private boolean downloadInFlight;
    private AlertDialog progressDialog;
    private ProgressBar downloadProgressBar;
    private TextView downloadProgressText;

    public GithubReleaseUpdater(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdates() {
        checkForUpdates(false);
    }

    public void checkForUpdates(boolean manual) {
        if (checkInFlight || (!manual && (checkCompleted || dialogShown))) {
            return;
        }
        checkInFlight = true;
        AppExecutors.io().execute(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                checkInFlight = false;
                checkCompleted = true;
                if (release == null || !release.isNewerThan(currentVersionName())) {
                    if (manual) {
                        AppExecutors.main(() -> showNoUpdateDialog(release));
                    }
                    return;
                }
                AppExecutors.main(() -> showUpdateDialog(release));
            } catch (Exception exception) {
                checkInFlight = false;
                checkCompleted = true;
                if (manual) {
                    AppExecutors.main(() -> showManualCheckFailedDialog(exception));
                }
            }
        });
    }

    public String currentVersionName() {
        return localVersionName();
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
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("\u53d1\u73b0\u65b0\u7248\u672c " + release.displayName())
                .setMessage(TextUtils.isEmpty(release.body) ? "\u662f\u5426\u4e0b\u8f7d\u5e76\u5b89\u88c5\u6700\u65b0\u7248\u672c\uff1f" : release.body)
                .setPositiveButton("\u7acb\u5373\u66f4\u65b0", null)
                .setNegativeButton("\u7a0d\u540e\u518d\u8bf4", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            dialog.dismiss();
            downloadAndInstall(release);
        });
    }

    private void showNoUpdateDialog(ReleaseInfo release) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        String latest = release == null ? "\u672a\u627e\u5230\u53ef\u7528 APK \u53d1\u884c\u7248" : release.displayName();
        new AlertDialog.Builder(activity)
                .setTitle("\u5df2\u662f\u6700\u65b0\u7248\u672c")
                .setMessage("\u5f53\u524d\u7248\u672c\uff1a" + currentVersionName() + "\n\u6700\u65b0\u7248\u672c\uff1a" + latest)
                .setPositiveButton("\u77e5\u9053\u4e86", null)
                .show();
    }

    private void showManualCheckFailedDialog(Exception exception) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("\u68c0\u67e5\u66f4\u65b0\u5931\u8d25")
                .setMessage(TextUtils.isEmpty(exception.getMessage()) ? "\u65e0\u6cd5\u8fde\u63a5 GitHub \u53d1\u884c\u7248\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002" : exception.getMessage())
                .setPositiveButton("\u77e5\u9053\u4e86", null)
                .show();
    }

    private void downloadAndInstall(ReleaseInfo release) {
        if (downloadInFlight) {
            return;
        }
        File cachedApk = apkFileFor(release);
        if (isCompleteApk(cachedApk, release)) {
            installApk(cachedApk);
            return;
        }
        downloadInFlight = true;
        showDownloadProgressDialog(release);
        AppExecutors.io().execute(() -> {
            try {
                File apkFile = downloadApk(release);
                AppExecutors.main(() -> {
                    downloadInFlight = false;
                    dismissProgressDialog();
                    installApk(apkFile);
                });
            } catch (Exception exception) {
                AppExecutors.main(() -> {
                    downloadInFlight = false;
                    dismissProgressDialog();
                    new AlertDialog.Builder(activity)
                            .setTitle("\u66f4\u65b0\u5931\u8d25")
                            .setMessage(TextUtils.isEmpty(exception.getMessage()) ? "\u5b89\u88c5\u5305\u4e0b\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002" : exception.getMessage())
                            .setPositiveButton("\u77e5\u9053\u4e86", null)
                            .show();
                });
            }
        });
    }

    private File downloadApk(ReleaseInfo release) throws Exception {
        HttpURLConnection connection = openConnection(release.apkUrl);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("\u4e0b\u8f7d\u5931\u8d25\uff1aHTTP " + status);
        }
        File updateDir = new File(activity.getCacheDir(), "updates");
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            throw new IllegalStateException("\u65e0\u6cd5\u521b\u5efa\u66f4\u65b0\u7f13\u5b58\u76ee\u5f55");
        }
        File apkFile = apkFileFor(release);
        long totalBytes = release.apkSize > 0L ? release.apkSize : connection.getContentLengthLong();
        publishDownloadProgress(0L, totalBytes);
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(apkFile)) {
            byte[] buffer = new byte[8192];
            int length;
            long downloadedBytes = 0L;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                downloadedBytes += length;
                publishDownloadProgress(downloadedBytes, totalBytes);
            }
        }
        return apkFile;
    }

    private void showDownloadProgressDialog(ReleaseInfo release) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(KinUi.dp(activity, 4), KinUi.dp(activity, 8), KinUi.dp(activity, 4), KinUi.dp(activity, 2));

        downloadProgressText = KinUi.muted(activity, "\u51c6\u5907\u4e0b\u8f7d...", 14);
        root.addView(downloadProgressText);

        downloadProgressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        downloadProgressBar.setMax(100);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.topMargin = KinUi.dp(activity, 12);
        root.addView(downloadProgressBar, progressParams);

        progressDialog = new AlertDialog.Builder(activity)
                .setTitle("\u6b63\u5728\u4e0b\u8f7d " + release.displayName())
                .setView(root)
                .setCancelable(false)
                .show();
    }

    private void publishDownloadProgress(long downloadedBytes, long totalBytes) {
        AppExecutors.main(() -> {
            if (downloadProgressBar == null || downloadProgressText == null) {
                return;
            }
            if (totalBytes > 0L) {
                int progress = (int) Math.min(100L, downloadedBytes * 100L / totalBytes);
                downloadProgressBar.setIndeterminate(false);
                downloadProgressBar.setProgress(progress);
                downloadProgressText.setText("\u5df2\u4e0b\u8f7d " + formatBytes(downloadedBytes)
                        + " / " + formatBytes(totalBytes) + " (" + progress + "%)");
            } else {
                downloadProgressBar.setIndeterminate(true);
                downloadProgressText.setText("\u5df2\u4e0b\u8f7d " + formatBytes(downloadedBytes));
            }
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        downloadProgressBar = null;
        downloadProgressText = null;
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

    private File apkFileFor(ReleaseInfo release) {
        File updateDir = new File(activity.getCacheDir(), "updates");
        return new File(updateDir, "KinAPP-" + release.safeVersionName() + ".apk");
    }

    private boolean isCompleteApk(File apkFile, ReleaseInfo release) {
        return apkFile.exists() && apkFile.isFile() && apkFile.length() > 0L
                && (release.apkSize <= 0L || apkFile.length() == release.apkSize);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0L) {
            return "0 MB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024f / 1024f);
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
        final long apkSize;

        private ReleaseInfo(String tagName, String name, String body, String apkUrl, long apkSize) {
            this.tagName = tagName;
            this.name = name;
            this.body = body;
            this.apkUrl = apkUrl;
            this.apkSize = apkSize;
        }

        static ReleaseInfo fromJson(JSONObject json) {
            JSONArray assets = json.optJSONArray("assets");
            String apkUrl = "";
            long apkSize = 0L;
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) {
                        continue;
                    }
                    String assetName = asset.optString("name");
                    if (!TextUtils.isEmpty(assetName) && assetName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url");
                        apkSize = asset.optLong("size", 0L);
                        break;
                    }
                }
            }
            return new ReleaseInfo(
                    json.optString("tag_name"),
                    json.optString("name"),
                    json.optString("body"),
                    apkUrl,
                    apkSize
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
