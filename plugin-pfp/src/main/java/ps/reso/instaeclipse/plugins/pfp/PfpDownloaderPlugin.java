package ps.reso.instaeclipse.plugins.pfp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.plugin.api.InstaEclipsePlugin;
import ps.reso.instaeclipse.plugin.api.PluginContext;

/** First independently delivered InstaEclipse feature pack. */
public final class PfpDownloaderPlugin implements InstaEclipsePlugin {
    private static final String ID = "pfp-downloader";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> { Thread thread = new Thread(r, "InstaEclipse-PFP-Plugin"); thread.setDaemon(true); return thread; });
    private static final Map<View, Boolean> HOOKED = new WeakHashMap<>();
    private static volatile int expandedPicViewId;
    private PluginContext context;

    @Override public String getId() { return ID; }
    @Override public String getVersion() { return BuildConfig.VERSION_NAME; }

    @Override public void onLoad(PluginContext context) {
        this.context = context;
        context.getLogger().info("PFP Downloader Pack loaded for Instagram " + context.getInstagramVersion());
        XposedHelpers.findAndHookMethod(View.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                if (!isExpandedProfilePicture(view) || isHooked(view)) return;
                markHooked(view);
                view.setOnLongClickListener(v -> { download(v); return true; });
            }
        });
    }

    private boolean isExpandedProfilePicture(View view) {
        int id = view.getId(); if (id == View.NO_ID) return false;
        if (expandedPicViewId != 0) return id == expandedPicViewId;
        try { if (!"expanded_profile_pic".equals(view.getResources().getResourceEntryName(id))) return false; expandedPicViewId = id; return true; } catch (Throwable ignored) { return false; }
    }
    private static synchronized boolean isHooked(View view) { return HOOKED.containsKey(view); }
    private static synchronized void markHooked(View view) { HOOKED.put(view, Boolean.TRUE); }

    private void download(View view) {
        Context app = view.getContext().getApplicationContext(); String url = extractUrl(view);
        if (url == null) { Toast.makeText(app, "PFP URL unavailable", Toast.LENGTH_SHORT).show(); context.getLogger().warn("expanded_profile_pic did not expose an ImageUrl"); return; }
        Toast.makeText(app, "Downloading profile picture…", Toast.LENGTH_SHORT).show();
        EXECUTOR.execute(() -> { try { saveImage(app, url); Toast.makeText(app, "Profile picture saved", Toast.LENGTH_SHORT).show(); context.getLogger().info("PFP download complete"); } catch (Throwable error) { context.getLogger().error("PFP download failed", error); Toast.makeText(app, "PFP download failed", Toast.LENGTH_SHORT).show(); } });
    }

    private static String extractUrl(View view) {
        for (String fieldName : new String[]{"A0E", "A0D", "A0c"}) { String url = readImageUrlField(view, fieldName); if (url != null) return url; }
        try { Object tag = view.getTag(); if (tag instanceof Uri) return tag.toString(); if (tag instanceof String && ((String) tag).startsWith("http")) return (String) tag; } catch (Throwable ignored) {}
        return null;
    }
    private static String readImageUrlField(View view, String fieldName) {
        Class<?> type = view.getClass();
        while (type != null && type != Object.class) {
            try { Field field = type.getDeclaredField(fieldName); field.setAccessible(true); Object imageUrl = field.get(view); if (imageUrl == null) return null; Object result = imageUrl.getClass().getMethod("getUrl").invoke(imageUrl); return result instanceof String && ((String) result).startsWith("http") ? (String) result : null; }
            catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
            catch (Throwable ignored) { return null; }
        }
        return null;
    }

    private static void saveImage(Context context, String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); connection.setConnectTimeout(10000); connection.setReadTimeout(20000); connection.setInstanceFollowRedirects(true); connection.setRequestProperty("User-Agent", "InstaEclipse-PFP/1.0");
        try {
            int response = connection.getResponseCode(); if (response < 200 || response >= 300) throw new IllegalStateException("HTTP " + response);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throw new IllegalStateException("PFP plugin requires Android 10+");
            ContentResolver resolver = context.getContentResolver(); ContentValues values = new ContentValues(); values.put(MediaStore.Images.Media.DISPLAY_NAME, "InstaEclipse_PFP_" + System.currentTimeMillis() + ".jpg"); values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/InstaEclipse"); values.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri output = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values); if (output == null) throw new IllegalStateException("MediaStore insert failed"); boolean success = false;
            try (InputStream input = connection.getInputStream(); OutputStream stream = resolver.openOutputStream(output)) { if (stream == null) throw new IllegalStateException("MediaStore output unavailable"); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) stream.write(buffer, 0, read); success = true; }
            finally { if (success) { ContentValues publish = new ContentValues(); publish.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(output, publish, null, null); } else resolver.delete(output, null, null); }
        } finally { connection.disconnect(); }
    }
}
