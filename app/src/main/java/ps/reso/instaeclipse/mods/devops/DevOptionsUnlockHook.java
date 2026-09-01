package ps.reso.instaeclipse.mods.devops;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.Xposed.Module;
import ps.reso.instaeclipse.utils.core.AdaptiveResolver;
import ps.reso.instaeclipse.utils.core.CompatibilityRuntime;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class DevOptionsUnlockHook {

    private static final String MOBILECONFIG_GETTER_CLASS = "com.facebook.mobileconfig.factory.MobileConfigUnsafeContext";
    private static final String USER_SESSION_CLASS = "com.instagram.common.session.UserSession";
    private static final int MAX_GATE_OPCODES = 16;
    private static final int MIN_CALLER_FAN_IN = 3;
    private static final long[] IS_EMPLOYEE_CONFIG_IDS = {
            36310834636390667L,
            36310830341423371L,
            36310856111227168L,
            36310864701161762L,
    };

    public void handleDevOptions(DexKitBridge bridge) {
        if (DexKitCache.isCacheValid()) {
            AdaptiveResolver.Result cached = AdaptiveResolver.resolve(
                    "DevOptions",
                    java.util.Collections.singletonList(new AdaptiveResolver.Candidate(
                            "cache",
                            10,
                            () -> DexKitCache.loadMethod("DevOptionsMethod", Module.hostClassLoader)
                    )),
                    this::isValidGateMethod
            );
            if (cached.isSuccess()) {
                hookExactMethod(cached.method());
                return;
            }

            String cachedClass = DexKitCache.loadString("DevOptionsClass");
            if (cachedClass != null) {
                hookBooleanMethodsViaReflection(cachedClass);
                return;
            }
        }
        try {
            findAndHookDynamicMethod(bridge);
        } catch (Exception e) {
            CompatibilityRuntime.resolverFailed("DevOptions", e.toString());
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Error handling Dev Options: " + e.getMessage());
        }
    }

    private boolean isValidGateMethod(Method method) {
        if (method == null || method.getReturnType() != boolean.class) return false;
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || !USER_SESSION_CLASS.equals(params[0].getName())) return false;
        try {
            method.setAccessible(true);
            return method.getDeclaringClass() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void findAndHookDynamicMethod(DexKitBridge bridge) {
        try {
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): 🔍 Discovery Tier 1 (String)...");
            List<ClassData> classes = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create().usingStrings("is_employee"))
            );

            boolean found = false;
            if (!classes.isEmpty()) {
                for (ClassData classData : classes) {
                    String className = classData.getName();
                    if (!className.startsWith("X.")) continue;
                    List<MethodData> methods = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .declaredClass(className)
                                    .usingStrings("is_employee"))
                    );
                    for (MethodData method : methods) {
                        if (inspectInvokedMethods(bridge, method)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }

            if (!found) {
                ModuleLog.line("(InstaEclipse | DevOptionsEnable): ⚠️ Tier 1 failed. Discovery Tier 2 (Structural)...");
                MethodData structural = resolveEmployeeGateStructurally(bridge);
                if (structural != null) {
                    try {
                        Method targetMethod = structural.getMethodInstance(Module.hostClassLoader);
                        if (isValidGateMethod(targetMethod)) {
                            DexKitCache.saveMethod("DevOptionsMethod", targetMethod);
                            hookExactMethod(targetMethod);
                            ModuleLog.line("(InstaEclipse | DevOptionsEnable): 🎯 Found via structural match: "
                                    + structural.getClassName() + "." + structural.getName());
                            found = true;
                        }
                    } catch (Throwable e) {
                        ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Structural match failed to bind: " + e.getMessage());
                    }
                }
            }

            if (!found) {
                ModuleLog.line("(InstaEclipse | DevOptionsEnable): ⚠️ Tier 2 failed. Discovery Tier 3 (Config ID)...");
                for (long configId : IS_EMPLOYEE_CONFIG_IDS) {
                    List<MethodData> idMethods = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .usingNumbers(configId)
                                    .returnType("boolean")
                                    .paramCount(1))
                    );
                    if (!idMethods.isEmpty()) {
                        String targetClass = idMethods.get(0).getClassName();
                        ModuleLog.line("(InstaEclipse | DevOptionsEnable): 🎯 Found via Config ID " + configId + " in: " + targetClass);
                        DexKitCache.saveString("DevOptionsClass", targetClass);
                        hookAllBooleanMethodsInClass(bridge, targetClass);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Tier 3 failed. Debugging global references...");
                List<MethodData> debugMethods = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create().usingStrings("is_employee")));
                for (MethodData m : debugMethods) {
                    ModuleLog.line("(InstaEclipse | DevOptionsDebug): String 'is_employee' found in: " + m.getClassName() + "." + m.getName());
                }
                CompatibilityRuntime.resolverFailed("DevOptions", "all discovery tiers failed");
            }
        } catch (Exception e) {
            CompatibilityRuntime.resolverFailed("DevOptions", e.toString());
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Error during discovery: " + e.getMessage());
        }
    }

    private MethodData resolveEmployeeGateStructurally(DexKitBridge bridge) {
        try {
            List<MethodData> getters = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass(MOBILECONFIG_GETTER_CLASS)
                            .returnType("boolean")
                            .paramTypes("long")));
            if (getters.isEmpty()) return null;
            MethodsMatcher getterInvoke = MethodsMatcher.create();
            for (MethodData getter : getters) getterInvoke.add(MethodMatcher.create(getter.getDescriptor()));

            List<MethodData> candidates = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .returnType("boolean")
                            .paramTypes(USER_SESSION_CLASS)
                            .invokeMethods(getterInvoke)));

            MethodData best = null;
            int bestFanIn = 0;
            for (MethodData candidate : candidates) {
                if (candidate.getOpCodes().size() > MAX_GATE_OPCODES) continue;
                Set<String> callerClasses = new HashSet<>();
                for (MethodData caller : candidate.getCallers()) callerClasses.add(caller.getClassName());
                if (callerClasses.size() > bestFanIn) {
                    bestFanIn = callerClasses.size();
                    best = candidate;
                }
            }
            return bestFanIn >= MIN_CALLER_FAN_IN ? best : null;
        } catch (Exception e) {
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Structural discovery error: " + e.getMessage());
            return null;
        }
    }

    private void hookExactMethod(Method m) {
        try {
            if (!isValidGateMethod(m)) throw new IllegalArgumentException("resolved method failed gate validation");
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (FeatureFlags.isDevEnabled) {
                        param.setResult(true);
                        FeatureStatusTracker.setHooked("DevOptions");
                    }
                }
            });
            CompatibilityRuntime.installed("DevOptions", "validated-method");
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ✅ Hooked: " + m.getDeclaringClass().getName() + "." + m.getName());
        } catch (Throwable e) {
            CompatibilityRuntime.runtimeFailed("DevOptions", e);
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Failed to hook resolved method: " + e.getMessage());
        }
    }

    private boolean inspectInvokedMethods(DexKitBridge bridge, MethodData method) {
        try {
            List<MethodData> invokedMethods = method.getInvokes();
            if (invokedMethods.isEmpty()) return false;
            for (MethodData invokedMethod : invokedMethods) {
                String returnType = String.valueOf(invokedMethod.getReturnType());
                if (!returnType.contains("boolean")) continue;
                List<String> paramTypes = new ArrayList<>();
                for (Object param : invokedMethod.getParamTypes()) paramTypes.add(String.valueOf(param));
                if (paramTypes.size() == 1 && paramTypes.get(0).contains(USER_SESSION_CLASS)) {
                    String targetClass = invokedMethod.getClassName();
                    ModuleLog.line("(InstaEclipse | DevOptionsEnable): 📦 Hooking via String detection: " + targetClass);
                    DexKitCache.saveString("DevOptionsClass", targetClass);
                    hookAllBooleanMethodsInClass(bridge, targetClass);
                    return true;
                }
            }
        } catch (Exception e) {
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Error inspecting invoked methods: " + e.getMessage());
        }
        return false;
    }

    private void hookBooleanMethodsViaReflection(String className) {
        try {
            Class<?> clazz = Module.hostClassLoader.loadClass(className);
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (FeatureFlags.isDevEnabled) {
                        param.setResult(true);
                        FeatureStatusTracker.setHooked("DevOptions");
                    }
                }
            };
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getReturnType() != boolean.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 1 || !USER_SESSION_CLASS.equals(params[0].getName())) continue;
                m.setAccessible(true);
                XposedBridge.hookMethod(m, hook);
                ModuleLog.line("(InstaEclipse | DevOptionsEnable): ✅ Hooked (cache): " + className + "." + m.getName());
            }
        } catch (Throwable e) {
            CompatibilityRuntime.runtimeFailed("DevOptions", e);
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Reflection fallback failed: " + e.getMessage());
        }
    }

    private void hookAllBooleanMethodsInClass(DexKitBridge bridge, String className) {
        try {
            List<MethodData> methods = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create().declaredClass(className)));
            for (MethodData method : methods) {
                String returnType = String.valueOf(method.getReturnType());
                List<String> paramTypes = new ArrayList<>();
                for (Object param : method.getParamTypes()) paramTypes.add(String.valueOf(param));
                if (returnType.contains("boolean") && paramTypes.size() == 1 && paramTypes.get(0).contains(USER_SESSION_CLASS)) {
                    try {
                        Method targetMethod = method.getMethodInstance(Module.hostClassLoader);
                        if (!isValidGateMethod(targetMethod)) continue;
                        XposedHelpers.findAndHookMethod(targetMethod.getDeclaringClass(), targetMethod.getName(), targetMethod.getParameterTypes()[0], new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (FeatureFlags.isDevEnabled) {
                                    param.setResult(true);
                                    FeatureStatusTracker.setHooked("DevOptions");
                                }
                            }
                        });
                        CompatibilityRuntime.installed("DevOptions", "validated-class-method");
                        ModuleLog.line("(InstaEclipse | DevOptionsEnable): ✅ Hooked: " + method.getClassName() + "." + method.getName());
                    } catch (Throwable e) {
                        CompatibilityRuntime.runtimeFailed("DevOptions", e);
                        ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Failed to hook " + method.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            CompatibilityRuntime.resolverFailed("DevOptions", e.toString());
            ModuleLog.line("(InstaEclipse | DevOptionsEnable): ❌ Error while hooking class: " + className + " → " + e.getMessage());
        }
    }
}
