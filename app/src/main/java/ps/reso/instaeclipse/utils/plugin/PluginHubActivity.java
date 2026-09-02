package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import ps.reso.instaeclipse.R;

/** Feature Hub: built-ins are informational; downloadable entries are real executable packs. */
public final class PluginHubActivity extends AppCompatActivity {
    private static final String CATALOG_URL = "https://raw.githubusercontent.com/somilkhan/InstaEclipse/stability/latest-instagram/plugins/catalog.json";
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Feature Hub");
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(32));
        scroll.addView(content);
        setContentView(scroll);
        showLoading();
        loadCatalog();
    }

    @Override protected void onResume() { super.onResume(); if (content != null) renderInstalledState(); }

    private void showLoading() {
        content.removeAllViews();
        TextView text = new TextView(this);
        text.setText("Loading plugin catalog…"); text.setTextSize(16); text.setPadding(0, dp(24), 0, dp(24));
        content.addView(text);
    }

    private void loadCatalog() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.0");
                if (c.getResponseCode() != 200) throw new IllegalStateException("Catalog HTTP " + c.getResponseCode());
                String json = new String(readAll(c.getInputStream()), java.nio.charset.StandardCharsets.UTF_8); c.disconnect();
                Catalog catalog = new Gson().fromJson(json, Catalog.class);
                runOnUiThread(() -> render(catalog));
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    content.removeAllViews();
                    TextView text = new TextView(this); text.setText("Feature Hub unavailable\n" + error.getMessage()); text.setTextSize(16);
                    content.addView(text);
                });
            }
        }).start();
    }

    private void render(Catalog catalog) {
        content.removeAllViews();
        addSection("BUILT-IN", "Core InstaEclipse features remain part of the installed Core APK and are not represented as plugins.");
        addSection("DOWNLOADABLE PLUGIN PACKS", "These are independently versioned executable APKs. Download → verify → Android install → restart Instagram.");
        if (catalog == null || catalog.plugins == null || catalog.plugins.isEmpty()) {
            TextView empty = new TextView(this); empty.setText("No downloadable packs published yet."); empty.setPadding(0, dp(16), 0, dp(16)); content.addView(empty); return;
        }
        for (PluginEntry plugin : catalog.plugins) addPlugin(plugin);
    }

    private void renderInstalledState() {
        // Installation state is read by each card when the catalog is rendered.
    }

    private void addPlugin(PluginEntry plugin) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(com.google.android.material.R.drawable.m3_tabs_rounded_background);
        TextView title = new TextView(this); title.setText(plugin.name + "  ·  v" + plugin.version); title.setTextSize(18); title.setTextColor(Color.WHITE);
        TextView description = new TextView(this); description.setText(plugin.description); description.setTextSize(14); description.setTextColor(0xFFB7B7B7); description.setPadding(0, dp(6), 0, dp(12));
        TextView compatibility = new TextView(this); compatibility.setText("Core API " + plugin.minCoreApi + "–" + plugin.maxCoreApi + "  ·  Instagram " + plugin.minInstagramVersion + "–" + plugin.maxInstagramVersion); compatibility.setTextSize(12); compatibility.setTextColor(0xFF8F8F8F);
        Button action = new Button(this);
        boolean installed = PluginManager.isInstalled(this, plugin.id);
        boolean enabled = PluginManager.isEnabled(this, plugin.id);
        if (!installed) { action.setText("Download"); action.setOnClickListener(v -> download(plugin)); }
        else { action.setText(enabled ? "Installed · Disable" : "Disabled · Enable"); action.setOnClickListener(v -> { PluginManager.setEnabled(this, plugin.id, !enabled); Toast.makeText(this, !enabled ? "Enabled — restart Instagram to activate." : "Disabled — restart Instagram to deactivate.", Toast.LENGTH_LONG).show(); loadCatalog(); }); }
        Button remove = new Button(this); remove.setText("Remove"); remove.setOnClickListener(v -> PluginManager.requestUninstall(this, plugin.id));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.END); actions.addView(action); if (installed) actions.addView(remove);
        card.addView(title); card.addView(description); card.addView(compatibility); card.addView(actions);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(12); content.addView(card, p);
    }

    private void download(PluginEntry plugin) {
        if (plugin.downloadUrl == null || plugin.downloadUrl.isEmpty() || plugin.sha256Url == null || plugin.sha256Url.isEmpty()) {
            Toast.makeText(this, "This pack is not publish-ready yet.", Toast.LENGTH_LONG).show(); return;
        }
        Toast.makeText(this, "Downloading " + plugin.name + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String sha = new String(readUrl(plugin.sha256Url), java.nio.charset.StandardCharsets.UTF_8).trim();
                PluginDownloadManager.downloadAndQueue(this, plugin.id, plugin.version, plugin.downloadUrl, sha, "0");
            } catch (Throwable error) { runOnUiThread(() -> Toast.makeText(this, "Download failed: " + error.getMessage(), Toast.LENGTH_LONG).show()); }
        }).start();
    }

    private void addSection(String title, String subtitle) {
        TextView t = new TextView(this); t.setText(title); t.setTextSize(12); t.setTextColor(0xFF8F8F8F); t.setPadding(0, dp(16), 0, dp(4)); content.addView(t);
        TextView s = new TextView(this); s.setText(subtitle); s.setTextSize(14); s.setTextColor(0xFFB7B7B7); s.setPadding(0, 0, 0, dp(12)); content.addView(s);
    }

    private byte[] readUrl(String url) throws Exception { HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.0"); try { if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode()); return readAll(c.getInputStream()); } finally { c.disconnect(); } }
    private static byte[] readAll(InputStream input) throws Exception { java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] b = new byte[8192]; int n; while ((n = input.read(b)) != -1) out.write(b, 0, n); return out.toByteArray(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static final class Catalog { List<PluginEntry> plugins; }
    private static final class PluginEntry {
        String id; String name; String description; String version;
        @SerializedName("min_core_api") int minCoreApi;
        @SerializedName("max_core_api") int maxCoreApi;
        @SerializedName("min_instagram_version") String minInstagramVersion;
        @SerializedName("max_instagram_version") String maxInstagramVersion;
        @SerializedName("download_url") String downloadUrl;
        @SerializedName("sha256_url") String sha256Url;
    }
}
