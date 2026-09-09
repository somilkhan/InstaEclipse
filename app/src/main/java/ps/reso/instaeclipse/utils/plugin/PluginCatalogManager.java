package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/** Fetches the authoritative plugin catalog; plugin metadata is never compiled into the UI. */
public final class PluginCatalogManager {
    private static final String CATALOG_URL = "https://raw.githubusercontent.com/somilkhan/InstaEclipse/stability/latest-instagram/plugins/catalog.json";
    private static final String CACHE_FILE = "plugin-catalog.json";
    private static final String ETAG_FILE = "plugin-catalog.etag";
    private static final long MAX_CACHE_AGE_MS = 24L * 60L * 60L * 1000L;
    private static final Gson GSON = new Gson();
    private PluginCatalogManager() {}

    public static Catalog load(Context context, boolean forceRefresh) throws Exception {
        File cache = new File(context.getFilesDir(), CACHE_FILE);
        try {
            Catalog remote = fetch(context, cache, forceRefresh ? null : readText(new File(context.getFilesDir(), ETAG_FILE)));
            if (remote != null) return validate(remote);
        } catch (Exception error) {
            if (!cache.exists()) throw error;
        }
        if (!cache.exists()) throw new IllegalStateException("Plugin catalog unavailable");
        return validate(GSON.fromJson(readText(cache), Catalog.class));
    }

    private static Catalog fetch(Context context, File cache, String etag) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(15000); connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json"); connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.1");
        if (etag != null && !etag.isEmpty()) connection.setRequestProperty("If-None-Match", etag);
        try {
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED && cache.exists()) return null;
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
            byte[] bytes = readAll(connection.getInputStream());
            Catalog catalog = validate(GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), Catalog.class));
            writeAtomically(cache, bytes);
            String newEtag = connection.getHeaderField("ETag");
            if (newEtag != null && !newEtag.isEmpty()) writeText(new File(context.getFilesDir(), ETAG_FILE), newEtag);
            return catalog;
        } finally { connection.disconnect(); }
    }

    private static Catalog validate(Catalog catalog) {
        if (catalog == null || catalog.schema != 1) throw new IllegalStateException("Unsupported plugin catalog schema");
        if (catalog.plugins == null) catalog.plugins = Collections.emptyList();
        for (PluginEntry p : catalog.plugins) {
            if (p == null || p.id == null || p.id.isEmpty() || p.version == null || p.version.isEmpty()) throw new IllegalStateException("Invalid plugin catalog entry");
            if (!p.version.matches("\\d+\\.\\d+\\.\\d+")) throw new IllegalStateException("Invalid plugin version: " + p.id);
            if (p.remote && (p.downloadUrl == null || p.downloadUrl.isEmpty() || p.sha256Url == null || p.sha256Url.isEmpty())) throw new IllegalStateException("Incomplete remote plugin: " + p.id);
        }
        return catalog;
    }

    private static void writeAtomically(File target, byte[] bytes) throws Exception {
        File temp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) { output.write(bytes); output.flush(); }
        if (!temp.renameTo(target)) { if (target.exists() && !target.delete()) throw new IllegalStateException("catalog cache replace failed"); if (!temp.renameTo(target)) throw new IllegalStateException("catalog cache replace failed"); }
    }
    private static void writeText(File file, String value) throws Exception { try (FileOutputStream output = new FileOutputStream(file)) { output.write(value.getBytes(StandardCharsets.UTF_8)); } }
    private static String readText(File file) throws Exception { try (InputStream input = new FileInputStream(file)) { return new String(readAll(input), StandardCharsets.UTF_8).trim(); } }
    private static byte[] readAll(InputStream input) throws Exception { ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); return output.toByteArray(); }

    public static final class Catalog {
        int schema;
        @SerializedName("catalog_version") public long catalogVersion;
        public List<PluginEntry> plugins;
    }

    public static final class PluginEntry {
        public String id;
        public String name;
        public String description;
        public String version;
        @SerializedName("min_core_api") public int minCoreApi;
        @SerializedName("max_core_api") public int maxCoreApi;
        @SerializedName("min_instagram_version") public String minInstagramVersion;
        @SerializedName("max_instagram_version") public String maxInstagramVersion;
        public String packageName;
        public String entrypoint;
        public String channel;
        public boolean remote;
        @SerializedName("download_url") public String downloadUrl;
        @SerializedName("sha256_url") public String sha256Url;
        @SerializedName("release_notes") public String releaseNotes;
        @SerializedName("mandatory") public boolean mandatory;
    }
}
