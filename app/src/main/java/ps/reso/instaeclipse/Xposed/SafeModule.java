package ps.reso.instaeclipse.Xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import org.luckypray.dexkit.DexKitBridge;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ps.reso.instaeclipse.mods.ads.AdBlocker;
import ps.reso.instaeclipse.mods.ads.TrackingLinkDisable;
import ps.reso.instaeclipse.mods.devops.BuildExpiredPopupHook;
import ps.reso.instaeclipse.mods.devops.DevOptionsUnlockHook;
import ps.reso.instaeclipse.mods.feed.FeedPhotoZoomHook;
import ps.reso.instaeclipse.mods.feed.HideSuggestedFeedItemsHook;
import ps.reso.instaeclipse.mods.ghost.GhostChannelMarkAsReadHook;
import ps.reso.instaeclipse.mods.ghost.GhostDMMarkAsReadHook;
import ps.reso.instaeclipse.mods.ghost.GhostDMSeenHook;
import ps.reso.instaeclipse.mods.ghost.GhostEphemeralKeepHook;
import ps.reso.instaeclipse.mods.ghost.GhostPermanentViewHook;
import ps.reso.instaeclipse.mods.ghost.GhostReplayLimitHook;
import ps.reso.instaeclipse.mods.ghost.GhostScreenshotDetectionHook;
import ps.reso.instaeclipse.mods.ghost.GhostStorySeenHook;
import ps.reso.instaeclipse.mods.ghost.GhostTypingIndicatorHook;
import ps.reso.instaeclipse.mods.ghost.GhostViewOnceHook;
import ps.reso.instaeclipse.mods.ghost.ScreenshotPermissionHook;
import ps.reso.instaeclipse.mods.location.LocationSpoofHook;
import ps.reso.instaeclipse.mods.media.FeedVideoDownloadHook;
import ps.reso.instaeclipse.mods.media.ForceReelQualityHook;
import ps.reso.instaeclipse.mods.media.ModernVideoVersionCaptureHook;
import ps.reso.instaeclipse.mods.media.PostDownloadContextMenuHook;
import ps.reso.instaeclipse.mods.media.ProfilePicDownloadHook;
import ps.reso.instaeclipse.mods.media.ReelDownloadHook;
import ps.reso.instaeclipse.mods.media.StoryDownloadHook;
import ps.reso.instaeclipse.mods.media.StorySelfMenuCompatibilityHook;
import ps.reso.instaeclipse.mods.media.UsernameResolverPatch;
import ps.reso.instaeclipse.mods.misc.CaptionCopyContextMenuHook;
import ps.reso.instaeclipse.mods.misc.CommentCopyHook;
import ps.reso.instaeclipse.mods.misc.DisableDoubleTapLikeHook;
import ps.reso.instaeclipse.mods.misc.DisableStoryFlippingHook;
import ps.reso.instaeclipse.mods.misc.DisableVideoAutoPlayHook;
import ps.reso.instaeclipse.mods.misc.StoryMentionHook;
import ps.reso.instaeclipse.mods.network.IGNetworkInterceptor;
import ps.reso.instaeclipse.mods.ui.UIHookManager;
import ps.reso.instaeclipse.mods.ui.theme.IgThemeHook;
import ps.reso.instaeclipse.utils.compat.CompatibilityRuntime;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.core.SettingsManager;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureManager;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Startup-safe Xposed entrypoint; DexKit discovery never runs on Instagram's main thread. */
@SuppressLint("UnsafeDynamicallyLoadedCode")
public final class SafeModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final List<String> SUPPORTED_PACKAGES = CommonUtils.SUPPORTED_PACKAGES;
    private static final ExecutorService BOOTSTRAP_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "InstaEclipse-Bootstrap");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> BOOTSTRAP_STARTED = ConcurrentHashMap.newKeySet();

    private static String moduleSourceDir;
    private static String moduleLibDir;

    @Override
    public void initZygote(StartupParam startupParam) {
        moduleSourceDir = startupParam.modulePath;
        String abi = Build.SUPPORTED_ABIS.length == 0 ? "arm64" : Build.SUPPORTED_ABIS[0];
        String abiFolder;
        if ("arm64-v8a".equalsIgnoreCase(abi)) abiFolder = "arm64";
        else if ("armeabi-v7a".equalsIgnoreCase(abi) || "armeabi".equalsIgnoreCase(abi) || "armv8i".equalsIgnoreCase(abi)) abiFolder = "arm";
        else if ("x86".equalsIgnoreCase(abi)) abiFolder = "x86";
        else if ("x86_64".equalsIgnoreCase(abi)) abiFolder = "x86_64";
        else abiFolder = abi;
        int slash = moduleSourceDir.lastIndexOf('/');
        moduleLibDir = slash > 0 ? moduleSourceDir.substring(0, slash) + "/lib/" + abiFolder : null;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SUPPORTED_PACKAGES.contains(lpparam.packageName)) return;
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "attach", Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!BOOTSTRAP_STARTED.add(lpparam.packageName)) return;
                            bootstrapAsync((Context) param.args[0], lpparam);
                        }
                    });
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Startup): ❌ attach hook failed: " + t);
        }
    }

    private void bootstrapAsync(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final ClassLoader classLoader = lpparam.classLoader;
        final String apkPath = lpparam.appInfo.sourceDir;
        String igVersion = "unknown";

        SettingsManager.init(appContext);
        SettingsManager.loadAllFlags(appContext);
        try {
            android.content.pm.PackageInfo pi = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            igVersion = String.valueOf(pi.getLongVersionCode());
            DexKitCache.init(appContext, igVersion);
        } catch (Throwable t) {
            ModuleLog.line("(DexKitCache) ❌ init failed: " + t.getMessage());
        }
        CompatibilityRuntime.initialize(igVersion);
        try {
            Logging.init(appContext, "instaeclipse_module.log");
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Startup): logging init failed: " + t.getMessage());
        }
        restoreCompanionDownloadPath();

        final String detectedVersion = igVersion;
        BOOTSTRAP_EXECUTOR.execute(() -> {
            try {
                Module.hostClassLoader = classLoader;
                Module.moduleSourceDir = moduleSourceDir;
                if (moduleLibDir == null) throw new IllegalStateException("DexKit native library directory unavailable");
                System.load(moduleLibDir + "/libdexkit.so");
                DexKitBridge bridge = DexKitBridge.create(apkPath);
                Module.dexKitBridge = bridge;
                FeatureManager.refreshFeatureStatus();
                registerSyncReceiver(appContext);
                installAllFeatures(bridge, classLoader, lpparam);
                ModuleLog.line("(InstaEclipse | Startup): ✅ asynchronous bootstrap complete for IG " + detectedVersion);
            } catch (Throwable t) {
                ModuleLog.line("(InstaEclipse | Startup): ❌ bootstrap failed safely: " + t);
            }
        });
    }

    private void restoreCompanionDownloadPath() {
        try {
            XSharedPreferences cp = new XSharedPreferences(CommonUtils.MY_PACKAGE_NAME, "instaeclipse_cache");
            cp.reload();
            String path = cp.getString("downloaderCustomPath", "");
            String uri = cp.getString("downloaderCustomUri", "");
            if (!path.isEmpty()) FeatureFlags.downloaderCustomPath = path;
            if (!uri.isEmpty()) FeatureFlags.downloaderCustomUri = uri;
        } catch (Throwable ignored) {}
    }

    private void registerSyncReceiver(Context context) {
        try {
            Method m = Module.class.getDeclaredMethod("registerSyncReceiver", Context.class);
            m.setAccessible(true);
            m.invoke(new Module(), context);
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Sync): ❌ receiver registration failed: " + t.getMessage());
        }
    }

    private void installAllFeatures(DexKitBridge bridge, ClassLoader classLoader, XC_LoadPackage.LoadPackageParam lpparam) {
        run("DevOptions", () -> new DevOptionsUnlockHook().handleDevOptions(bridge));
        run("GhostSeen", () -> { new GhostDMSeenHook().handleSeenBlock(bridge); new GhostDMMarkAsReadHook(moduleSourceDir).install(classLoader); new GhostChannelMarkAsReadHook().install(classLoader); });
        run("GhostTyping", () -> new GhostTypingIndicatorHook().handleTypingBlock(bridge));
        run("GhostScreenshot", () -> new GhostScreenshotDetectionHook().handleScreenshotBlock(bridge));
        run("ScreenshotPermission", () -> new ScreenshotPermissionHook().install(classLoader));
        run("GhostViewOnce", () -> new GhostViewOnceHook().handleViewOnceBlock(bridge));
        run("UnlimitedReplays", () -> new GhostReplayLimitHook().install(bridge, classLoader));
        run("GhostStorySeen", () -> new GhostStorySeenHook().handleStorySeenBlock(bridge));
        run("HideSuggested", () -> new HideSuggestedFeedItemsHook().install(bridge, classLoader));
        run("AdBlocker", () -> new AdBlocker().disableSponsoredContent(bridge, classLoader));
        run("TrackingLinkDisable", () -> new TrackingLinkDisable().disableTrackingLinks(classLoader));
        run("StoryFlipping", () -> new DisableStoryFlippingHook().handleStoryFlippingDisable(bridge));
        run("StoryMentions", () -> new StoryMentionHook().install(bridge, classLoader));
        run("CopyComment", () -> new CommentCopyHook().install(bridge, classLoader));
        run("Caption", () -> new CaptionCopyContextMenuHook().install(bridge, classLoader));
        run("DoubleTapLike", () -> new DisableDoubleTapLikeHook().install(bridge, classLoader));
        run("PhotoZoom", () -> new FeedPhotoZoomHook().install(classLoader));
        run("SpoofLocation", () -> new LocationSpoofHook().install(classLoader));
        run("Theme", () -> new IgThemeHook().install(classLoader));
        run("ForceReelQuality", () -> new ForceReelQualityHook().install(bridge, classLoader));
        run("AutoPlayDisable", () -> new DisableVideoAutoPlayHook().handleAutoPlayDisable(bridge));
        run("BuildExpired", () -> new BuildExpiredPopupHook().install(bridge, classLoader));
        run("MediaDownload", () -> {
            new FeedVideoDownloadHook().install(classLoader);
            FeedVideoDownloadHook.installVideoUrlCaptureHook(bridge, classLoader);
            ModernVideoVersionCaptureHook.install(bridge, classLoader);
            UsernameResolverPatch.install(bridge, classLoader);
        });
        run("PostDownload", () -> new PostDownloadContextMenuHook().install(bridge, classLoader));
        run("EphemeralHook", () -> new GhostEphemeralKeepHook().install(bridge, classLoader));
        run("ViewOnceMedia", () -> new GhostPermanentViewHook().install(bridge, classLoader));
        run("StoryDownload", () -> {
            new StoryDownloadHook().install(bridge, classLoader);
            StorySelfMenuCompatibilityHook.install(bridge, classLoader);
        });
        run("ReelDownload", () -> new ReelDownloadHook().install(bridge, classLoader));
        run("ProfileDownload", ProfilePicDownloadHook::install);
        run("Interceptor", () -> new IGNetworkInterceptor().handleInterceptor(lpparam));
        run("MainActivityUI", () -> new UIHookManager().mainActivity(classLoader));
    }

    private interface FeatureInstall { void install() throws Throwable; }

    private static void run(String name, FeatureInstall install) {
        if (!CompatibilityRuntime.begin(name)) return;
        try {
            install.install();
            CompatibilityRuntime.installed(name, "feature-installer");
        } catch (Throwable t) {
            CompatibilityRuntime.unavailable(name, t.toString());
            ModuleLog.line("(InstaEclipse | " + name + "): ❌ isolated failure: " + t);
        }
    }
}
