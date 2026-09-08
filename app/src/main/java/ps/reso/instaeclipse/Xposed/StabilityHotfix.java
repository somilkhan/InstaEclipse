package ps.reso.instaeclipse.Xposed;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.mods.media.ReelDownloadHook;
import ps.reso.instaeclipse.mods.ui.theme.IgThemeEngine;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Narrow runtime compatibility hotfixes for Instagram 443.0.0.48.82. */
public final class StabilityHotfix implements IXposedHookLoadPackage {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static volatile Activity currentActivity;
    private static volatile Method reelInject;
    private static volatile Method reelGuard;

    @Override
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.instagram.android".equals(lpparam.packageName)) return;
        if (!INSTALLED.compareAndSet(false, true)) return;
        hookCurrentActivityTracking();
        hookThemeRefresh();
        hookReelMenu(lpparam.classLoader);
        ModuleLog.line("(IE|Stability) ✅ runtime hotfixes installed");
    }

    private static void hookCurrentActivityTracking() {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    Activity activity = (Activity) p.thisObject;
                    if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                    if ("com.instagram.android".equals(activity.getPackageName())) currentActivity = activity;
                }
            });
        } catch (Throwable t) {
            ModuleLog.line("(IE|Stability) ⚠️ Activity tracker unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void hookThemeRefresh() {
        try {
            XposedHelpers.findAndHookMethod(
                    "ps.reso.instaeclipse.mods.ui.theme.IgThemeHook",
                    StabilityHotfix.class.getClassLoader(),
                    "refreshCurrentActivity",
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            if (!FeatureFlags.customThemeEnabled) return;
                            Activity activity = currentActivity;
                            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                            final long started = android.os.SystemClock.uptimeMillis();
                            ModuleLog.line("(IE|Theme|APPLY) target=" + activity.getClass().getName());
                            new Handler(Looper.getMainLooper()).post(() -> {
                                try {
                                    IgThemeEngine.invalidate();
                                    activity.recreate();
                                    ModuleLog.line("(IE|Theme|RECREATE) requested elapsed=" +
                                            (android.os.SystemClock.uptimeMillis() - started) + "ms");
                                } catch (Throwable t) {
                                    ModuleLog.line("(IE|Theme|RECREATE) ❌ " + t.getClass().getSimpleName());
                                }
                            });
                            p.setResult(null);
                        }
                    });
        } catch (Throwable t) {
            ModuleLog.line("(IE|Theme) ⚠️ refresh bridge unavailable: " + t.getClass().getSimpleName());
        }
    }

    /** Inject before Instagram appends native rows; the existing ReelDownloadHook then de-dupes. */
    private static void hookReelMenu(ClassLoader cl) {
        try {
            Class<?> media = cl.loadClass("com.instagram.feed.media.Media");
            Class<?> a012c = cl.loadClass("X.012c");
            Class<?> a06qm = cl.loadClass("X.06QM");
            Class<?> builder = cl.loadClass("X.0QgR");
            XposedHelpers.findAndHookMethod("X.06TG", cl, "A0A",
                    a012c, a06qm, media, builder, boolean.class,
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            if (!FeatureFlags.enableReelDownload || p.args == null || p.args.length < 4) return;
                            Object menuBuilder = p.args[3];
                            Object reelMedia = p.args[2];
                            if (menuBuilder == null || reelMedia == null) return;
                            try {
                                installReelGuard(menuBuilder);
                                invokeReelInjection(p.thisObject, reelMedia, menuBuilder);
                            } catch (Throwable t) {
                                ModuleLog.line("(IE|Reel|HOTFIX) ❌ pre-build injection: " + t.getClass().getSimpleName());
                            }
                        }
                    });
            ModuleLog.line("(IE|Reel|HOTFIX) ✅ pre-build A0A hook installed");
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel|HOTFIX) ⚠️ A0A hook unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installReelGuard(Object builder) throws Exception {
        if (reelGuard == null) {
            reelGuard = ReelDownloadHook.class.getDeclaredMethod("installNativeDownloadBuilderGuard", Object.class);
            reelGuard.setAccessible(true);
        }
        reelGuard.invoke(null, builder);
    }

    private static void invokeReelInjection(Object controller, Object media, Object builder) throws Exception {
        if (reelInject == null) {
            reelInject = ReelDownloadHook.class.getDeclaredMethod("injectReelDownloadOption", Object.class, Object.class, Object.class);
            reelInject.setAccessible(true);
        }
        reelInject.invoke(null, controller, media, builder);
    }
}
