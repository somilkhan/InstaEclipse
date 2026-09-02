package ps.reso.instaeclipse.utils.plugin;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import ps.reso.instaeclipse.utils.log.Logging;

/** Validates a plugin APK, then hands installation to Android's package installer. */
public final class PluginInstallActivity extends Activity {
    private static final int INSTALL_REQUEST = 7311; private File stagedApk; private String expectedId; private String expectedVersion;
    @Override protected void onCreate(Bundle state) { super.onCreate(state); Logging.init(this, "instaeclipse_companion.log"); Uri uri = getIntent().getData(); expectedId = getIntent().getStringExtra(PluginManager.EXTRA_ID); expectedVersion = getIntent().getStringExtra(PluginManager.EXTRA_VERSION); String sha = getIntent().getStringExtra(PluginManager.EXTRA_SHA256); if (uri == null) { fail("Plugin package URI missing"); return; } new Thread(() -> prepare(uri, sha == null ? "" : sha)).start(); }
    private void prepare(Uri uri, String expectedSha) { try {
        File dir = new File(getFilesDir(), "plugin-pending"); if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("plugin staging unavailable"); stagedApk = new File(dir, "install-" + System.nanoTime() + ".apk");
        try (InputStream input = getContentResolver().openInputStream(uri); FileOutputStream output = new FileOutputStream(stagedApk)) { if (input == null) throw new IllegalStateException("plugin source unavailable"); byte[] buffer = new byte[32 * 1024]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); }
        if (!expectedSha.isEmpty() && !expectedSha.equalsIgnoreCase(sha256(stagedApk))) throw new SecurityException("SHA-256 mismatch"); Manifest manifest = readManifest(stagedApk);
        if (expectedId != null && !expectedId.equals(manifest.id)) throw new SecurityException("plugin id mismatch"); if (expectedVersion != null && !expectedVersion.equals(manifest.version)) throw new SecurityException("plugin version mismatch");
        PackageInfo candidate = getPackageManager().getPackageArchiveInfo(stagedApk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES); PackageInfo core = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        if (candidate == null || core == null || candidate.signingInfo == null || core.signingInfo == null || !sameSigner(core, candidate)) throw new SecurityException("plugin signing certificate does not match InstaEclipse");
        stagedApk.setReadOnly(); Uri installUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", stagedApk); runOnUiThread(() -> launchInstaller(installUri));
    } catch (Throwable error) { runOnUiThread(() -> fail(error.getMessage() == null ? "Plugin validation failed" : error.getMessage())); } }
    private void launchInstaller(Uri uri) { try { Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT, true); startActivityForResult(install, INSTALL_REQUEST); } catch (Throwable error) { fail("Android package installer unavailable"); } }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) { super.onActivityResult(requestCode, resultCode, data); if (requestCode != INSTALL_REQUEST) return; Toast.makeText(this, resultCode == RESULT_OK ? "Plugin installed. Restart Instagram to activate it." : "Plugin installation cancelled.", Toast.LENGTH_LONG).show(); finish(); }
    private void fail(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); finish(); }
    private static boolean sameSigner(PackageInfo a, PackageInfo b) throws Exception { Set<String> left = digests(a), right = digests(b); for (String digest : left) if (right.contains(digest)) return true; return false; }
    private static Set<String> digests(PackageInfo info) throws Exception { android.content.pm.SigningInfo signing = info.signingInfo; android.content.pm.Signature[] signatures = signing.hasMultipleSigners() ? signing.getApkContentsSigners() : signing.getSigningCertificateHistory(); Set<String> result = new HashSet<>(); MessageDigest md = MessageDigest.getInstance("SHA-256"); if (signatures != null) for (android.content.pm.Signature s : signatures) result.add(hex(md.digest(s.toByteArray()))); return result; }
    private static String sha256(File file) throws Exception { MessageDigest md = MessageDigest.getInstance("SHA-256"); try (InputStream input = new java.io.FileInputStream(file)) { byte[] buffer = new byte[32 * 1024]; int read; while ((read = input.read(buffer)) != -1) md.update(buffer, 0, read); } return hex(md.digest()); }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(bytes.length * 2); for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b)); return out.toString(); }
    private static Manifest readManifest(File apk) throws Exception { try (ZipFile zip = new ZipFile(apk)) { ZipEntry entry = zip.getEntry("assets/plugin.json"); if (entry == null) throw new SecurityException("plugin manifest missing"); try (InputStream input = zip.getInputStream(entry)) { Manifest result = new Gson().fromJson(new String(readAll(input), StandardCharsets.UTF_8), Manifest.class); if (result == null || result.schema != 1 || result.id == null || result.version == null) throw new SecurityException("plugin manifest invalid"); return result; } } }
    private static byte[] readAll(InputStream input) throws Exception { java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read); return output.toByteArray(); }
    private static final class Manifest { int schema; String id; String version; }
}
