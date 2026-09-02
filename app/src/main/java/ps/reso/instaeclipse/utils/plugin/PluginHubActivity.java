package ps.reso.instaeclipse.utils.plugin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** Feature Hub: built-ins are informational; downloadable entries are real executable packs. */
public final class PluginHubActivity extends AppCompatActivity {
    private static final String CATALOG_URL = "https://raw.githubusercontent.com/somilkhan/InstaEclipse/stability/latest-instagram/plugins/catalog.json";
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Feature Hub");
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(16), dp(20), dp(32)); scroll.addView(content); setContentView(scroll); showLoading(); loadCatalog();
    }
    private void showLoading() { content.removeAllViews(); TextView text = new TextView(this); text.setText("Loading plugin catalog…"); text.setTextSize(16); text.setPadding(0, dp(24), 0, dp(24)); content.addView(text); }
    private void loadCatalog() { new Thread(() -> { try { Catalog c = new Gson().fromJson(new String(readUrl(CATALOG_URL), java.nio.charset.StandardCharsets.UTF_8), Catalog.class); runOnUiThread(() -> render(c)); } catch (Throwable e) { runOnUiThread(() -> { content.removeAllViews(); TextView t = new TextView(this); t.setText("Feature Hub unavailable\n" + e.getMessage()); t.setTextSize(16); content.addView(t); }); } }).start(); }

    private void render(Catalog catalog) {
        content.removeAllViews(); addSection("BUILT-IN", "Core InstaEclipse features remain part of the installed Core APK and are never mislabeled as plugins."); addSection("DOWNLOADABLE PLUGIN PACKS", "Independent executable APKs. Download → verify → Android install → restart Instagram.");
        if (catalog == null || catalog.plugins == null || catalog.plugins.isEmpty()) { TextView e = new TextView(this); e.setText("No downloadable packs published yet."); content.addView(e); return; }
        for (PluginEntry p : catalog.plugins) addPlugin(p);
    }

    private void addPlugin(PluginEntry p) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(16), dp(16), dp(16)); GradientDrawable bg = new GradientDrawable(); bg.setColor(0xFF171717); bg.setCornerRadius(dp(18)); card.setBackground(bg);
        TextView title = new TextView(this); title.setText(p.name + "  ·  v" + p.version); title.setTextSize(18); title.setTextColor(Color.WHITE);
        TextView desc = new TextView(this); desc.setText(p.description); desc.setTextSize(14); desc.setTextColor(0xFFB7B7B7); desc.setPadding(0, dp(6), 0, dp(12));
        TextView compat = new TextView(this); compat.setText("Core API " + p.minCoreApi + "–" + p.maxCoreApi + "  ·  Instagram " + p.minInstagramVersion + "–" + p.maxInstagramVersion); compat.setTextSize(12); compat.setTextColor(0xFF8F8F8F);
        boolean installed = PluginManager.isInstalled(this, p.id); boolean enabled = PluginManager.isEnabled(this, p.id); Button action = new Button(this);
        if (!installed) { action.setText("Download"); action.setOnClickListener(v -> download(p)); } else { action.setText(enabled ? "Installed · Disable" : "Disabled · Enable"); action.setOnClickListener(v -> { PluginManager.setEnabled(this, p.id, !enabled); Toast.makeText(this, !enabled ? "Enabled — restart Instagram to activate." : "Disabled — restart Instagram to deactivate.", Toast.LENGTH_LONG).show(); loadCatalog(); }); }
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.END); actions.addView(action);
        if (installed) { Button remove = new Button(this); remove.setText("Remove"); remove.setOnClickListener(v -> PluginManager.requestUninstall(this, p.id)); actions.addView(remove); }
        card.addView(title); card.addView(desc); card.addView(compat); card.addView(actions); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); content.addView(card, lp);
    }

    private void download(PluginEntry p) {
        if (p.downloadUrl == null || p.downloadUrl.isEmpty() || p.sha256Url == null || p.sha256Url.isEmpty()) { Toast.makeText(this, "This pack is not publish-ready yet.", Toast.LENGTH_LONG).show(); return; }
        Toast.makeText(this, "Downloading " + p.name + "…", Toast.LENGTH_SHORT).show(); new Thread(() -> { try { String sidecar = new String(readUrl(p.sha256Url), java.nio.charset.StandardCharsets.UTF_8); String fileName = p.downloadUrl.substring(p.downloadUrl.lastIndexOf('/') + 1); String sha = findSha(sidecar, fileName); if (sha.isEmpty()) throw new SecurityException("published SHA-256 not found for " + fileName); PluginDownloadManager.downloadAndQueue(this, p.id, p.version, p.downloadUrl, sha, "0"); } catch (Throwable e) { runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show()); } }).start();
    }
    private static String findSha(String sidecar, String fileName) { for (String line : sidecar.split("\\R")) { String[] parts = line.trim().split("\\s+"); if (parts.length >= 2 && parts[1].equals(fileName)) return parts[0]; } return ""; }
    private void addSection(String title, String subtitle) { TextView t = new TextView(this); t.setText(title); t.setTextSize(12); t.setTextColor(0xFF8F8F8F); t.setPadding(0, dp(16), 0, dp(4)); content.addView(t); TextView s = new TextView(this); s.setText(subtitle); s.setTextSize(14); s.setTextColor(0xFFB7B7B7); s.setPadding(0, 0, 0, dp(12)); content.addView(s); }
    private byte[] readUrl(String url) throws Exception { HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.0"); try { if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode()); return readAll(c.getInputStream()); } finally { c.disconnect(); } }
    private static byte[] readAll(InputStream i) throws Exception { java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] b = new byte[8192]; int n; while ((n = i.read(b)) != -1) o.write(b, 0, n); return o.toByteArray(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private static final class Catalog { List<PluginEntry> plugins; }
    private static final class PluginEntry { String id; String name; String description; String version; @SerializedName("min_core_api") int minCoreApi; @SerializedName("max_core_api") int maxCoreApi; @SerializedName("min_instagram_version") String minInstagramVersion; @SerializedName("max_instagram_version") String maxInstagramVersion; @SerializedName("download_url") String downloadUrl; @SerializedName("sha256_url") String sha256Url; }
}
