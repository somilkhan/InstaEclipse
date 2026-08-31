package ps.reso.instaeclipse.mods.media;

import android.app.AndroidAppHelper;
import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.i18n.I18n;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Compatibility layer for Instagram's self-story menu. Recent builds use a different
 * option-list builder and dispatcher for the viewer's own story, while StoryDownloadHook
 * historically resolved the public-story path only.
 */
public final class StorySelfMenuCompatibilityHook {
    private static final String ANCHOR = "[INTERNAL] Pause Playback";
    private static final String BUTTON_CACHE = "StoryDownload_button_v2";
    private static final String CLICK_CACHE = "StoryDownload_click_v2";

    private StorySelfMenuCompatibilityHook() {}

    public static void install(DexKitBridge bridge, ClassLoader classLoader) {
        installButtons(bridge, classLoader);
        installClicks(bridge, classLoader);
    }

    private static void installButtons(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            List<Method> methods = DexKitCache.isCacheValid()
                    ? DexKitCache.loadMethods(BUTTON_CACHE, classLoader) : null;
            if (methods == null || methods.isEmpty()) methods = discoverButtons(bridge, classLoader);
            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Story|Self) ⚠️ no additional option builders found");
                return;
            }

            final List<Method> targets = methods;
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.enableStoryDownload) return;
                    Object result = param.getResult();
                    if (!(result instanceof CharSequence[] original)) return;
                    String label = I18n.t(AndroidAppHelper.currentApplication(), R.string.ig_dl_title);
                    for (CharSequence item : original) if (label.contentEquals(item)) return;
                    CharSequence[] extended = new CharSequence[original.length + 1];
                    System.arraycopy(original, 0, extended, 0, original.length);
                    extended[original.length] = label;
                    param.setResult(extended);
                }
            };

            List<Method> hooked = new ArrayList<>();
            for (Method method : targets) {
                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, hook);
                    hooked.add(method);
                } catch (Throwable ignored) {}
            }
            if (!hooked.isEmpty()) DexKitCache.saveMethods(BUTTON_CACHE, hooked);
            ModuleLog.line("(IE|Story|Self) ✅ option builders hooked=" + hooked.size());
        } catch (Throwable t) {
            ModuleLog.line("(IE|Story|Self) ❌ button compatibility: " + t);
        }
    }

    private static List<Method> discoverButtons(DexKitBridge bridge, ClassLoader classLoader) {
        List<Method> out = new ArrayList<>();
        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create().usingStrings(ANCHOR)));
            Set<String> seen = new java.util.HashSet<>();
            for (MethodData data : results) {
                try {
                    Method method = data.getMethodInstance(classLoader);
                    if (!method.getReturnType().isArray()
                            || !CharSequence.class.isAssignableFrom(method.getReturnType().getComponentType())) continue;
                    String key = method.getDeclaringClass().getName() + '#' + method.getName();
                    if (!seen.add(key)) continue;
                    method.setAccessible(true);
                    out.add(method);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|Story|Self) ❌ button discovery: " + t);
        }
        return out;
    }

    private static void installClicks(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            List<Method> candidates = DexKitCache.isCacheValid()
                    ? DexKitCache.loadMethods(CLICK_CACHE, classLoader) : null;
            if (candidates == null || candidates.isEmpty()) candidates = discoverClicks(bridge, classLoader);

            // Do not double-hook the legacy dispatcher already owned by StoryDownloadHook.
            Set<String> legacy = new java.util.HashSet<>();
            if (DexKitCache.isCacheValid()) {
                List<Method> old = DexKitCache.loadMethods("StoryDownload_click", classLoader);
                if (old != null) for (Method method : old) legacy.add(signature(method));
                Method single = DexKitCache.loadMethod("StoryDownload_click", classLoader);
                if (single != null) legacy.add(signature(single));
            }

            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.enableStoryDownload) return;
                    CharSequence tapped = null;
                    for (Object arg : param.args) {
                        if (arg instanceof CharSequence cs) { tapped = cs; break; }
                    }
                    String label = I18n.t(AndroidAppHelper.currentApplication(), R.string.ig_dl_title);
                    if (tapped == null || !label.contentEquals(tapped)) return;

                    try {
                        Object holder = invokePrivateStatic("findReelItemHolder",
                                new Class<?>[]{XC_MethodHook.MethodHookParam.class}, param);
                        if (holder == null) holder = param.thisObject;
                        Context context = findContext(param.thisObject);
                        if (context == null) {
                            for (Object arg : param.args) {
                                context = findContext(arg);
                                if (context != null) break;
                            }
                        }
                        if (context == null) {
                            ModuleLog.line("(IE|Story|Self) ❌ context not found");
                            return;
                        }

                        Class<?> hookClass = StoryDownloadHook.class;
                        Method extract = hookClass.getDeclaredMethod("extractStoryMediaOptions", Context.class, Object.class);
                        extract.setAccessible(true);
                        Object mediaOptions = extract.invoke(null, context, holder);
                        if (mediaOptions == null) {
                            ModuleLog.line("(IE|Story|Self) ❌ story media not resolved");
                            return;
                        }

                        Method username = hookClass.getDeclaredMethod("extractUsernameFromReelItemHolder", Object.class);
                        Method mediaId = hookClass.getDeclaredMethod("extractMediaIdFromReelItemHolder", Object.class);
                        Method handle = null;
                        username.setAccessible(true);
                        mediaId.setAccessible(true);

                        Object user = username.invoke(null, holder);
                        Object id = mediaId.invoke(null, holder);
                        for (Method method : hookClass.getDeclaredMethods()) {
                            if (!"handleStoryMedia".equals(method.getName()) || method.getParameterCount() != 4) continue;
                            handle = method;
                            break;
                        }
                        if (handle == null) {
                            ModuleLog.line("(IE|Story|Self) ❌ story media dispatcher missing");
                            return;
                        }
                        handle.setAccessible(true);
                        param.setResult(null);
                        handle.invoke(null, context, mediaOptions, user, id);
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Story|Self) ❌ click compatibility: " + t);
                    }
                }
            };

            List<Method> hooked = new ArrayList<>();
            for (Method method : candidates) {
                if (legacy.contains(signature(method))) continue;
                if (!hasCharSequenceParam(method)) continue;
                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, hook);
                    hooked.add(method);
                } catch (Throwable ignored) {}
            }
            if (!hooked.isEmpty()) DexKitCache.saveMethods(CLICK_CACHE, hooked);
            ModuleLog.line("(IE|Story|Self) ✅ click dispatchers hooked=" + hooked.size());
        } catch (Throwable t) {
            ModuleLog.line("(IE|Story|Self) ❌ click compatibility: " + t);
        }
    }

    private static List<Method> discoverClicks(DexKitBridge bridge, ClassLoader classLoader) {
        List<Method> out = new ArrayList<>();
        try {
            List<MethodData> results = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .returnType("void")
                            .usingStrings(ANCHOR)));
            Set<String> seen = new java.util.HashSet<>();
            for (MethodData data : results) {
                try {
                    Method method = data.getMethodInstance(classLoader);
                    if (!hasCharSequenceParam(method)) continue;
                    String key = signature(method);
                    if (!seen.add(key)) continue;
                    method.setAccessible(true);
                    out.add(method);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|Story|Self) ❌ click discovery: " + t);
        }
        return out;
    }

    private static boolean hasCharSequenceParam(Method method) {
        for (Class<?> type : method.getParameterTypes()) {
            if (CharSequence.class.isAssignableFrom(type)) return true;
        }
        return false;
    }

    private static String signature(Method method) {
        return method.getDeclaringClass().getName() + '#' + method.getName()
                + java.util.Arrays.toString(method.getParameterTypes());
    }

    private static Object invokePrivateStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = StoryDownloadHook.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Context findContext(Object root) {
        if (root == null) return null;
        if (root instanceof Context context) return context;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return findContextRecursive(root, visited, 0);
    }

    private static Context findContextRecursive(Object value, Set<Object> visited, int depth) {
        if (value == null || depth > 4 || !visited.add(value)) return null;
        if (value instanceof Context context) return context;
        Class<?> cls = value.getClass();
        while (cls != null && cls != Object.class) {
            for (java.lang.reflect.Field field : cls.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object nested = field.get(value);
                    Context found = findContextRecursive(nested, visited, depth + 1);
                    if (found != null) return found;
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
