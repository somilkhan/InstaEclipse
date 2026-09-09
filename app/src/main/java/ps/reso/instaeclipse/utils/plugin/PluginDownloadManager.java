package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Companion-side downloader. Downloaded code is never executed before Android installs it. */
public final class PluginDownloadManager {
    private static final String PENDING_DIR = "plugin-pending";
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
        connection.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.0");
        try {
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) throw new IllegalStateException("HTTP " + response);
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[32 * 1024]; int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
            }
        } finally { connection.disconnect(); }

        // The installer activity performs SHA-256, manifest and signer verification before install.
        File checksum = new File(dir, target.getName() + ".sha256");
        try (FileOutputStream output = new FileOutputStream(checksum)) {
            output.write((sha256 == null ? "" : sha256).getBytes(StandardCharsets.UTF_8));
        }
        launchInstaller(context, target, id, version, sha256);
    }

    public static void transferPending(Context context) {
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".apk"));
        if (files == null) return;
        for (File file : files) {
            String name = file.getName(); int split = name.lastIndexOf('-');
            if (split <= 0) continue;
            String id = name.substring(0, split);
            String version = name.substring(split + 1, name.length() - 4);
            File checksum = new File(dir, name + ".sha256");
            String sha = readText(checksum);
            launchInstaller(context, file, id, version, sha);
        }
    }

    public static void markInstalled(Context context, String id, String version) {
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        new File(dir, id + "-" + version + ".apk").delete();
        new File(dir, id + "-" + version + ".apk.sha256").delete();
    }

    private static void launchInstaller(Context context, File file, String id, String version, String sha256) {
        try {
            androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(context, PluginInstallActivity.class)
                    .setData(androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file))
                    .putExtra(PluginManager.EXTRA_ID, id)
                    .putExtra(PluginManager.EXTRA_VERSION, version)
                    .putExtra(PluginManager.EXTRA_SHA256, sha256 == null ? "" : sha256)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            ModuleLog.line("(InstaEclipse | Plugin): ready " + id + " v" + version + " for user-authorized installation");
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): installer launch failed: " + error); }
    }

    private static String readText(File file) {
        if (!file.exists()) return "";
        try (InputStream input = new java.io.FileInputStream(file)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
            while ((n = input.read(b)) != -1) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
        } catch (Throwable ignored) { return ""; }
    }
}
