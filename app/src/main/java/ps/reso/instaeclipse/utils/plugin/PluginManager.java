package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
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

/** Runtime for signed, independently installed InstaEclipse plugin APKs. */
public final class PluginManager {
    public static final int CORE_API = 1;
    public static final String ACTION_INSTALL_PLUGIN = "ps.reso.instaeclipse.ACTION_INSTALL_PLUGIN";
    public static final String ACTION_REQUEST_PENDING = "ps.reso.instaeclipse.ACTION_REQUEST_PENDING_PLUGIN";
    public static final String ACTION_PLUGIN_INSTALLED = "ps.reso.instaeclipse.ACTION_PLUGIN_INSTALLED";
    public static final String EXTRA_URI = "plugin_uri";
    public static final String EXTRA_ID = "plugin_id";
    public static final String EXTRA_VERSION = "plugin_version";
    public static final String EXTRA_SHA256 = "plugin_sha256";
    public static final String META_PLUGIN = "ps.reso.instaeclipse.plugin";
    public static final String META_PLUGIN_ID = "ps.reso.instaeclipse.plugin.id";
    public static final String META_PLUGIN_VERSION = "ps.reso.instaeclipse.plugin.version";
    public static final String META_PLUGIN_ENTRYPOINT = "ps.reso.instaeclipse.plugin.entrypoint";
    private static final Gson GSON = new Gson();
    private static final Set<String> LOADED = new HashSet<>();
    private static volatile boolean bootstrapped;
    private PluginManager() {}

    public static synchronized void bootstrap(Context context, ClassLoader instagramClassLoader, String instagramVersion) {
        if (bootstrapped) return; bootstrapped = true;
        try {
            PackageManager pm = context.getPackageManager(); int flags = PackageManager.GET_META_DATA | PackageManager.GET_SIGNING_CERTIFICATES;
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

    public static boolean installFromUri(Context context, ClassLoader ignored, String ignoredInstagramVersion, Uri uri, String expectedId, String expectedVersion, String expectedSha256) {
        try { Intent intent = new Intent(context, PluginInstallActivity.class).setData(uri).putExtra(EXTRA_ID, expectedId).putExtra(EXTRA_VERSION, expectedVersion).putExtra(EXTRA_SHA256, expectedSha256 == null ? "" : expectedSha256).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION); context.startActivity(intent); return true; }
        catch (Throwable error) { ModuleLog.line("(InstaEclipse | Plugin): installer launch failed: " + error); return false; }
    }
    public static boolean isInstalled(Context context, String id) { return findPlugin(context, id) != null; }
    public static String getInstalledVersion(Context context, String id) { PackageInfo info = findPlugin(context, id); return info == null ? null : info.versionName; }
    public static boolean isEnabled(Context context, String id) { return isInstalled(context, id) && context.getSharedPreferences("instaeclipse_plugins", Context.MODE_PRIVATE).getBoolean("enabled_" + id, true); }
    public static void setEnabled(Context context, String id, boolean enabled) { context.getSharedPreferences("instaeclipse_plugins", Context.MODE_PRIVATE).edit().putBoolean("enabled_" + id, enabled).apply(); }
    public static void requestUninstall(Context context, String id) { PackageInfo info = findPlugin(context, id); if (info == null) return; context.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + info.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    /** Backward-compatible API; removal now goes through Android's user-authorized package uninstall flow. */
    public static void uninstall(Context context, String id) { requestUninstall(context, id); }

    private static PackageInfo findPlugin(Context context, String id) {
        try { int flags = PackageManager.GET_META_DATA | PackageManager.GET_SIGNING_CERTIFICATES; for (PackageInfo info : context.getPackageManager().getInstalledPackages(flags)) { ApplicationInfo app = info.applicationInfo; if (app != null && app.metaData != null && app.metaData.getBoolean(META_PLUGIN, false) && id.equals(app.metaData.getString(META_PLUGIN_ID))) return info; } } catch (Throwable ignored) {}
        return null;
    }

    private static void loadInstalled(Context context, ClassLoader instagramClassLoader, String instagramVersion, PackageInfo info) throws Exception {
        ApplicationInfo app = info.applicationInfo; String id = app.metaData.getString(META_PLUGIN_ID); String version = app.metaData.getString(META_PLUGIN_VERSION); String entrypoint = app.metaData.getString(META_PLUGIN_ENTRYPOINT);
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

    private static boolean signerMatchesCore(Context context, PackageInfo plugin) {
        try { PackageInfo core = context.getPackageManager().getPackageArchiveInfo(Module.moduleSourceDir, PackageManager.GET_SIGNING_CERTIFICATES); return core != null && core.signingInfo != null && plugin.signingInfo != null && sameSigner(core.signingInfo, plugin.signingInfo); } catch (Throwable ignored) { return false; }
    }
    private static boolean sameSigner(SigningInfo a, SigningInfo b) throws Exception { Set<String> left = signatureDigests(a), right = signatureDigests(b); for (String digest : left) if (right.contains(digest)) return true; return false; }
    private static Set<String> signatureDigests(SigningInfo info) throws Exception { Signature[] signatures = info.hasMultipleSigners() ? info.getApkContentsSigners() : info.getSigningCertificateHistory(); Set<String> result = new HashSet<>(); if (signatures == null) return result; MessageDigest digest = MessageDigest.getInstance("SHA-256"); for (Signature signature : signatures) result.add(hex(digest.digest(signature.toByteArray()))); return result; }

    private static PluginManifest readManifest(String apkPath) throws Exception { try (ZipFile zip = new ZipFile(apkPath)) { ZipEntry entry = zip.getEntry("assets/plugin.json"); if (entry == null) throw new SecurityException("plugin manifest missing"); try (InputStream input = zip.getInputStream(entry)) { PluginManifest manifest = GSON.fromJson(new String(readAll(input), StandardCharsets.UTF_8), PluginManifest.class); if (manifest == null) throw new SecurityException("plugin manifest invalid"); return manifest; } } }
    private static void validateManifest(PluginManifest manifest, String instagramVersion) { if (manifest.schema != 1) throw new IllegalStateException("unsupported plugin schema"); if (manifest.id == null || !manifest.id.matches("[a-z0-9][a-z0-9._-]{1,63}")) throw new SecurityException("invalid plugin id"); if (manifest.version == null || !manifest.version.matches("\\d+\\.\\d+\\.\\d+")) throw new SecurityException("invalid plugin version"); if (manifest.entrypoint == null || manifest.entrypoint.length() > 200) throw new SecurityException("invalid entrypoint"); if (manifest.min_core_api > CORE_API || manifest.max_core_api < CORE_API) throw new IllegalStateException("core API incompatible"); long ig = parseLong(instagramVersion); if (ig < manifest.min_instagram_version || ig > manifest.max_instagram_version) throw new IllegalStateException("Instagram version incompatible"); }
    private static byte[] readAll(InputStream input) throws Exception { java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); return output.toByteArray(); }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(bytes.length * 2); for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b)); return out.toString(); }
    private static long parseLong(String value) { try { return Long.parseLong(value); } catch (Throwable ignored) { return -1; } }
    private static void requestPendingTransfer(Context context) { try { Intent intent = new Intent(ACTION_REQUEST_PENDING).setPackage(CommonUtils.MY_PACKAGE_NAME); context.sendBroadcast(intent); } catch (Throwable ignored) {} }
    private static final class PluginManifest { int schema; String id; String name; String description; String version; int min_core_api; int max_core_api; String entrypoint; long min_instagram_version; long max_instagram_version; }
    private static final class PluginLog implements PluginLogger { private final String id; PluginLog(String id) { this.id = id; } @Override public void info(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): " + message); } @Override public void warn(String message) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): WARN " + message); } @Override public void error(String message, Throwable throwable) { ModuleLog.line("(InstaEclipse | Plugin/" + id + "): ERROR " + message + " :: " + throwable); } }
}
