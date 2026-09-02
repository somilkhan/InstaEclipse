package ps.reso.instaeclipse.plugins.pfp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.plugin.api.InstaEclipsePlugin;
import ps.reso.instaeclipse.plugin.api.PluginContext;

/** Independently delivered profile-picture downloader for Instagram. */
public final class PfpDownloaderPlugin implements InstaEclipsePlugin {
    private static final String ID = "pfp-downloader";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "InstaEclipse-PFP-Plugin");
        thread.setDaemon(true);
        return thread;
    });
    private static final String[] URL_FIELDS = {"A0E", "A0D", "A0c", "A0B", "A0f"};
    private static final String[] URL_METHODS = {"getUrl", "getUri", "getImageUrl", "getImageUri", "getSourceUrl"};
    private PluginContext context;

    @Override public String getId() { return ID; }
    @Override public String getVersion() { return BuildConfig.VERSION_NAME; }

    @Override public void onLoad(PluginContext context) {
        this.context = context;
        context.getLogger().info("PFP Downloader Pack loaded for Instagram " + context.getInstagramVersion());

        // Hook the actual long-click dispatch instead of replacing Instagram's listener.
        // This preserves Instagram's own long-click behavior whenever it already handles the event.
        XposedHelpers.findAndHookMethod(View.class, "performLongClick", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (Boolean.TRUE.equals(param.getResult())) return;
                View view = (View) param.thisObject;
                if (!(view instanceof ImageView) || !isProfilePicture(view)) return;
                download(view);
                param.setResult(true);
            }
        });
    }

    /** Instagram changes avatar resource names across releases; keep matching deliberately broad. */
    private static boolean isProfilePicture(View view) {
        try {
            int id = view.getId();
            if (id == View.NO_ID) return false;
            String name = view.getResources().getResourceEntryName(id).toLowerCase();
            return containsAny(name,
                    "profile_pic", "profile_picture", "profilephoto", "profile_photo",
                    "profile_image", "profileimage", "avatar", "user_pic", "user_photo",
                    "user_image", "account_pic", "account_image", "account_avatar",
                    "participant_photo", "participant_avatar", "author_avatar", "author_photo");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private void download(View view) {
        Context app = view.getContext().getApplicationContext();
        String url = extractUrl(view);
        if (url == null) {
            Toast.makeText(app, "Profile picture URL unavailable", Toast.LENGTH_SHORT).show();
            context.getLogger().warn("No image URL found for profile picture view: " + view.getClass().getName());
            return;
        }
        Toast.makeText(app, "Downloading profile picture…", Toast.LENGTH_SHORT).show();
        EXECUTOR.execute(() -> {
            try {
                saveImage(app, url);
                Toast.makeText(app, "Profile picture saved", Toast.LENGTH_SHORT).show();
                context.getLogger().info("PFP download complete");
            } catch (Throwable error) {
                context.getLogger().error("PFP download failed", error);
                Toast.makeText(app, "PFP download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String extractUrl(View view) {
        Object tag = view.getTag();
        if (tag != null) {
            String tagged = scanForUrl(tag, 4, Collections.newSetFromMap(new IdentityHashMap<>()));
            if (tagged != null) return tagged;
        }
        for (String fieldName : URL_FIELDS) {
            String url = readUrlField(view, fieldName);
            if (url != null) return url;
        }
        return scanForUrl(view, 4, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static String scanForUrl(Object object, int depth, Set<Object> visited) {
        if (object == null || depth < 0 || visited.contains(object)) return null;
        if (object instanceof String) return normalizeUrl((String) object);
        if (object instanceof Uri) return normalizeUrl(object.toString());
        if (object instanceof Drawable || object instanceof Number || object instanceof Boolean || object instanceof Character) return null;
        visited.add(object);

        String direct = invokeUrlMethods(object);
        if (direct != null) return direct;
        if (depth == 0) return null;

        Class<?> type = object.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                    field.setAccessible(true);
                    Object value = field.get(object);
                    String found = scanForUrl(value, depth - 1, visited);
                    if (found != null) return found;
                } catch (Throwable ignored) { }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String readUrlField(Object object, String fieldName) {
        Class<?> type = object.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return scanForUrl(field.get(object), 2, Collections.newSetFromMap(new IdentityHashMap<>()));
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static String invokeUrlMethods(Object object) {
        Class<?> type = object.getClass();
        for (String methodName : URL_METHODS) {
            try {
                Method method = type.getMethod(methodName);
                if (method.getParameterTypes().length != 0) continue;
                method.setAccessible(true);
                Object value = method.invoke(object);
                String url = value instanceof Uri ? value.toString() : value instanceof String ? (String) value : null;
                url = normalizeUrl(url);
                if (url != null) return url;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static String normalizeUrl(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return (trimmed.startsWith("https://") || trimmed.startsWith("http://")) ? trimmed : null;
    }

    private static void saveImage(Context context, String url) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IllegalStateException("PFP plugin requires Android 10+");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        connection.setRequestProperty("User-Agent", "InstaEclipse-PFP/" + BuildConfig.VERSION_NAME);
        try {
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) throw new IllegalStateException("HTTP " + response);

            String mime = connection.getContentType();
            if (mime == null || !mime.toLowerCase().startsWith("image/")) mime = "image/jpeg";
            String extension = extensionForMime(mime);

            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                    "InstaEclipse_PFP_" + System.currentTimeMillis() + extension);
            values.put(MediaStore.Images.Media.MIME_TYPE, mime.split(";", 2)[0].trim());
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/InstaEclipse");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri output = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (output == null) throw new IllegalStateException("MediaStore insert failed");

            boolean success = false;
            try (InputStream input = connection.getInputStream(); OutputStream stream = resolver.openOutputStream(output)) {
                if (stream == null) throw new IllegalStateException("MediaStore output unavailable");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) stream.write(buffer, 0, read);
                success = true;
            } finally {
                if (success) {
                    ContentValues publish = new ContentValues();
                    publish.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(output, publish, null, null);
                } else {
                    resolver.delete(output, null, null);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String extensionForMime(String mime) {
        if (mime == null) return ".jpg";
        String value = mime.toLowerCase();
        if (value.contains("png")) return ".png";
        if (value.contains("webp")) return ".webp";
        if (value.contains("avif")) return ".avif";
        return ".jpg";
    }
}
