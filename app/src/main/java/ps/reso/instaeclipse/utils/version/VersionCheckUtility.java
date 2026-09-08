package ps.reso.instaeclipse.utils.version;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ps.reso.instaeclipse.BuildConfig;
import ps.reso.instaeclipse.R;

public final class VersionCheckUtility {
    private static final String CURRENT_VERSION = BuildConfig.VERSION_NAME;
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/somilkhan/InstaEclipse/main/version.json";
    private static final String ALLOWED_UPDATE_HOST = "github.com";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "InstaEclipse-VersionCheck");
        t.setDaemon(true);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private VersionCheckUtility() {}

    public static void checkForUpdates(Context context) {
        EXECUTOR.execute(() -> {
            VersionCheck result = fetchVersion();
            if (result == null) return;

            MAIN.post(() -> {
                if (!(context instanceof Activity)) return;
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (isNewer(CURRENT_VERSION, result.getLatestVersion())) {
                    showUpdateDialog(activity, result.getUpdateUrl(), result.getLatestVersion());
                }
            });
        });
    }

    private static VersionCheck fetchVersion() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(VERSION_CHECK_URL).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "InstaEclipse/" + CURRENT_VERSION);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return new Gson().fromJson(body.toString(), VersionCheck.class);
        } catch (Throwable ignored) {
            // Update checks are best-effort and must never interrupt the home screen.
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isNewer(String current, String latest) {
        if (current == null || latest == null || current.trim().isEmpty() || latest.trim().isEmpty()) return false;
        try {
            String[] a = current.split("\\.");
            String[] b = latest.split("\\.");
            int count = Math.max(a.length, b.length);
            for (int i = 0; i < count; i++) {
                int av = i < a.length ? Integer.parseInt(a[i]) : 0;
                int bv = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (bv != av) return bv > av;
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static void showUpdateDialog(Activity activity, String updateUrl, String newVersion) {
        if (!isTrustedUpdateUrl(updateUrl) || newVersion == null || newVersion.trim().isEmpty()) return;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.ig_update_title))
                .setMessage(activity.getString(R.string.ig_update_message, newVersion))
                .setPositiveButton(activity.getString(R.string.ig_update_button), (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                    if (browserIntent.resolveActivity(activity.getPackageManager()) != null) {
                        activity.startActivity(browserIntent);
                    }
                })
                .setNegativeButton(activity.getString(R.string.ig_update_later), null)
                .show();
    }

    private static boolean isTrustedUpdateUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return "https".equalsIgnoreCase(scheme)
                    && ALLOWED_UPDATE_HOST.equalsIgnoreCase(host)
                    && uri.getUserInfo() == null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
