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

import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Companion-side downloader and secure URI hand-off to the injected Instagram process. */
public final class PluginDownloadManager {
    private static final String PENDING_DIR = "plugin-pending";
    private static final String AUTHORITY_SUFFIX = ".fileprovider";

    private PluginDownloadManager() {}

    public static void downloadAndQueue(Context context, String id, String version, String url,
                                        String sha256, String instagramVersion) throws Exception {
        if (id == null || version == null || url == null || url.isEmpty()) throw new IllegalArgumentException("plugin download metadata incomplete");
        File dir = new File(context.getFilesDir(), PENDING_DIR);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("plugin staging unavailable");
        File target = new File(dir, id + "-" + version + ".apk");
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
                byte[] buffer = new byte[32 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
            }
        } finally {
            connection.disconnect();
        }
        target.setReadOnly();
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
            transferFile(context, file, id, version, "");
        }
    }

    private static void transferFile(Context context, File file, String id, String version, String sha256) {
        try {
            String authority = context.getPackageName() + AUTHORITY_SUFFIX;
            Uri uri = FileProvider.getUriForFile(context, authority, file);
            context.grantUriPermission(CommonUtils.SUPPORTED_PACKAGES.get(0), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = new Intent(PluginManager.ACTION_INSTALL_PLUGIN);
            intent.setPackage(CommonUtils.SUPPORTED_PACKAGES.get(0));
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
