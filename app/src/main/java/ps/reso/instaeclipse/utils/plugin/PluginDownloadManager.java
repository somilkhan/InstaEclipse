package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Companion-side downloader and secure URI hand-off to the injected Instagram process. */
public final class PluginDownloadManager {
    private static final String PENDING_DIR = "plugin-pending";
    private static final String AUTHORITY_SUFFIX = ".fileprovider";

    private PluginDownloadManager() {}

    public static boolean isQueued(Context context, String id, String version) {
        return new File(new File(context.getFilesDir(), PENDING_DIR), id + "-" + version + ".apk").exists();
    }

    public static void downloadAndQueue(Context context, String id, String version, String url,
                                        String sha256, String instagramVersion) throws Exception {
        if (id == null || version == null || url == null || url.isEmpty()) throw new IllegalArgumentException("plugin download metadata incomplete");
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("plugin staging unavailable");
        File target = new File(dir, id + "-" + version + ".apk");
        if (target.exists() && !target.delete()) throw new IllegalStateException("old plugin package cannot be replaced");

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream");
        connection.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/0.6");
        try {
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) throw new IllegalStateException("HTTP " + response);
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
                // Android 14+ requires dynamically loaded code to be read-only; make the
                // staging file immutable before writing to close the overwrite race.
                if (!target.setReadOnly()) throw new SecurityException("plugin staging cannot be made read-only");
                byte[] buffer = new byte[32 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
            }
        } finally {
            connection.disconnect();
        }
        File checksum = new File(dir, target.getName() + ".sha256");
        try (FileOutputStream output = new FileOutputStream(checksum)) {
            output.write((sha256 == null ? "" : sha256).getBytes(StandardCharsets.UTF_8));
        }
        checksum.setReadOnly();
        transferFile(context, target, id, version, sha256);
    }

    public static void transferPending(Context context) {
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".apk"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            int split = name.lastIndexOf('-');
            if (split <= 0) continue;
            String id = name.substring(0, split);
            String version = name.substring(split + 1, name.length() - 4);
            String sha256 = "";
            File checksum = new File(dir, name + ".sha256");
            if (checksum.exists()) {
                try (InputStream input = new java.io.FileInputStream(checksum)) {
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] bytes = new byte[4096];
                    int read;
                    while ((read = input.read(bytes)) != -1) buffer.write(bytes, 0, read);
                    sha256 = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
                } catch (Throwable ignored) {}
            }
            transferFile(context, file, id, version, sha256);
        }
    }

    public static void markInstalled(Context context, String id, String version) {
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        new File(dir, id + "-" + version + ".apk").delete();
        new File(dir, id + "-" + version + ".apk.sha256").delete();
        context.getSharedPreferences("instaeclipse_cache", Context.MODE_PRIVATE)
                .edit().putBoolean("plugin_installed_" + id, true).apply();
    }

    private static void transferFile(Context context, File file, String id, String version, String sha256) {
        try {
            String authority = context.getPackageName() + AUTHORITY_SUFFIX;
            Uri uri = FileProvider.getUriForFile(context, authority, file);
            String instagramPackage = CommonUtils.SUPPORTED_PACKAGES.get(0);
            context.grantUriPermission(instagramPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = new Intent(PluginManager.ACTION_INSTALL_PLUGIN);
            intent.setPackage(instagramPackage);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(PluginManager.EXTRA_URI, uri.toString());
            intent.putExtra(PluginManager.EXTRA_ID, id);
            intent.putExtra(PluginManager.EXTRA_VERSION, version);
            intent.putExtra(PluginManager.EXTRA_SHA256, sha256 == null ? "" : sha256);
            context.sendBroadcast(intent);
            ModuleLog.line("(InstaEclipse | Plugin): queued " + id + " v" + version + " for Instagram");
        } catch (Throwable error) {
            ModuleLog.line("(InstaEclipse | Plugin): transfer failed: " + error);
        }
    }
}
