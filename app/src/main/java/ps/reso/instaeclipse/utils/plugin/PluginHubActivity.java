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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Live Feature Hub. All feature metadata comes from the remote signed-plugin catalog. */
public final class PluginHubActivity extends AppCompatActivity {
    private LinearLayout content;
    private PluginCatalogManager.Catalog catalog;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Feature Hub");
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(32));
        scroll.addView(content);
        setContentView(scroll);
        loadCatalog(false);
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) loadCatalog(true);
    }

    private void loadCatalog(boolean refresh) {
        showLoading(refresh ? "Refreshing Feature Hub…" : "Loading Feature Hub…");
        new Thread(() -> {
            try {
                PluginCatalogManager.Catalog result = PluginCatalogManager.load(this, refresh);
                runOnUiThread(() -> { catalog = result; render(); });
            } catch (Throwable error) {
                runOnUiThread(() -> showError(error.getMessage()));
            }
        }, "InstaEclipse-FeatureHub").start();
    }

    private void render() {
        content.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Feature Hub");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = new Button(this);
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> loadCatalog(true));
        header.addView(refresh);
        content.addView(header);

        TextView status = new TextView(this);
        status.setText("Live catalog · v" + catalog.catalogVersion);
        status.setTextSize(12);
        status.setTextColor(0xFF8F8F8F);
        status.setPadding(0, dp(4), 0, dp(8));
        content.addView(status);

        boolean any = false;
        boolean anyInstalled = false;
        if (catalog.plugins != null) for (PluginCatalogManager.PluginEntry p : catalog.plugins) {
            if (p == null || !p.remote) continue;
            any = true;
            if (PluginManager.isInstalled(this, p.id)) anyInstalled = true;
            addPlugin(p);
        }

        if (!any) {
            TextView e = new TextView(this);
            e.setText("No downloadable plugin packs are published yet.");
            e.setTextSize(15);
            e.setTextColor(0xFFB7B7B7);
            content.addView(e);
        }

        if (anyInstalled) addRestartPanel();
    }

    private void addRestartPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF171717);
        bg.setCornerRadius(dp(18));
        panel.setBackground(bg);

        TextView message = new TextView(this);
        message.setText("Plugin changes activate after an Instagram restart.");
        message.setTextSize(14);
        message.setTextColor(0xFFB7B7B7);
        message.setPadding(0, 0, 0, dp(10));
        panel.addView(message);

        Button restart = new Button(this);
        restart.setText("Restart Instagram");
        restart.setOnClickListener(v -> {
            if (PluginManager.restartInstagram(this)) {
                Toast.makeText(this, "Restarting Instagram…", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Instagram is not available to restart.", Toast.LENGTH_LONG).show();
            }
        });
        panel.addView(restart);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(12);
        content.addView(panel, lp);
    }

    private void addPlugin(PluginCatalogManager.PluginEntry p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF171717);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        TextView title = new TextView(this);
        title.setText((p.name == null ? p.id : p.name) + "  ·  v" + p.version);
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);

        TextView desc = new TextView(this);
        desc.setText(p.description == null ? "" : p.description);
        desc.setTextSize(14);
        desc.setTextColor(0xFFB7B7B7);
        desc.setPadding(0, dp(6), 0, dp(12));

        TextView compat = new TextView(this);
        compat.setText("Core API " + p.minCoreApi + "–" + p.maxCoreApi + "  ·  Instagram "
                + safe(p.minInstagramVersion) + "–" + safe(p.maxInstagramVersion));
        compat.setTextSize(12);
        compat.setTextColor(0xFF8F8F8F);

        String installedVersion = PluginManager.getInstalledVersion(this, p.id);
        boolean installed = installedVersion != null;
        boolean enabled = installed && PluginManager.isEnabled(this, p.id);
        boolean update = installed && compareVersion(p.version, installedVersion) > 0;

        Button action = new Button(this);
        if (!installed) {
            action.setText("Download");
            action.setOnClickListener(v -> download(p));
        } else if (update) {
            action.setText("Update to v" + p.version);
            action.setOnClickListener(v -> download(p));
        } else {
            action.setText(enabled ? "Installed · Disable" : "Disabled · Enable");
            action.setOnClickListener(v -> {
                PluginManager.setEnabled(this, p.id, !enabled);
                Toast.makeText(this,
                        !enabled ? "Enabled — restart Instagram to activate."
                                : "Disabled — restart Instagram to deactivate.",
                        Toast.LENGTH_LONG).show();
                render();
            });
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(action);

        if (installed) {
            TextView current = new TextView(this);
            current.setText("Installed v" + installedVersion + (update
                    ? "  ·  Update available" : "  ·  Up to date"));
            current.setTextSize(12);
            current.setTextColor(update ? 0xFFFFC107 : 0xFF8F8F8F);
            current.setPadding(0, dp(8), 0, 0);
            card.addView(current);

            Button remove = new Button(this);
            remove.setText("Remove");
            remove.setOnClickListener(v -> {
                PluginManager.requestUninstall(this, p.id);
                Toast.makeText(this, "Confirm the Android uninstall prompt, then refresh.", Toast.LENGTH_LONG).show();
            });
            actions.addView(remove);
        }

        if (p.releaseNotes != null && !p.releaseNotes.isEmpty() && update) {
            TextView notes = new TextView(this);
            notes.setText("What's new: " + p.releaseNotes);
            notes.setTextSize(13);
            notes.setTextColor(0xFFB7B7B7);
            notes.setPadding(0, dp(8), 0, dp(4));
            card.addView(notes);
        }

        card.addView(title);
        card.addView(desc);
        card.addView(compat);
        card.addView(actions);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        content.addView(card, lp);
    }

    private void download(PluginCatalogManager.PluginEntry p) {
        if (p.downloadUrl == null || p.downloadUrl.isEmpty()
                || p.sha256Url == null || p.sha256Url.isEmpty()) {
            Toast.makeText(this, "This pack is not publish-ready yet.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Downloading " + (p.name == null ? p.id : p.name)
                + " v" + p.version + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String sidecar = new String(readUrl(p.sha256Url), StandardCharsets.UTF_8);
                String fileName = p.downloadUrl.substring(p.downloadUrl.lastIndexOf('/') + 1);
                String sha = findSha(sidecar, fileName);
                if (sha.isEmpty()) throw new SecurityException(
                        "Published SHA-256 not found for " + fileName);
                PluginDownloadManager.downloadAndQueue(this, p.id, p.version,
                        p.downloadUrl, sha, p.maxInstagramVersion);
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "InstaEclipse-PluginDownload").start();
    }

    private void showLoading(String text) {
        content.removeAllViews();
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(24), 0, dp(24));
        content.addView(t);
    }

    private void showError(String message) {
        content.removeAllViews();
        TextView t = new TextView(this);
        t.setText("Feature Hub unavailable\n" + (message == null ? "Unknown error" : message));
        t.setTextSize(16);
        t.setTextColor(Color.WHITE);
        content.addView(t);
        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(v -> loadCatalog(true));
        content.addView(retry);
    }

    private static String findSha(String sidecar, String fileName) {
        for (String line : sidecar.split("\\R")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2 && parts[1].equals(fileName)) return parts[0];
        }
        return "";
    }

    private static String safe(String value) { return value == null ? "?" : value; }

    private static int compareVersion(String left, String right) {
        String[] a = left.split("\\."), b = right.split("\\.");
        for (int i = 0; i < 3; i++) {
            int x = i < a.length ? parse(a[i]) : 0;
            int y = i < b.length ? parse(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int parse(String value) {
        try { return Integer.parseInt(value); } catch (Throwable ignored) { return 0; }
    }

    private byte[] readUrl(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "InstaEclipse-FeatureHub/1.1");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            return readAll(c.getInputStream());
        } finally { c.disconnect(); }
    }

    private static byte[] readAll(InputStream i) throws Exception {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = i.read(b)) != -1) o.write(b, 0, n);
        return o.toByteArray();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
