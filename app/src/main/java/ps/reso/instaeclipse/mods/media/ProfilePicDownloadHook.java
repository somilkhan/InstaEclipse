package ps.reso.instaeclipse.mods.media;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.i18n.I18n;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Built-in profile-picture downloader for Instagram profile/avatar surfaces. */
public final class ProfilePicDownloadHook {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "InstaEclipse-ProfileDownload");
        t.setDaemon(true);
        return t;
    });
    private static final MapLike<View, Long> LAST_TRIGGER = new MapLike<>();
    private static volatile boolean installed;

    private ProfilePicDownloadHook() {}

    public static void install() {
        if (installed) return;
        installed = true;

        if (FeatureFlags.enableProfileDownload) {
            FeatureStatusTracker.setEnabled("ProfileDownload", R.string.ig_dialog_downloader_profiles);
            FeatureStatusTracker.setHooked("ProfileDownload");
        }

        XC_MethodHook hook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                handleLongClick((View) param.thisObject);
            }
        };

        // Hook only the canonical framework entry point. Android's coordinate-aware
        // performLongClick(float, float) delegates into this no-argument method, so
        // there is no reason to resolve a second, API-dependent overload here.
        // This avoids brittle exact-signature resolution across Android runtimes.
        XposedHelpers.findAndHookMethod(View.class, "performLongClick", hook);

        ModuleLog.line("(InstaEclipse | ProfileDownload): hook installed");
    }

    private static void handleLongClick(View view) {
        if (!FeatureFlags.enableProfileDownload || !(view instanceof ImageView)) return;
        if (!isProfilePictureView(view)) return;

        long now = android.os.SystemClock.uptimeMillis();
        Long previous = LAST_TRIGGER.get(view);
        if (previous != null && now - previous < 600L) return;
        LAST_TRIGGER.put(view, now);

        Context context = view.getContext();
        Activity activity = activityFromContext(context);
        String url = extractUrl(view);
        if (url == null) {
            ModuleLog.line("(IE|ProfileDL) URL extraction failed: " + view.getClass().getName());
            return;
        }

        String username = activity == null ? null : extractUsername(activity);
        String filename = FeedVideoDownloadHook.buildFilename(username, "profile", null, false);
        MAIN.post(() -> Toast.makeText(context,
                I18n.t(context, R.string.ig_toast_downloading_profile_pic), Toast.LENGTH_SHORT).show());

        IO.execute(() -> {
            try {
                boolean delegated = FeedVideoDownloadHook.downloadAndSave(context, url, filename, false, username);
                if (!delegated) {
                    MAIN.post(() -> Toast.makeText(context,
                            I18n.t(context, R.string.ig_toast_profile_pic_saved), Toast.LENGTH_SHORT).show());
                }
                ModuleLog.line("(IE|ProfileDL) ✓ saved profile picture");
            } catch (Throwable e) {
                ModuleLog.line("(IE|ProfileDL) ❌ download: " + e);
                MAIN.post(() -> Toast.makeText(context,
                        I18n.t(context, R.string.ig_toast_download_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static boolean isProfilePictureView(View view) {
        String resource = resourceName(view);
        String description = String.valueOf(view.getContentDescription());
        String type = view.getClass().getName();
        if (containsProfileMarker(resource) || containsProfileMarker(description) || containsProfileMarker(type)) return true;

        View parent = view;
        for (int i = 0; i < 5; i++) {
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
            if (parent == null) break;
            if (containsProfileMarker(resourceName(parent))
                    || containsProfileMarker(String.valueOf(parent.getContentDescription()))
                    || containsProfileMarker(parent.getClass().getName())) return true;
        }

        String url = extractUrl(view);
        return url != null && isInstagramImageUrl(url)
                && Math.abs(view.getWidth() - view.getHeight()) <= Math.max(8,
                view.getResources().getDisplayMetrics().density * 8);
    }

    private static boolean containsProfileMarker(String value) {
        if (value == null || value.isEmpty()) return false;
        String s = value.toLowerCase(java.util.Locale.US);
        return s.contains("profile") || s.contains("avatar") || s.contains("profile_pic")
                || s.contains("profilepicture") || s.contains("user_avatar") || s.contains("account_avatar");
    }

    private static String resourceName(View view) {
        try {
            int id = view.getId();
            if (id != View.NO_ID) return view.getResources().getResourceEntryName(id);
        } catch (Throwable ignored) {}
        return "";
    }

    private static String extractUrl(View view) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        String direct = inspectObject(view, visited, 0);
        if (direct != null) return direct;
        try {
            Object tag = view.getTag();
            if (tag instanceof Uri) return validUrl(tag.toString());
            if (tag instanceof String) return validUrl((String) tag);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String inspectObject(Object object, Set<Object> visited, int depth) {
        if (object == null || depth > 2 || visited.contains(object)) return null;
        visited.add(object);
        if (object instanceof String) return validUrl((String) object);
        if (object instanceof Uri) return validUrl(object.toString());

        Class<?> cls = object.getClass();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    String name = field.getName().toLowerCase(java.util.Locale.US);
                    if (!(name.contains("url") || name.contains("uri") || name.contains("image")
                            || name.equals("a0e") || name.equals("a0d") || name.equals("a0c"))) continue;
                    field.setAccessible(true);
                    String url = inspectValue(field.get(object), visited, depth);
                    if (url != null) return url;
                } catch (Throwable ignored) {}
            }
        }

        for (String methodName : new String[]{"getUrl", "getImageUrl", "getUri", "getImageUri"}) {
            try {
                Method method = cls.getMethod(methodName);
                if (method.getParameterTypes().length != 0) continue;
                String url = inspectValue(method.invoke(object), visited, depth);
                if (url != null) return url;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String inspectValue(Object value, Set<Object> visited, int depth) {
        if (value == null) return null;
        if (value instanceof String) return validUrl((String) value);
        if (value instanceof Uri) return validUrl(value.toString());
        return inspectObject(value, visited, depth + 1);
    }

    private static String validUrl(String candidate) {
        if (candidate == null) return null;
        String value = candidate.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) return null;
        return isInstagramImageUrl(value) ? value : null;
    }

    private static boolean isInstagramImageUrl(String value) {
        try {
            String host = new URL(value).getHost().toLowerCase(java.util.Locale.US);
            return isAllowedHost(host, "instagram.com")
                    || isAllowedHost(host, "cdninstagram.com")
                    || isAllowedHost(host, "fbcdn.net")
                    || isAllowedHost(host, "fbsbx.com");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAllowedHost(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static Activity activityFromContext(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static String extractUsername(Activity activity) {
        try {
            CharSequence title = activity.getTitle();
            if (title != null && looksLikeUsername(title.toString().trim())) return title.toString().trim();
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean looksLikeUsername(String value) {
        return value != null && value.length() <= 30
                && value.matches("[a-zA-Z0-9._]+") && !value.matches("\\d+");
    }

    private static final class MapLike<K, V> {
        private final WeakHashMap<K, V> map = new WeakHashMap<>();
        synchronized V get(K key) { return map.get(key); }
        synchronized void put(K key, V value) { map.put(key, value); }
    }
}
