package ps.reso.instaeclipse.mods.media;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Filters Instagram's already parsed reel video-version list to the closest requested height.
 *
 * The resolver is deliberately layered: the current Instagram build exposes the media getter
 * through Media.AAX and the VideoVersion model through VideoVersionIntf/ImmutablePandoVideoVersion.
 * Those names are used only as version-scoped compatibility fallbacks. DexKit semantic discovery
 * remains the primary future-build path.
 */
public final class ForceReelQualityHook {
    private static final String CACHE_VERSIONS = "ForceReelQuality_VideoVersionsGetter";
    private static final String CACHE_HEIGHT = "ForceReelQuality_HeightGetter";
    private static final String MEDIA_CLASS = "com.instagram.feed.media.Media";
    private static final String VERSION_INTERFACE = "com.instagram.api.schemas.VideoVersionIntf";
    private static final String VERSION_IMPL = "com.instagram.api.schemas.ImmutablePandoVideoVersion";

    public void install(DexKitBridge bridge, ClassLoader loader) {
        try {
            Method versions = loadCached(CACHE_VERSIONS, loader);
            Method height = loadCached(CACHE_HEIGHT, loader);

            if (!validVersionsGetter(versions)) versions = resolveVersions(bridge, loader);
            if (!validHeightGetter(height, loader)) height = resolveHeight(loader, bridge);

            if (!validVersionsGetter(versions) || !validHeightGetter(height, loader)) {
                ModuleLog.line("(InstaEclipse | ForceReelQuality): unavailable on this IG build"
                        + " versions=" + validVersionsGetter(versions)
                        + " height=" + validHeightGetter(height, loader));
                return;
            }

            versions.setAccessible(true);
            height.setAccessible(true);
            DexKitCache.saveMethod(CACHE_VERSIONS, versions);
            DexKitCache.saveMethod(CACHE_HEIGHT, height);

            final Method finalHeight = height;
            XposedBridge.hookMethod(versions, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (FeatureFlags.forceReelQuality <= 0) return;
                    try {
                        Object result = param.getResult();
                        if (!(result instanceof List<?> list) || list.size() <= 1) return;
                        Object selected = pickBest(list, FeatureFlags.forceReelQuality, finalHeight);
                        if (selected != null) param.setResult(Collections.singletonList(selected));
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | ForceReelQuality): runtime guard: " + t);
                    }
                }
            });

            FeatureStatusTracker.setHooked("ForceReelQuality");
            ModuleLog.line("(InstaEclipse | ForceReelQuality): hooked "
                    + versions.getDeclaringClass().getName() + "." + versions.getName()
                    + " / " + height.getDeclaringClass().getName() + "." + height.getName());
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ForceReelQuality): install failed safely: " + t);
        }
    }

    private static Method loadCached(String key, ClassLoader loader) {
        if (!DexKitCache.isCacheValid()) return null;
        try { return DexKitCache.loadMethod(key, loader); } catch (Throwable ignored) { return null; }
    }

    private static boolean validVersionsGetter(Method m) {
        return m != null && m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType());
    }

    private static boolean validHeightGetter(Method m, ClassLoader loader) {
        if (m == null || m.getParameterCount() != 0 || m.getReturnType() != Integer.class) return false;
        try {
            Class<?> intf = Class.forName(VERSION_INTERFACE, false, loader);
            return intf.isAssignableFrom(m.getDeclaringClass()) ||
                    intf.isAssignableFrom(m.getDeclaringClass().getInterfaces().length > 0
                            ? m.getDeclaringClass() : m.getDeclaringClass());
        } catch (Throwable ignored) {
            return VERSION_IMPL.equals(m.getDeclaringClass().getName());
        }
    }

    private static Method resolveVersions(DexKitBridge bridge, ClassLoader loader) {
        // Exact semantic model fallback discovered in Instagram 443.0.0.48.82.
        try {
            Class<?> media = Class.forName(MEDIA_CLASS, false, loader);
            Method exact = media.getDeclaredMethod("AAX");
            if (validVersionsGetter(exact)) return exact;
        } catch (Throwable ignored) { }

        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().paramCount(0).usingEqStrings(List.of("video_versions"))));
            Set<String> seen = new HashSet<>();
            for (MethodData data : results) {
                try {
                    Method m = data.getMethodInstance(loader);
                    if (!validVersionsGetter(m)) continue;
                    String key = m.getDeclaringClass().getName() + '#' + m.getName();
                    if (seen.add(key)) return m;
                } catch (Throwable ignored) { }
            }
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ForceReelQuality): semantic versions discovery failed: " + t);
        }
        return null;
    }

    private static Method resolveHeight(ClassLoader loader, DexKitBridge bridge) {
        // The 443.0.0.48.82 Pando implementation exposes the three nullable Integer fields
        // through CFd/Ddy/DnP. Ddy is the height accessor for this model.
        try {
            Class<?> impl = Class.forName(VERSION_IMPL, false, loader);
            Method exact = impl.getDeclaredMethod("Ddy");
            if (exact.getReturnType() == Integer.class && exact.getParameterCount() == 0) return exact;
        } catch (Throwable ignored) { }

        // Future-build fallback: resolve an Integer getter on VideoVersionIntf and prefer a
        // method whose declaring class is a concrete VideoVersion implementation.
        try {
            Class<?> intf = Class.forName(VERSION_INTERFACE, false, loader);
            for (Method m : intf.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == Integer.class) {
                    try {
                        Class<?> impl = Class.forName(VERSION_IMPL, false, loader);
                        Method candidate = impl.getDeclaredMethod(m.getName());
                        if (candidate.getReturnType() == Integer.class) return candidate;
                    } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable ignored) { }

        // Final semantic fallback is intentionally conservative; do not hook an arbitrary
        // Integer getter when its model cannot be established.
        return null;
    }

    private static Object pickBest(List<?> versions, int desired, Method heightGetter) {
        Object best = null;
        int bestDelta = Integer.MAX_VALUE;
        int bestHeight = -1;
        for (Object item : versions) {
            if (item == null) continue;
            try {
                Method getter = heightGetter;
                if (!getter.getDeclaringClass().isInstance(item)) {
                    getter = item.getClass().getDeclaredMethod(heightGetter.getName());
                    getter.setAccessible(true);
                }
                Object raw = getter.invoke(item);
                if (!(raw instanceof Integer)) continue;
                int height = (Integer) raw;
                if (height <= 0) continue;
                int delta = desired == Integer.MAX_VALUE ? -height : Math.abs(height - desired);
                if (delta < bestDelta || (delta == bestDelta && height > bestHeight)) {
                    bestDelta = delta;
                    bestHeight = height;
                    best = item;
                }
            } catch (Throwable ignored) { }
        }
        return best;
    }
}
