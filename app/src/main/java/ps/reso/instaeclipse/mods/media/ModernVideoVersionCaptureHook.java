package ps.reso.instaeclipse.mods.media;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Modern Instagram no longer reliably exposes video versions through VideoVersionIntf.
 * Capture the already-parsed video_versions list instead, then read getUrl() from the
 * returned objects. This removes the dependency on the old interface implementor graph.
 */
final class ModernVideoVersionCaptureHook {
    private static final String CACHE_KEY = "ModernVideoVersionCapture_Getters";
    private static final int MAX_ITEMS = 32;

    private ModernVideoVersionCaptureHook() {}

    static void install(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            List<Method> methods = DexKitCache.isCacheValid()
                    ? DexKitCache.loadMethods(CACHE_KEY, classLoader) : null;

            if (methods == null || methods.isEmpty()) {
                methods = discover(bridge, classLoader);
                if (!methods.isEmpty()) DexKitCache.saveMethods(CACHE_KEY, methods);
            }

            if (methods.isEmpty()) {
                ModuleLog.line("(IE|DL|ModernVideo) ❌ video_versions getter not found");
                return;
            }

            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.enablePostDownload) return;
                    Object result = param.getResult();
                    if (!(result instanceof List<?> list)) return;

                    int count = 0;
                    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                    for (Object item : list) {
                        if (++count > MAX_ITEMS) break;
                        capture(item, visited, 0);
                    }
                }
            };

            int hooked = 0;
            for (Method method : methods) {
                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, hook);
                    hooked++;
                } catch (Throwable t) {
                    ModuleLog.line("(IE|DL|ModernVideo) ⚠️ hook failed: "
                            + method.getDeclaringClass().getName() + "." + method.getName());
                }
            }
            ModuleLog.line("(IE|DL|ModernVideo) ✅ hooked " + hooked + " video_versions getter(s)");
        } catch (Throwable t) {
            ModuleLog.line("(IE|DL|ModernVideo) ❌ install: " + t);
        }
    }

    private static List<Method> discover(DexKitBridge bridge, ClassLoader classLoader) {
        java.util.ArrayList<Method> out = new java.util.ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramCount(0)
                            .usingEqStrings(List.of("video_versions"))));

            for (MethodData data : results) {
                try {
                    Method method = data.getMethodInstance(classLoader);
                    if (method.getParameterCount() != 0
                            || !List.class.isAssignableFrom(method.getReturnType())) continue;
                    String key = method.getDeclaringClass().getName() + '#' + method.getName();
                    if (!seen.add(key)) continue;
                    method.setAccessible(true);
                    out.add(method);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|DL|ModernVideo) ❌ discovery: " + t);
        }
        return out;
    }

    private static void capture(Object value, Set<Object> visited, int depth) {
        if (value == null || depth > 3 || !visited.add(value)) return;

        if (value instanceof String s) {
            FeedVideoDownloadHook.rememberVideoUrl(s);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object child : iterable) {
                if (++count > MAX_ITEMS) break;
                capture(child, visited, depth + 1);
            }
            return;
        }

        Class<?> cls = value.getClass();
        try {
            Method getUrl = null;
            try {
                getUrl = cls.getMethod("getUrl");
            } catch (Throwable ignored) {
                for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Method method : c.getDeclaredMethods()) {
                        if (method.getParameterCount() == 0 && method.getReturnType() == String.class
                                && "getUrl".equals(method.getName())) {
                            getUrl = method;
                            break;
                        }
                    }
                    if (getUrl != null) break;
                }
            }
            if (getUrl != null) {
                getUrl.setAccessible(true);
                Object url = getUrl.invoke(value);
                if (url instanceof String s) FeedVideoDownloadHook.rememberVideoUrl(s);
            }
        } catch (Throwable ignored) {}
    }
}
