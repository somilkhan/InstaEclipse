package ps.reso.instaeclipse.utils.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import dalvik.system.PathClassLoader;
import ps.reso.instaeclipse.Xposed.Module;
import ps.reso.instaeclipse.plugin.api.InstaEclipsePlugin;
import ps.reso.instaeclipse.plugin.api.PluginContext;
import ps.reso.instaeclipse.plugin.api.PluginLogger;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;
import de.robv.android.xposed.XSharedPreferences;

/** Runtime for signed, independently installed InstaEclipse plugin APKs. */
public final class PluginManager {
    public static final int CORE_API = 1;
    public static final String ACTION_INSTALL_PLUGIN = "ps.reso.instaeclipse.ACTION_INSTALL_PLUGIN";
    public static final String ACTION_REQUEST_PENDING = "ps.reso.instaeclipse.ACTION_REQUEST_PENDING_PLUGIN";
    public static final String ACTION_PLUGIN_INSTALLED = "ps.reso.instaeclipse.ACTION_PLUGIN_INSTALLED";
    public static final String ACTION_RESTART_INSTAGRAM = "ps.reso.instaeclipse.ACTION_RESTART_INSTAGRAM";
    public static final String RESTART_PERMISSION = "ps.reso.instaeclipse.permission.RESTART_INSTAGRAM";
    public static final String EXTRA_URI = "plugin_uri";
    public static final String EXTRA_ID = "plugin_id";
    public static final String EXTRA_VERSION = "plugin_version";
    public static final String EXTRA_PACKAGE = "plugin_package";
    public static final String EXTRA_SHA256 = "plugin_sha256";
    public static final String META_PLUGIN = "ps.reso.instaeclipse.plugin";
    public static final String META_PLUGIN_ID = "ps.reso.instaeclipse.plugin.id";
    public static final String META_PLUGIN_VERSION = "ps.reso.instaeclipse.plugin.version";
    public static final String META_PLUGIN_ENTRYPOINT = "ps.reso.instaeclipse.plugin.entrypoint";
    private static final String PLUGIN_PREFS = "instaeclipse_plugins";
    private static final Gson GSON = new Gson();
    private static final Set<String> LOADED = new HashSet<>();
    private static volatile boolean bootstrapped;
    private static volatile boolean restartReceiverRegistered;
    private PluginManager() {}

    public static synchronized void bootstrap(Context context, ClassLoader instagramClassLoader, String instagramVersion) {
        if (bootstrapped) return;
        bootstrapped = true;
        try {
            registerRestartReceiver(context.getApplicationContext());
            PackageManager pm = context.getPackageManager();
            int flags = PackageManager.GET_META_DATA | PackageManager.GET_SIGNING_CERTIFICATES;
            List<PackageInfo> packages = pm.getInstalledPackages(flags);
            for (PackageInfo info : packages) {
                ApplicationInfo app = info.applicationInfo;
                if (app == null || app.metaData == null || !app.metaData.getBoolean(META_PLUGIN, false)) continue;
                try { loadInstalled(context, instagramClassLoader, instagramVersion, info); }
                catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): rejected " + info.packageName + ": " + error.getMessage()); }
            }
            requestPendingTransfer(context);
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): bootstrap failed: " + error); }
    }

    public static boolean installFromUri(Context context, ClassLoader ignored, String ignoredInstagramVersion, Uri uri, String expectedId, String expectedVersion, String expectedPackageName, String expectedSha256) {
        try {
            Intent intent = new Intent(context, PluginInstallActivity.class).setData(uri)
                    .putExtra(EXTRA_ID, expectedId).putExtra(EXTRA_VERSION, expectedVersion)
                    .putExtra(EXTRA_PACKAGE, expectedPackageName).putExtra(EXTRA_SHA256, expectedSha256 == null ? "" : expectedSha256)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent); return true;
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): installer launch failed: " + error); return false; }
    }

    public static boolean isInstalled(Context context, String id) { return findPlugin(context, id) != null; }
    public static String getInstalledVersion(Context context, String id) { PackageInfo info = findPlugin(context, id); return info == null ? null : info.versionName; }

    public static boolean isEnabled(Context context, String id) {
        if (!isInstalled(context, id)) return false;
        try {
            if (CommonUtils.MY_PACKAGE_NAME.equals(context.getPackageName())) return context.getSharedPreferences(PLUGIN_PREFS, Context.MODE_PRIVATE).getBoolean("enabled_" + id, true);
            XSharedPreferences prefs = new XSharedPreferences(CommonUtils.MY_PACKAGE_NAME, PLUGIN_PREFS); prefs.reload(); return prefs.getBoolean("enabled_" + id, true);
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): state read failed for " + id + ": " + error.getMessage()); return true; }
    }

    public static void setEnabled(Context context, String id, boolean enabled) {
        if (!CommonUtils.MY_PACKAGE_NAME.equals(context.getPackageName())) return;
        context.getSharedPreferences(PLUGIN_PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled_" + id, enabled).apply();
    }

    public static void requestUninstall(Context context, String id) {
        PackageInfo info = findPlugin(context, id); if (info == null) return;
        context.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + info.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    public static void uninstall(Context context, String id) { requestUninstall(context, id); }

    /** Requests the injected Instagram process to terminate, then relaunches it. */
    public static boolean restartInstagram(Context context) {
        try {
            String packageName = null;
            for (String candidate : CommonUtils.SUPPORTED_PACKAGES) {
                try { context.getPackageManager().getPackageInfo(candidate, 0); packageName = candidate; break; }
                catch (Throwable ignored) {}
            }
            if (packageName == null) { ModuleLog.line("(InstaEclipse | Plugin): no supported Instagram package installed"); return false; }
            context.sendBroadcast(new Intent(ACTION_RESTART_INSTAGRAM).setPackage(packageName));
            final String target = packageName;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(target);
                    if (launch != null) { launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); context.startActivity(launch); }
                } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): Instagram relaunch failed: " + error); }
            }, 1200L);
            return true;
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): restart failed: " + error); return false; }
    }

    private static PackageInfo findPlugin(Context context, String id) {
        try {
            int flags = PackageManager.GET_META_DATA | PackageManager.GET_SIGNING_CERTIFICATES;
            for (PackageInfo info : context.getPackageManager().getInstalledPackages(flags)) {
                ApplicationInfo app = info.applicationInfo;
                if (app != null && app.metaData != null && app.metaData.getBoolean(META_PLUGIN, false) && id.equals(app.metaData.getString(META_PLUGIN_ID))) return info;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void loadInstalled(Context context, ClassLoader instagramClassLoader, String instagramVersion, PackageInfo info) throws Exception {
        ApplicationInfo app = info.applicationInfo;
        String id = app.metaData.getString(META_PLUGIN_ID), version = app.metaData.getString(META_PLUGIN_VERSION), entrypoint = app.metaData.getString(META_PLUGIN_ENTRYPOINT);
        if (id == null || version == null || entrypoint == null) throw new SecurityException("plugin metadata incomplete");
        if (!isEnabled(context, id)) { ModuleLog.line("(InstaEclipse | Plugin): disabled " + id); return; }
        if (!signerMatchesCore(context, info)) throw new SecurityException("plugin signer does not match InstaEclipse core");
        PluginManifest manifest = readManifest(app.sourceDir); validateManifest(manifest, instagramVersion);
        if (!id.equals(manifest.id) || !entrypoint.equals(manifest.entrypoint)) throw new SecurityException("plugin metadata/manifest mismatch");
        if (!version.equals(info.versionName) || !version.equals(manifest.version)) throw new SecurityException("plugin version metadata mismatch");
        if (LOADED.contains(id)) return;
        PathClassLoader loader = new PathClassLoader(app.sourceDir, null, InstaEclipsePlugin.class.getClassLoader());
        Class<?> entry = Class.forName(entrypoint, true, loader); Object instance = entry.getDeclaredConstructor().newInstance();
        if (!(instance instanceof InstaEclipsePlugin)) throw new IllegalStateException("entrypoint is not an InstaEclipsePlugin");
        InstaEclipsePlugin plugin = (InstaEclipsePlugin) instance;
        if (!id.equals(plugin.getId()) || !version.equals(plugin.getVersion())) throw new SecurityException("plugin entrypoint identity mismatch");
        try { plugin.onLoad(new PluginContext(context, instagramClassLoader, instagramVersion, new PluginLog(id))); }
        catch (Throwable error) { throw new Exception("plugin onLoad failed", error); }
        LOADED.add(id); ModuleLog.line("(InstaEclipse | Plugin): loaded " + id + " v" + version);
    }

    private static void registerRestartReceiver(Context context) {
        if (restartReceiverRegistered) return;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                if (!ACTION_RESTART_INSTAGRAM.equals(intent.getAction())) return;
                ModuleLog.line("(InstaEclipse | Plugin): restart requested; stopping Instagram process");
                new Handler(Looper.getMainLooper()).postDelayed(() -> android.os.Process.killProcess(android.os.Process.myPid()), 100L);
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_RESTART_INSTAGRAM);
        try {
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, RESTART_PERMISSION, null, Context.RECEIVER_EXPORTED);
            else context.registerReceiver(receiver, filter, RESTART_PERMISSION, null);
            restartReceiverRegistered = true;
        } catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): restart receiver registration failed: " + error); }
    }

    private static boolean signerMatchesCore(Context context, PackageInfo plugin) {
        try {
            PackageInfo core = context.getPackageManager().getPackageArchiveInfo(Module.moduleSourceDir, PackageManager.GET_SIGNING_CERTIFICATES);
            return core != null && core.signingInfo != null && plugin.signingInfo != null && sameSigner(core.signingInfo, plugin.signingInfo);
        } catch (Throwable ignored) { return false; }
    }
    private static boolean sameSigner(SigningInfo a, SigningInfo b) throws Exception { Set<String> left = signatureDigests(a), right = signatureDigests(b); for (String digest : left) if (right.contains(digest)) return true; return false; }
    private static Set<String> signatureDigests(SigningInfo info) throws Exception { Signature[] signatures = info.hasMultipleSigners() ? info.getApkContentsSigners() : info.getSigningCertificateHistory(); Set<String> result = new HashSet<>(); if (signatures == null) return result; MessageDigest digest = MessageDigest.getInstance("SHA-256"); for (Signature signature : signatures) result.add(hex(digest.digest(signature.toByteArray()))); return result; }

    private static PluginManifest readManifest(String apkPath) throws Exception {
        try (ZipFile zip = new ZipFile(apkPath)) {
            ZipEntry entry = zip.getEntry("assets/plugin.json"); if (entry == null) throw new SecurityException("plugin manifest missing");
            try (InputStream input = zip.getInputStream(entry)) { PluginManifest manifest = GSON.fromJson(new String(readAll(input), StandardCharsets.UTF_8), PluginManifest.class); if (manifest == null) throw new SecurityException("plugin manifest invalid"); return manifest; }
        }
    }

    private static void validateManifest(PluginManifest manifest, String instagramVersion) {
        if (manifest.schema != 1) throw new IllegalStateException("unsupported plugin schema");
        if (manifest.id == null || !manifest.id.matches("[a-z0-9][a-z0-9._-]{1,63}")) throw new SecurityException("invalid plugin id");
        if (manifest.version == null || !manifest.version.matches("\\d+\\.\\d+\\.\\d+")) throw new SecurityException("invalid plugin version");
        if (manifest.entrypoint == null || manifest.entrypoint.length() > 200) throw new SecurityException("invalid entrypoint");
        if (manifest.min_core_api > CORE_API || manifest.max_core_api < CORE_API) throw new IllegalStateException("core API incompatible");
        long ig = parseInstagramVersion(instagramVersion);
        if (ig < manifest.min_instagram_version || ig > manifest.max_instagram_version) throw new IllegalStateException("Instagram version incompatible: " + instagramVersion);
    }

    /** Supports both Android versionCode strings and Instagram versionName such as 443.0.0.48.82. */
    private static long parseInstagramVersion(String value) {
        if (value == null || value.isEmpty()) return -1;
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) {}
        String[] parts = value.split("\\.");
        try {
            long major = Long.parseLong(parts[0]);
            long minor = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
            long patch = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
            return major * 1_000_000L + minor * 1_000L + patch;
        } catch (Throwable ignored) { return -1; }
    }

    private static byte[] readAll(InputStream input) throws Exception { java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); return output.toByteArray(); }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(bytes.length * 2); for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b)); return out.toString(); }
    private static void requestPendingTransfer(Context context) { try { context.sendBroadcast(new Intent(ACTION_REQUEST_PENDING).setPackage(CommonUtils.MY_PACKAGE_NAME)); } catch (Throwable ignored) {} }

    private static final class PluginManifest { int schema; String id; String name; String description; String version; int min_core_api; int max_core_api; String entrypoint; long min_instagram_version; long max_instagram_version; }
    private static final class PluginLog implements PluginLogger {
        private final String id; PluginLog(String id) { this.id = id; }
        @Override public void info(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): " + message); }
        @Override public void warn(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): WARN " + message); }
        @Override public void error(String message, Throwable throwable) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): ERROR " + message + " :: " + throwable); }
    }
}