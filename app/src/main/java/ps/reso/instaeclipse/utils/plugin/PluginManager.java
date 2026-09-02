package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import ps.reso.instaeclipse.Xposed.Module;
import ps.reso.instaeclipse.plugin.api.InstaEclipsePlugin;
import ps.reso.instaeclipse.plugin.api.PluginContext;
import ps.reso.instaeclipse.plugin.api.PluginLogger;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Secure first-party executable plugin runtime. */
public final class PluginManager {
    public static final int CORE_API = 1;
    public static final String ACTION_INSTALL_PLUGIN = "ps.reso.instaeclipse.ACTION_INSTALL_PLUGIN";
    public static final String ACTION_REQUEST_PENDING = "ps.reso.instaeclipse.ACTION_REQUEST_PENDING_PLUGIN";
    public static final String ACTION_PLUGIN_INSTALLED = "ps.reso.instaeclipse.ACTION_PLUGIN_INSTALLED";
    public static final String EXTRA_URI = "plugin_uri";
    public static final String EXTRA_ID = "plugin_id";
    public static final String EXTRA_VERSION = "plugin_version";
    public static final String EXTRA_SHA256 = "plugin_sha256";

    private static final Gson GSON = new Gson();
    private static final Set<String> LOADED = new HashSet<>();
    private static volatile boolean bootstrapped;

    private PluginManager() {}

    public static void bootstrap(Context context, ClassLoader instagramClassLoader, String instagramVersion) {
        if (bootstrapped) return;
        bootstrapped = true;
        try {
            File root = pluginDirectory(context);
            if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("plugin directory unavailable");
            File[] files = root.listFiles((dir, name) -> name.endsWith(".apk"));
            if (files != null) {
                for (File file : files) {
                    try {
                        loadInstalled(context, instagramClassLoader, instagramVersion, file);
                    } catch (Throwable error) {
                        ModuleLog.line("(InstaEclipse | Plugin): rejected " + file.getName() + ": " + error.getMessage());
                    }
                }
            }
            requestPendingTransfer(context);
        } catch (Throwable error) {
            ModuleLog.line("(InstaEclipse | Plugin): bootstrap failed: " + error);
        }
    }

    public static boolean installFromUri(Context context, ClassLoader instagramClassLoader,
                                         String instagramVersion, Uri uri,
                                         String expectedId, String expectedVersion,
                                         String expectedSha256) {
        File root = pluginDirectory(context);
        File stagingDir = new File(root, ".staging");
        if (!stagingDir.exists() && !stagingDir.mkdirs()) return false;
        File staging = new File(stagingDir, "plugin-" + System.nanoTime() + ".apk");
        try {
            copyUri(context, uri, staging);
            PluginManifest manifest = readManifest(staging);
            if (!safeEquals(expectedId, manifest.id) || !safeEquals(expectedVersion, manifest.version)) {
                throw new SecurityException("catalog/plugin identity mismatch");
            }
            validateManifest(manifest, instagramVersion);
            if (expectedSha256 != null && !expectedSha256.isEmpty()) {
                String actual = sha256(staging);
                if (!expectedSha256.equalsIgnoreCase(actual)) throw new SecurityException("SHA-256 mismatch");
            }
            if (!signerMatchesModule(context, staging)) throw new SecurityException("plugin signer does not match InstaEclipse core");

            File target = new File(root, manifest.id + "-" + manifest.version + ".apk");
            if (target.exists() && !target.delete()) throw new IllegalStateException("old plugin cannot be replaced");
            if (!staging.renameTo(target)) throw new IllegalStateException("plugin commit failed");
            target.setReadOnly();
            loadInstalled(context, instagramClassLoader, instagramVersion, target);
            notifyCompanionInstalled(context, manifest.id, manifest.version);
            ModuleLog.line("(InstaEclipse | Plugin): installed " + manifest.id + " v" + manifest.version);
            return true;
        } catch (Throwable error) {
            ModuleLog.line("(InstaEclipse | Plugin): install failed: " + error);
            if (staging.exists()) staging.delete();
            return false;
        }
    }

    public static boolean isInstalled(Context context, String id) {
        File root = pluginDirectory(context);
        File[] files = root.listFiles((dir, name) -> name.startsWith(id + "-") && name.endsWith(".apk"));
        return files != null && files.length > 0;
    }

    public static void uninstall(Context context, String id) {
        File root = pluginDirectory(context);
        File[] files = root.listFiles((dir, name) -> name.startsWith(id + "-") && name.endsWith(".apk"));
        if (files != null) for (File file : files) file.delete();
        LOADED.remove(id);
    }

    private static void loadInstalled(Context context, ClassLoader instagramClassLoader,
                                      String instagramVersion, File apk) throws Exception {
        PluginManifest manifest = readManifest(apk);
        validateManifest(manifest, instagramVersion);
        if (!signerMatchesModule(context, apk)) throw new SecurityException("untrusted plugin signer");
        if (LOADED.contains(manifest.id)) return;

        apk.setReadOnly();
        File optimized = new File(context.getCodeCacheDir(), "instaeclipse-plugins");
        if (!optimized.exists()) optimized.mkdirs();
        DexClassLoader loader = new DexClassLoader(
                apk.getAbsolutePath(), optimized.getAbsolutePath(), null,
                InstaEclipsePlugin.class.getClassLoader());
        Class<?> entry = Class.forName(manifest.entrypoint, true, loader);
        Object instance = entry.getDeclaredConstructor().newInstance();
        if (!(instance instanceof InstaEclipsePlugin)) throw new IllegalStateException("entrypoint is not an InstaEclipsePlugin");
        InstaEclipsePlugin plugin = (InstaEclipsePlugin) instance;
        if (!manifest.id.equals(plugin.getId()) || !manifest.version.equals(plugin.getVersion())) {
            throw new SecurityException("plugin entrypoint identity mismatch");
        }
        plugin.onLoad(new PluginContext(context, instagramClassLoader, instagramVersion, new PluginLog(manifest.id)));
        LOADED.add(manifest.id);
        ModuleLog.line("(InstaEclipse | Plugin): loaded " + manifest.id + " v" + manifest.version);
    }

    private static void validateManifest(PluginManifest manifest, String instagramVersion) {
        if (manifest.schema != 1) throw new IllegalStateException("unsupported plugin schema");
        if (manifest.id == null || !manifest.id.matches("[a-z0-9][a-z0-9._-]{1,63}")) throw new SecurityException("invalid plugin id");
        if (manifest.entrypoint == null || manifest.entrypoint.length() > 200) throw new SecurityException("invalid entrypoint");
        if (manifest.min_core_api > CORE_API || manifest.max_core_api < CORE_API) throw new IllegalStateException("core API incompatible");
        long ig = parseLong(instagramVersion);
        if (ig < manifest.min_instagram_version || ig > manifest.max_instagram_version) throw new IllegalStateException("Instagram version incompatible");
    }

    private static PluginManifest readManifest(File apk) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry("assets/plugin.json");
            if (entry == null) entry = zip.getEntry("plugin.json");
            if (entry == null) throw new IllegalStateException("plugin manifest missing");
            try (InputStream input = zip.getInputStream(entry)) {
                PluginManifest manifest = GSON.fromJson(new String(readAll(input), java.nio.charset.StandardCharsets.UTF_8), PluginManifest.class);
                if (manifest == null) throw new IllegalStateException("plugin manifest invalid");
                return manifest;
            }
        }
    }

    private static void copyUri(Context context, Uri uri, File target) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IllegalStateException("plugin URI cannot be opened");
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            output.flush();
        }
    }

    private static boolean signerMatchesModule(Context context, File plugin) {
        try {
            PackageManager pm = context.getPackageManager();
            int flags = PackageManager.GET_SIGNING_CERTIFICATES;
            PackageInfo module = pm.getPackageArchiveInfo(Module.moduleSourceDir, flags);
            PackageInfo candidate = pm.getPackageArchiveInfo(plugin.getAbsolutePath(), flags);
            if (module == null || candidate == null || module.signingInfo == null || candidate.signingInfo == null) return false;
            Set<String> trusted = signatureDigests(module.signingInfo.getSigningCertificateHistory());
            if (trusted.isEmpty()) trusted = signatureDigests(module.signingInfo.getApkContentsSigners());
            Set<String> actual = signatureDigests(candidate.signingInfo.getSigningCertificateHistory());
            if (actual.isEmpty()) actual = signatureDigests(candidate.signingInfo.getApkContentsSigners());
            for (String digest : actual) if (trusted.contains(digest)) return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Set<String> signatureDigests(Signature[] signatures) throws Exception {
        Set<String> result = new HashSet<>();
        if (signatures == null) return result;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Signature signature : signatures) result.add(hex(digest.digest(signature.toByteArray())));
        return result;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    private static byte[] readAll(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b));
        return out.toString();
    }

    private static File pluginDirectory(Context context) {
        return new File(context.getFilesDir(), "instaeclipse/plugins");
    }

    private static void requestPendingTransfer(Context context) {
        try {
            Intent intent = new Intent(ACTION_REQUEST_PENDING);
            intent.setPackage(CommonUtils.MY_PACKAGE_NAME);
            context.sendBroadcast(intent);
        } catch (Throwable ignored) {
        }
    }

    private static void notifyCompanionInstalled(Context context, String id, String version) {
        try {
            Intent intent = new Intent(ACTION_PLUGIN_INSTALLED);
            intent.setPackage(CommonUtils.MY_PACKAGE_NAME);
            intent.putExtra(EXTRA_ID, id);
            intent.putExtra(EXTRA_VERSION, version);
            context.sendBroadcast(intent);
        } catch (Throwable ignored) {
        }
    }

    private static boolean safeEquals(String expected, String actual) {
        return expected != null && expected.equals(actual);
    }

    private static long parseLong(String value) {
        try { return Long.parseLong(value); } catch (Throwable ignored) { return -1; }
    }

    private static final class PluginManifest {
        int schema;
        String id;
        String name;
        String description;
        String version;
        int min_core_api;
        int max_core_api;
        String entrypoint;
        long min_instagram_version;
        long max_instagram_version;
    }

    private static final class PluginLog implements PluginLogger {
        private final String id;
        PluginLog(String id) { this.id = id; }
        @Override public void info(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): " + message); }
        @Override public void warn(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): WARN " + message); }
        @Override public void error(String message, Throwable throwable) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): ERROR " + message + " :: " + throwable); }
    }
}
