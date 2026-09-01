package ps.reso.instaeclipse.mods.ghost;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class GhostReplayLimitHook {

    private static final String VISUAL_VIEWER_CLASS =
            "instagram.features.direct.visual.internal.DirectVisualMessageViewerController";

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        hookUpdateMethod(bridge, classLoader);
        hookParseFromJsonMethod(bridge, classLoader);
        hookSyncMethod(bridge, classLoader);
    }

    /**
     * Hooks the current visual-message viewer state update. In Instagram 443 the
     * old pair of strings was split across unrelated methods; the real update
     * routine contains the visual-message validation string on the viewer controller.
     */
    private void hookUpdateMethod(DexKitBridge bridge, ClassLoader classLoader) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (FeatureFlags.enableUnlimitedReplays) param.setResult(null);
            }
        };

        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod("Replays_update", classLoader);
            if (cached != null) {
                XposedBridge.hookMethod(cached, hook);
                ModuleLog.line("[IE] ✅ Ghost Replay – update");
                FeatureStatusTracker.setHooked("UnlimitedReplays");
                return;
            }
        }

        try {
            List<MethodData> methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass(VISUAL_VIEWER_CLASS)
                            .returnType("void")
                            .usingStrings("Visual message is missing from thread entry")));

            Method resolved = null;
            MethodData resolvedData = null;
            for (MethodData md : methods) {
                try {
                    Method m = md.getMethodInstance(classLoader);
                    if (m.getReturnType() != void.class) continue;
                    resolved = m;
                    resolvedData = md;
                    break;
                } catch (Throwable ignored) {}
            }

            // Keep a narrow generic fallback for minor packaging changes while still
            // requiring the exact current visual-message marker string.
            if (resolved == null) {
                methods = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .returnType("void")
                                .usingStrings("Visual message is missing from thread entry")));
                for (MethodData md : methods) {
                    try {
                        Method m = md.getMethodInstance(classLoader);
                        if (!m.getDeclaringClass().getName().contains("VisualMessageViewerController")) continue;
                        resolved = m;
                        resolvedData = md;
                        break;
                    } catch (Throwable ignored) {}
                }
            }

            if (resolved == null) {
                ModuleLog.line("(IE|Replays) ❌ update method not found in visual message viewer");
                return;
            }

            resolved.setAccessible(true);
            DexKitCache.saveMethod("Replays_update", resolved);
            XposedBridge.hookMethod(resolved, hook);
            ModuleLog.line("(IE|Replays) ✅ update hook → "
                    + resolvedData.getClassName() + "." + resolvedData.getName());
            FeatureStatusTracker.setHooked("UnlimitedReplays");
        } catch (Throwable t) {
            ModuleLog.line("(IE|Replays) ❌ hookUpdateMethod: " + t);
        }
    }

    private void hookParseFromJsonMethod(DexKitBridge bridge, ClassLoader classLoader) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.enableUnlimitedReplays) return;
                zeroReplayCountFields(param.thisObject);
                if (param.getResult() != null && param.getResult() != param.thisObject)
                    zeroReplayCountFields(param.getResult());
            }
        };

        if (DexKitCache.isCacheValid()) {
            List<Method> cached = DexKitCache.loadMethods("Replays_parse", classLoader);
            if (cached != null && !cached.isEmpty()) {
                for (Method m : cached) XposedBridge.hookMethod(m, hook);
                ModuleLog.line("[IE] ✅ Ghost Replay – parseFromJson");
                return;
            }
        }

        try {
            List<MethodData> methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .usingStrings("seen_count", "tap_models")));

            List<Method> hooked = new ArrayList<>();
            for (MethodData md : methods) {
                try {
                    Method m = md.getMethodInstance(classLoader);
                    XposedBridge.hookMethod(m, hook);
                    hooked.add(m);
                    ModuleLog.line("(IE|Replays) ✅ parseFromJson hook → "
                            + md.getClassName() + "." + md.getName());
                } catch (Throwable ignored) {}
            }
            if (hooked.isEmpty()) {
                ModuleLog.line("(IE|Replays) ❌ parseFromJson method not found");
            } else {
                DexKitCache.saveMethods("Replays_parse", hooked);
                ModuleLog.line("[IE] ✅ Ghost Replay – parseFromJson");
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|Replays) ❌ hookParseFromJsonMethod: " + t);
        }
    }

    private void hookSyncMethod(DexKitBridge bridge, ClassLoader classLoader) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (FeatureFlags.enableUnlimitedReplays) param.setResult(null);
            }
        };

        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod("Replays_sync", classLoader);
            if (cached != null) {
                XposedBridge.hookMethod(cached, hook);
                ModuleLog.line("[IE] ✅ Ghost Replay – sync");
                FeatureStatusTracker.setHooked("UnlimitedReplays");
                return;
            }
        }

        try {
            List<MethodData> methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .paramTypes("com.instagram.common.session.UserSession", null, null)
                            .returnType("void")));

            for (MethodData md : methods) {
                try {
                    Method m = md.getMethodInstance(classLoader);
                    if (!java.lang.reflect.Modifier.isSynchronized(m.getModifiers())) continue;
                    DexKitCache.saveMethod("Replays_sync", m);
                    XposedBridge.hookMethod(m, hook);
                    ModuleLog.line("[IE] ✅ Ghost Replay – sync");
                    ModuleLog.line("(IE|Replays) ✅ sync hook → " + md.getClassName() + "." + md.getName());
                    FeatureStatusTracker.setHooked("UnlimitedReplays");
                    return;
                } catch (Throwable ignored) {}
            }
            ModuleLog.line("(IE|Replays) ❌ sync method not found");
        } catch (Throwable t) {
            ModuleLog.line("(IE|Replays) ❌ hookSyncMethod: " + t);
        }
    }

    private static void zeroReplayCountFields(Object obj) {
        if (obj == null) return;
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                f.setAccessible(true);
                int val = f.getInt(obj);
                if (val >= 1 && val <= 10) f.setInt(obj, 0);
            }
        } catch (Throwable ignored) {}
    }
}
