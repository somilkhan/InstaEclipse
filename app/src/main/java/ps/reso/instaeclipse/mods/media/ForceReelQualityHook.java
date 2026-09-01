package ps.reso.instaeclipse.mods.media;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
 * Forces reel playback to a selected video height by filtering Instagram's already-parsed
 * video-version list. Discovery deliberately avoids hard-coded obfuscated/concrete Instagram
 * class names so an APK refactor does not make the feature silently disappear.
 */
public final class ForceReelQualityHook {

    private static final int HEIGHT_HASH = "height".hashCode();
    private static final String CACHE_GETTER_KEY = "ForceReelQuality_VideoVersionsGetter";
    private static final String CACHE_HEIGHT_KEY = "ForceReelQuality_HeightGetter";

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            Method versionsGetter = DexKitCache.isCacheValid()
                    ? DexKitCache.loadMethod(CACHE_GETTER_KEY, classLoader) : null;
            Method heightGetter = DexKitCache.isCacheValid()
                    ? DexKitCache.loadMethod(CACHE_HEIGHT_KEY, classLoader) : null;

            if (versionsGetter == null || heightGetter == null) {
                versionsGetter = resolveVideoVersionsGetter(bridge, classLoader);
                heightGetter = resolveHeightGetter(bridge, classLoader);
            }

            if (versionsGetter == null || heightGetter == null) {
                ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ dynamic discovery failed"
                        + " versions=" + (versionsGetter != null)
                        + " height=" + (heightGetter != null));
                return;
            }

            versionsGetter.setAccessible(true);
            heightGetter.setAccessible(true);
            DexKitCache.saveMethod(CACHE_GETTER_KEY, versionsGetter);
            DexKitCache.saveMethod(CACHE_HEIGHT_KEY, heightGetter);

            final Method finalHeightGetter = heightGetter;
            XposedBridge.hookMethod(versionsGetter, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (FeatureFlags.forceReelQuality <= 0) return;
                    try {
                        Object result = param.getResult();
                        if (!(result instanceof List<?> versions) || versions.size() <= 1) return;

                        Object chosen = pickBestQuality(versions,
                                FeatureFlags.forceReelQuality, finalHeightGetter);
                        if (chosen != null) param.setResult(Collections.singletonList(chosen));
                    } catch (Throwable t) {
                        ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ runtime filtering: " + t);
                    }
                }
            });

            FeatureStatusTracker.setHooked("ForceReelQuality");
            ModuleLog.line("(InstaEclipse | ForceReelQuality): ✅ hooked "
                    + versionsGetter.getDeclaringClass().getName() + "." + versionsGetter.getName()
                    + " height=" + heightGetter.getDeclaringClass().getName() + "." + heightGetter.getName());
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ install: " + t);
        }
    }

    private static Method resolveVideoVersionsGetter(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(0)
                            .usingEqStrings(List.of("video_versions"))));

            Set<String> seen = new HashSet<>();
            for (MethodData md : results) {
                try {
                    Method m = md.getMethodInstance(classLoader);
                    if (m.getParameterCount() != 0 || !List.class.isAssignableFrom(m.getReturnType())) continue;
                    String key = m.getDeclaringClass().getName() + '#' + m.getName();
                    if (!seen.add(key)) continue;
                    m.setAccessible(true);
                    return m;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ video_versions discovery: " + t);
        }
        return null;
    }

    private static Method resolveHeightGetter(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(0)
                            .returnType("java.lang.Integer")
                            .usingNumbers(List.of(HEIGHT_HASH))));

            // Prefer methods declared by a model class that looks like a video-version model.
            // If Instagram renames/moves that class, the structural fallback still works.
            List<Method> fallback = new ArrayList<>();
            for (MethodData md : results) {
                try {
                    Method m = md.getMethodInstance(classLoader);
                    if (m.getParameterCount() != 0 || m.getReturnType() != Integer.class) continue;
                    String owner = m.getDeclaringClass().getName();
                    if (owner.contains("VideoVersion") || owner.contains("VideoSize")) {
                        m.setAccessible(true);
                        return m;
                    }
                    fallback.add(m);
                } catch (Throwable ignored) {}
            }
            if (!fallback.isEmpty()) {
                fallback.get(0).setAccessible(true);
                return fallback.get(0);
            }
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ForceReelQuality): ❌ height discovery: " + t);
        }
        return null;
    }

    private static Object pickBestQuality(List<?> versions, int desired, Method heightGetter) {
        Object best = null;
        int bestDelta = Integer.MAX_VALUE;
        int bestHeight = -1;

        for (Object item : versions) {
            if (item == null) continue;
            try {
                Method getter = heightGetter;
                if (!getter.getDeclaringClass().isInstance(item)) {
                    // The DexKit result may be declared on a shared parent/interface. Try
                    // the same signature on the runtime class before discarding the version.
                    try {
                        getter = item.getClass().getMethod(heightGetter.getName());
                        getter.setAccessible(true);
                    } catch (Throwable ignored) {
                        continue;
                    }
                }

                Object rawHeight = getter.invoke(item);
                if (!(rawHeight instanceof Integer)) continue;
                int height = (Integer) rawHeight;
                if (height <= 0) continue;

                if (desired == Integer.MAX_VALUE) {
                    if (height > bestHeight) {
                        bestHeight = height;
                        best = item;
                    }
                } else {
                    int delta = Math.abs(height - desired);
                    if (delta < bestDelta || (delta == bestDelta && height > bestHeight)) {
                        bestDelta = delta;
                        bestHeight = height;
                        best = item;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return best;
    }
}
