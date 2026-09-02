package ps.reso.instaeclipse.utils.feature;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ps.reso.instaeclipse.R;

/**
 * Safe feature catalog UI. The catalog only activates code already shipped in the core APK.
 * Remote executable code is deliberately not loaded.
 */
public final class FeatureHub {
    private static final String CATALOG_URL = "https://raw.githubusercontent.com/somilkhan/InstaEclipse/main/plugins.json";
    private static final String PREFS = "instaeclipse_cache";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "InstaEclipse-FeatureCatalog");
        t.setDaemon(true);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private FeatureHub() {}

    public static void show(Fragment fragment) {
        Context context = fragment.requireContext();
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout root = createRoot(context);

        TextView title = text(context, "Feature Hub", 22, true);
        TextView subtitle = text(context, "Install or update feature packs without reinstalling the core app.", 14, false);
        subtitle.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        root.addView(title);
        root.addView(subtitle, marginParams(0, 4, 0, 16));

        TextView status = text(context, "Checking catalog…", 13, false);
        root.addView(status, marginParams(0, 0, 0, 10));

        dialog.setContentView(root);
        dialog.show();
        loadCatalog(context, root, status, dialog);
    }

    private static LinearLayout createRoot(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 28));
        return root;
    }

    private static void loadCatalog(Context context, LinearLayout root, TextView status, BottomSheetDialog dialog) {
        EXECUTOR.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "InstaEclipse/0.6");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);

                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                } finally {
                    connection.disconnect();
                }

                Catalog catalog = new Gson().fromJson(body.toString(), Catalog.class);
                if (catalog == null || catalog.plugins == null) throw new IllegalStateException("Invalid catalog");
                MAIN.post(() -> renderCatalog(context, root, status, catalog));
            } catch (Throwable error) {
                MAIN.post(() -> {
                    status.setText("Couldn't reach the update catalog");
                    MaterialButton retry = new MaterialButton(context);
                    retry.setText("Retry");
                    retry.setOnClickListener(v -> loadCatalog(context, root, status, dialog));
                    root.addView(retry, marginParams(0, 8, 0, 0));
                });
            }
        });
    }

    private static void renderCatalog(Context context, LinearLayout root, TextView status, Catalog catalog) {
        status.setText("Core " + (catalog.core != null ? catalog.core.latest_version : "current") + " • " + catalog.plugins.length + " feature packs");
        for (Plugin plugin : catalog.plugins) {
            root.addView(pluginCard(context, plugin), marginParams(0, 0, 0, 10));
        }
    }

    private static View pluginCard(Context context, Plugin plugin) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.dark_gray));
        card.setRadius(dp(context, 20));
        card.setCardElevation(0);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 16), dp(context, 14), dp(context, 12), dp(context, 14));

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(context, plugin.name, 16, true);
        TextView description = text(context, plugin.description, 13, false);
        description.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        TextView version = text(context, "v" + plugin.version + " • Built-in", 11, false);
        version.setTextColor(ContextCompat.getColor(context, R.color.accent_blue));
        copy.addView(name);
        copy.addView(description, marginParams(0, 3, 8, 0));
        copy.addView(version, marginParams(0, 5, 0, 0));

        MaterialButton action = new MaterialButton(context);
        action.setMinWidth(0);
        action.setText(isInstalled(context, plugin) ? "Installed" : "Install");
        action.setOnClickListener(v -> {
            boolean enable = !isInstalled(context, plugin);
            setPlugin(context, plugin, enable);
            action.setText(enable ? "Installed" : "Install");
        });

        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(action, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(row);
        return card;
    }

    private static boolean isInstalled(Context context, Plugin plugin) {
        if (plugin.pref_keys == null || plugin.pref_keys.length == 0) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String key : plugin.pref_keys) {
            if (!prefs.getBoolean(key, false)) return false;
        }
        return true;
    }

    private static void setPlugin(Context context, Plugin plugin, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (plugin.pref_keys != null) {
            for (String key : plugin.pref_keys) {
                editor.putBoolean(key, enabled);
                Intent intent = new Intent("ps.reso.instaeclipse.ACTION_UPDATE_PREF");
                intent.putExtra("key", key);
                intent.putExtra("value", enabled);
                context.sendBroadcast(intent);
            }
        }
        editor.commit();
        makeWorldReadable(context);
    }

    private static void makeWorldReadable(Context context) {
        try {
            java.io.File prefs = new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs/" + PREFS + ".xml");
            if (prefs.exists()) prefs.setReadable(true, false);
        } catch (Throwable ignored) {
        }
    }

    private static TextView text(Context context, String value, float sizeSp, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        return view;
    }

    private static LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Catalog {
        Core core;
        Plugin[] plugins;
    }

    private static final class Core {
        String latest_version;
        String update_url;
    }

    private static final class Plugin {
        String id;
        String name;
        String description;
        String version;
        String delivery;
        String release_url;
        String[] pref_keys;
    }
}
