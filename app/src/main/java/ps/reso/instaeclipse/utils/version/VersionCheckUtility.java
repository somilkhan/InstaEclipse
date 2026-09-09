package ps.reso.instaeclipse.utils.version;

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
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "InstaEclipse-VersionCheck");
        t.setDaemon(true);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private VersionCheckUtility() {}

    public static void checkForUpdates(Context context) {
        EXECUTOR.execute(() -> {
            VersionCheck result = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_CHECK_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "InstaEclipse/" + CURRENT_VERSION);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) body.append(line);
                    }
                    result = new Gson().fromJson(body.toString(), VersionCheck.class);
                }
                connection.disconnect();
            } catch (Throwable ignored) {
                // Update checks are best-effort and must never interrupt the home screen.
            }

            VersionCheck finalResult = result;
            MAIN.post(() -> {
                if (finalResult != null && isNewer(CURRENT_VERSION, finalResult.getLatestVersion())) {
                    showUpdateDialog(context, finalResult.getUpdateUrl(), finalResult.getLatestVersion());
                }
            });
        });
    }

    private static boolean isNewer(String current, String latest) {
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

    private static void showUpdateDialog(Context context, String updateUrl, String newVersion) {
        if (updateUrl == null || updateUrl.trim().isEmpty()) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.ig_update_title))
                .setMessage(context.getString(R.string.ig_update_message, newVersion))
                .setPositiveButton(context.getString(R.string.ig_update_button), (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                    context.startActivity(browserIntent);
                })
                .setNegativeButton(context.getString(R.string.ig_update_later), null)
                .show();
    }
}
