package ps.reso.instaeclipse.mods.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.i18n.I18n;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class ReelDownloadHook {
    private static Class<?> controllerClass;
    private static Method hookMethod;
    private static Method builderAddMethod;
    private static Field activityField;
    private static Field cachedOuterField;
    private static Field cachedInnerField;
    private static final AtomicBoolean OPTIONS_PATCH_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean CONTROLLER_HOOK_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean BUILDER_GUARD_INSTALLED = new AtomicBoolean(false);
    private static final Map<Object, Boolean> INJECTED_BUILDERS = new WeakHashMap<>();

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        installRemoveNativeDownloadOption(bridge, classLoader);
        installReelOptionsController(bridge, classLoader);
    }

    /**
     * Instagram 443 still has a reduced-options ArrayList path. Keep this as a
     * compatibility layer so the native DOWNLOAD enum cannot produce a second
     * download action alongside our single Reel Download entry.
     */
    private static void installRemoveNativeDownloadOption(DexKitBridge bridge, ClassLoader classLoader) {
        if (!OPTIONS_PATCH_INSTALLED.compareAndSet(false, true)) return;
        try {
            Class<?> optionClass = classLoader.loadClass("com.instagram.feed.media.mediaoption.MediaOption$Option");
            Object download = null;
            for (Object value : (Object[]) optionClass.getMethod("values").invoke(null)) {
                if ("DOWNLOAD".equals(value.toString())) {
                    download = value;
                    break;
                }
            }
            if (download == null) {
                OPTIONS_PATCH_INSTALLED.set(false);
                return;
            }
            final Object nativeDownload = download;
            XC_MethodHook removeHook = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!FeatureFlags.enableReelDownload) return;
                    try {
                        Object result = p.getResult();
                        if (result instanceof List<?>) {
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) result;
                            list.remove(nativeDownload);
                        }
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD removal failed: "
                                + t.getClass().getSimpleName());
                    }
                }
            };

            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod("ReelOptionsListBuilder", classLoader);
                if (cached != null) {
                    XposedBridge.hookMethod(cached, removeHook);
                    return;
                }
            }

            String optionDescriptor = "Lcom/instagram/feed/media/mediaoption/MediaOption$Option;";
            var methods = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create()
                            .returnType("java.util.ArrayList")
                            .addUsingField(optionDescriptor + "->PLAYBACK_CONTROLS:" + optionDescriptor)
                            .addUsingField(optionDescriptor + "->UNSAVE:" + optionDescriptor)));
            if (methods.isEmpty()) {
                OPTIONS_PATCH_INSTALLED.set(false);
                ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD option builder not found");
                return;
            }
            Method target = methods.get(0).getMethodInstance(classLoader);
            target.setAccessible(true);
            XposedBridge.hookMethod(target, removeHook);
            DexKitCache.saveMethod("ReelOptionsListBuilder", target);
            ModuleLog.line("(IE|Reel) ✅ native DOWNLOAD list suppression hooked");
        } catch (Throwable t) {
            OPTIONS_PATCH_INSTALLED.set(false);
            ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD suppression unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    /**
     * Resolve the actual 443 overflow-menu controller from the stable purge marker,
     * then select its real A0A(Media,...,0QgR,boolean) row-building method.
     *
     * The old implementation took the first DexKit result's declaring class. In 443
     * that result can be an unrelated helper (e.g. X.05BL), so the Reel hook never
     * ran when the visible "About this reel" sheet was built. The APK proves that
     * X.06TG is the actual ClipsOrganicMediaItemViewMoreOptionsController and that
     * A0A receives the Media plus the live X.0QgR menu builder.
     */
    private static void installReelOptionsController(DexKitBridge bridge, ClassLoader classLoader) {
        if (!CONTROLLER_HOOK_INSTALLED.compareAndSet(false, true)) return;
        try {
            Method target = null;
            Class<?> resolvedController = null;
            var methods = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().usingStrings("android_purge_26_q3_ClipsOrganicMediaItemViewMoreOptionsController")));

            for (var result : methods) {
                Class<?> candidateClass = result.getMethodInstance(classLoader).getDeclaringClass();
                for (Method m : candidateClass.getDeclaredMethods()) {
                    Class<?>[] ps = m.getParameterTypes();
                    if (m.getReturnType() == void.class
                            && "A0A".equals(m.getName())
                            && ps.length == 5
                            && "com.instagram.feed.media.Media".equals(ps[2].getName())
                            && "X.0QgR".equals(ps[3].getName())
                            && ps[4] == boolean.class) {
                        resolvedController = candidateClass;
                        target = m;
                        break;
                    }
                }
                if (target != null) break;
            }

            if (target == null) {
                ModuleLog.line("(IE|Reel) ❌ 443 reel options builder A0A not found");
                CONTROLLER_HOOK_INSTALLED.set(false);
                return;
            }

            controllerClass = resolvedController;
            hookMethod = target;
            hookMethod.setAccessible(true);
            FeatureStatusTracker.setHooked("ReelDownload");
            DexKitCache.saveMethod("ReelDownload", hookMethod);

            XposedBridge.hookMethod(hookMethod, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!FeatureFlags.enableReelDownload || p.args == null || p.args.length < 4) return;
                    Object media = p.args[2];
                    Object builder = p.args[3];
                    injectReelDownloadOption(p.thisObject, media, builder);
                }
            });

            ModuleLog.line("(IE|Reel) ✅ hooked actual options builder: "
                    + controllerClass.getName() + "." + hookMethod.getName());
        } catch (Throwable t) {
            CONTROLLER_HOOK_INSTALLED.set(false);
            ModuleLog.line("(IE|Reel) ❌ controller hook install: " + t);
        }
    }

    private static void injectReelDownloadOption(Object controller, Object media, Object builder) {
        if (builder == null || media == null) return;
        synchronized (INJECTED_BUILDERS) {
            if (INJECTED_BUILDERS.containsKey(builder)) return;
            INJECTED_BUILDERS.put(builder, Boolean.TRUE);
        }
        try {
            Activity activity = resolveActivity(controller);
            if (activity == null) {
                ModuleLog.line("(IE|Reel) ⚠️ menu activity not resolved");
                return;
            }

            Method add = resolveBuilderAddMethod(builder.getClass());
            if (add == null) {
                ModuleLog.line("(IE|Reel) ❌ X.0QgR add-option method not found");
                return;
            }
            builderAddMethod = add;

            int icon = resolveDownloadIcon(activity);
            final Activity a = activity;
            final Object mc = media;
            final Object cc = controller;
            View.OnClickListener listener = v -> showReelDownloadChooser(a, mc, cc);

            add.invoke(builder, activity, listener, "Reel Download", icon);
            ModuleLog.line("(IE|Reel) ✅ injected Reel Download into X.0QgR builder");
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ Reel menu injection: " + t);
        }
    }

    private static Method resolveBuilderAddMethod(Class<?> builderClass) {
        if (builderAddMethod != null) return builderAddMethod;
        Class<?> owner = builderClass;
        while (owner != null && owner != Object.class) {
            for (Method m : owner.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getReturnType() == void.class
                        && ps.length == 4
                        && Context.class.isAssignableFrom(ps[0])
                        && View.OnClickListener.class.isAssignableFrom(ps[1])
                        && ps[2] == String.class
                        && ps[3] == int.class) {
                    m.setAccessible(true);
                    return m;
                }
            }
            owner = owner.getSuperclass();
        }
        return null;
    }

    /**
     * Defensive suppression at the real shared menu-builder boundary. X.0QgR.A01,
     * A02 and A03 all accept (Context, OnClickListener, String, int); Instagram's
     * native DOWNLOAD row therefore cannot be inserted after the reduced-list patch.
     */
    private static void installNativeDownloadBuilderGuard(Object builder) {
        if (builder == null || BUILDER_GUARD_INSTALLED.get()) return;
        Class<?> owner = builder.getClass();
        while (owner != null && owner != Object.class) {
            for (Method m : owner.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getReturnType() == void.class && ps.length == 4
                        && Context.class.isAssignableFrom(ps[0])
                        && View.OnClickListener.class.isAssignableFrom(ps[1])
                        && ps[2] == String.class && ps[3] == int.class) {
                    try {
                        m.setAccessible(true);
                        if (!BUILDER_GUARD_INSTALLED.compareAndSet(false, true)) return;
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                if (!FeatureFlags.enableReelDownload || p.args == null || p.args.length < 3) return;
                                if (!(p.args[2] instanceof String)) return;
                                String label = ((String) p.args[2]).trim().toLowerCase(java.util.Locale.ROOT);
                                if (isNativeDownloadLabel(label)) {
                                    ModuleLog.line("(IE|Reel) 🚫 native Download row suppressed");
                                    p.setResult(null);
                                }
                            }
                        });
                        ModuleLog.line("(IE|Reel) ✅ X.0QgR Download-row guard hooked");
                    } catch (Throwable t) {
                        BUILDER_GUARD_INSTALLED.set(false);
                        ModuleLog.line("(IE|Reel) ⚠️ X.0QgR guard unavailable: "
                                + t.getClass().getSimpleName());
                    }
                    return;
                }
            }
            owner = owner.getSuperclass();
        }
    }

    private static boolean isNativeDownloadLabel(String label) {
        if (label == null || label.isEmpty()) return false;
        String normalized = label.replace("_", " ").trim();
        return normalized.equals("download")
                || normalized.equals("download reel")
                || normalized.equals("download video");
    }

    private static Activity resolveActivity(Object controller) {
        if (controller == null) return null;
        try {
            if (activityField != null) {
                Object value = activityField.get(controller);
                if (value instanceof Activity) return (Activity) value;
            }
        } catch (Throwable ignored) {}
        Class<?> c = controller.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (!Activity.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object value = f.get(controller);
                    if (value instanceof Activity) {
                        activityField = f;
                        return (Activity) value;
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static int findReelCarouselIndex(Object controller) {
        if (controller == null) return 0;
        if (cachedOuterField != null && cachedInnerField != null) {
            try {
                Object holder = cachedOuterField.get(controller);
                if (holder != null) return cachedInnerField.getInt(holder);
            } catch (Throwable ignored) {}
        }
        int best = Integer.MAX_VALUE;
        Field bestOuter = null, bestInner = null;
        Class<?> c = controller.getClass();
        while (c != null && c != Object.class) {
            for (Field outer : c.getDeclaredFields()) {
                if (outer.getType().isPrimitive()) continue;
                String n = outer.getType().getName();
                if (n.startsWith("android.") || n.startsWith("java.") || n.startsWith("androidx.") || n.startsWith("kotlin.")) continue;
                try { outer.setAccessible(true); } catch (Throwable ignored) { continue; }
                Object nested;
                try { nested = outer.get(controller); } catch (Throwable ignored) { continue; }
                if (nested == null) continue;
                Field one = null;
                int count = 0;
                Class<?> nc = nested.getClass();
                while (nc != null && nc != Object.class) {
                    String nn = nc.getName();
                    if (nn.startsWith("android.") || nn.startsWith("java.") || nn.startsWith("androidx.") || nn.startsWith("kotlin.")) break;
                    for (Field f : nc.getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            count++;
                            one = f;
                            if (count > 1) break;
                        }
                    }
                    if (count > 1) break;
                    nc = nc.getSuperclass();
                }
                if (count == 1 && one != null) {
                    try {
                        one.setAccessible(true);
                        int idx = one.getInt(nested);
                        if (idx >= 0 && idx < 200 && idx < best) {
                            best = idx;
                            bestOuter = outer;
                            bestInner = one;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            c = c.getSuperclass();
        }
        if (bestOuter != null) {
            cachedOuterField = bestOuter;
            cachedInnerField = bestInner;
            return best;
        }
        return 0;
    }

    static int findCarouselIndexFromView(Context ctx, int size) {
        if (!(ctx instanceof Activity)) return -1;
        try {
            List<Integer> matches = new java.util.ArrayList<>();
            collectCarouselMatches(((Activity) ctx).getWindow().getDecorView(), size, matches);
            return matches.size() == 1 ? matches.get(0) : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int adapterCount(Object adapter) {
        try { return (int) adapter.getClass().getMethod("getItemCount").invoke(adapter); } catch (Throwable ignored) {}
        try { return (int) adapter.getClass().getMethod("getCount").invoke(adapter); } catch (Throwable ignored) {}
        return -1;
    }

    private static void collectCarouselMatches(View view, int size, List<Integer> out) {
        String cn = view.getClass().getName();
        if (cn.contains("ViewPager")) {
            try {
                Object adapter = view.getClass().getMethod("getAdapter").invoke(view);
                if (adapter != null && adapterCount(adapter) == size) {
                    for (String name : new String[]{"getCurrentItem", "getCurrentDataIndex", "getCurrentWrappedDataIndex", "getCurrentRawDataIndex"}) {
                        try {
                            int p = (int) view.getClass().getMethod(name).invoke(view);
                            if (p >= 0) { out.add(p); break; }
                        } catch (NoSuchMethodException ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (cn.contains("RecyclerView")) {
            try {
                Object adapter = view.getClass().getMethod("getAdapter").invoke(view);
                if (adapter != null && adapterCount(adapter) == size) {
                    Object lm = view.getClass().getMethod("getLayoutManager").invoke(view);
                    if (lm != null) {
                        try {
                            int orientation = (int) lm.getClass().getMethod("getOrientation").invoke(lm);
                            if (orientation != 0) lm = null;
                        } catch (Throwable ignored) {}
                        if (lm != null) {
                            Integer p = null;
                            try { p = (int) lm.getClass().getMethod("findFirstCompletelyVisibleItemPosition").invoke(lm); } catch (Throwable ignored) {}
                            if (p == null) try { p = (int) lm.getClass().getMethod("findFirstVisibleItemPosition").invoke(lm); } catch (Throwable ignored) {}
                            if (p != null && p >= 0) out.add(p);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectCarouselMatches(group.getChildAt(i), size, out);
        }
    }

    private static void showReelDownloadChooser(Activity activity, Object media, Object controller) {
        final String[] options = {"Download Video", "Download Image"};
        new AlertDialog.Builder(activity)
                .setTitle("Reel Download")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startReelVideoDownload(activity, media, controller);
                    else startReelImageDownload(activity, media, controller);
                })
                .show();
    }

    private static void startReelVideoDownload(Context ctx, Object media, Object controller) {
        String user = FeedVideoDownloadHook.extractUsernameFromMediaObject(media);
        if (user == null) user = "reel";
        String id = "0";
        try {
            Object x = media.getClass().getMethod("getId").invoke(media);
            if (x instanceof String && !((String) x).isEmpty()) id = (String) x;
        } catch (Throwable ignored) {}
        String video = FeedVideoDownloadHook.bestVideoUrlFromMedia(media);
        if (video != null) {
            final String fn = FeedVideoDownloadHook.buildFilename(user, "reel", id, true);
            final String u = video;
            final String usr = user;
            Toast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_downloading_reel), Toast.LENGTH_SHORT).show();
            FeedVideoDownloadHook.executor.submit(() -> {
                try {
                    boolean failed = FeedVideoDownloadHook.downloadAndSave(ctx, u, fn, true, usr);
                    if (!failed) FeedVideoDownloadHook.mainHandler.post(() ->
                            Toast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_saved), Toast.LENGTH_SHORT).show());
                } catch (Throwable e) {
                    FeedVideoDownloadHook.mainHandler.post(() ->
                            Toast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
                }
            });
            return;
        }
        List<String> urls = FeedVideoDownloadHook.extractAllUrlsFromMedia(ctx, media);
        if (urls.isEmpty()) {
            Toast.makeText(ctx, I18n.t(ctx, R.string.ig_toast_reel_url_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        int vi = findCarouselIndexFromView(ctx, urls.size());
        int idx = vi >= 0 ? vi : findReelCarouselIndex(controller);
        final String fu = user, fid = id;
        final int fi = idx;
        FeedVideoDownloadHook.mainHandler.post(() ->
                FeedVideoDownloadHook.showPostDownloadDialog(ctx, urls, fu, fid, fi));
    }

    private static void startReelImageDownload(Context ctx, Object media, Object controller) {
        try {
            String user = FeedVideoDownloadHook.extractUsernameFromMediaObject(media);
            if (user == null || user.isEmpty()) user = "reel";
            String id = "0";
            try {
                Object x = media.getClass().getMethod("getId").invoke(media);
                if (x instanceof String && !((String) x).isEmpty()) id = (String) x;
            } catch (Throwable ignored) {}
            String image = FeedVideoDownloadHook.imageUrlFromMedia(ctx, media);
            if (image == null) {
                List<String> urls = FeedVideoDownloadHook.extractAllUrlsFromMedia(ctx, media);
                for (String u : urls) if (isLikelyImageUrl(u)) { image = u; break; }
            }
            if (image == null) {
                Toast.makeText(ctx, "Reel image not available", Toast.LENGTH_SHORT).show();
                return;
            }
            final String url = image;
            final String fn = FeedVideoDownloadHook.buildFilename(user, "reel_image", id, false);
            final String usr = user;
            Toast.makeText(ctx, "Downloading reel image…", Toast.LENGTH_SHORT).show();
            FeedVideoDownloadHook.executor.submit(() -> {
                try {
                    boolean failed = FeedVideoDownloadHook.downloadAndSave(ctx, url, fn, false, usr);
                    if (!failed) FeedVideoDownloadHook.mainHandler.post(() ->
                            Toast.makeText(ctx, "Reel image saved", Toast.LENGTH_SHORT).show());
                } catch (Throwable e) {
                    FeedVideoDownloadHook.mainHandler.post(() ->
                            Toast.makeText(ctx, "Reel image failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) Reel image download failed: " + t);
        }
    }

    private static boolean isLikelyImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String l = url.toLowerCase(java.util.Locale.ROOT);
        return l.contains("dst-jpg") || l.contains("dst-png") || l.contains("dst-webp")
                || l.endsWith(".jpg") || l.contains(".jpg?")
                || l.endsWith(".jpeg") || l.contains(".jpeg?")
                || l.endsWith(".png") || l.contains(".png?")
                || l.endsWith(".webp") || l.contains(".webp?");
    }

    private static int resolveDownloadIcon(Context ctx) {
        try {
            Class<?> c = ctx.getClassLoader().loadClass("com.instagram.feed.media.mediaoption.MediaOption$Option");
            for (Object v : (Object[]) c.getMethod("values").invoke(null)) {
                if (v.toString().contains("DOWNLOAD")) {
                    Field f = v.getClass().getField("iconDrawable");
                    return (int) f.get(v);
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}