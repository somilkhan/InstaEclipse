package ps.reso.instaeclipse.mods.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.mods.ui.UIHookManager;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Custom Theme: hooks Android resource/color resolution and the direct/stateful color mutation
 * APIs Instagram uses for its modern UI. Stateful colors are explicitly preserved so selected
 * controls such as the liked-heart retain their semantic active color.
 */
public class IgThemeHook {

    private static volatile boolean installed;
    private static volatile Field typedArrayAttrsField;
    private static volatile Field typedArrayResourcesField;

    public void install(ClassLoader classLoader) {
        if (installed) return;
        try {
            hookResolveAttribute(classLoader);
            hookGetColor(classLoader);
            hookContextGetColor();
            hookTypedArrayGetColor(classLoader);
            hookStatefulColors(classLoader);
            hookDirectColorMutators();
            hookActivityLifecycle();
            hookPhoneWindowColors(classLoader);
            installed = true;
            FeatureStatusTracker.setHooked("CustomTheme");
            ModuleLog.line("(InstaEclipse | Theme): hooks installed enabled=" + FeatureFlags.customThemeEnabled);
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Theme): hook failed", t);
        }
    }

    private void hookResolveAttribute(final ClassLoader cl) {
        XposedHelpers.findAndHookMethod("android.content.res.Resources$Theme", cl, "resolveAttribute",
                int.class, TypedValue.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (IgColorRemapEngine.isBypassing() || !FeatureFlags.customThemeEnabled) return;
                        int attrId = (Integer) param.args[0];
                        TypedValue out = (TypedValue) param.args[1];
                        if (out == null) return;
                        if (!IgThemeEngine.isInitialized()) {
                            try {
                                Resources res = (Resources) XposedHelpers.getObjectField(param.thisObject, "mResources");
                                if (res != null) IgThemeEngine.ensureInitialized(res, cl);
                            } catch (Throwable ignored) {}
                        }
                        Integer override = IgThemeEngine.colorForAttr(attrId);
                        if (override != null) {
                            IgThemeEngine.applyAttrOverride(attrId, out);
                            param.setResult(true);
                        } else if (IgColorRemapEngine.isReady()) {
                            if ((out.type == TypedValue.TYPE_INT_COLOR_ARGB8 || out.type == TypedValue.TYPE_INT_COLOR_RGB8)) {
                                int remapped = IgColorRemapEngine.remap(out.data);
                                if (remapped != out.data) {
                                    out.data = remapped;
                                    param.setResult(true);
                                }
                            }
                        }
                    }
                });
    }

    private void hookGetColor(final ClassLoader cl) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                int resId = (Integer) param.args[0];
                if (IgThemeEngine.looksLikeDirectColor(resId)) {
                    param.setResult(IgColorRemapEngine.remap(resId));
                    return;
                }
                if (IgThemeEngine.looksLikeResourceId(resId)) {
                    if (!IgThemeEngine.isInitialized()) {
                        Resources res = (Resources) param.thisObject;
                        IgThemeEngine.ensureInitialized(res, cl);
                    }
                    Integer override = IgThemeEngine.colorForResource(resId);
                    if (override != null) param.setResult(override);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || param.getThrowable() != null || !IgColorRemapEngine.isReady()) return;
                int resId = (Integer) param.args[0];
                if (!IgThemeEngine.looksLikeDirectColor(resId) && IgThemeEngine.looksLikeResourceId(resId)
                        && IgThemeEngine.colorForResource(resId) == null) {
                    Object result = param.getResult();
                    if (result instanceof Integer) {
                        int resolved = (Integer) result;
                        int remapped = IgColorRemapEngine.remap(resolved);
                        if (remapped != resolved) param.setResult(remapped);
                    }
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getColor", int.class, Resources.Theme.class, hook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getColor", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookContextGetColor() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                int resId = (Integer) param.args[0];
                if (IgThemeEngine.looksLikeDirectColor(resId)) {
                    param.setResult(IgColorRemapEngine.remap(resId));
                    return;
                }
                if (IgThemeEngine.looksLikeResourceId(resId)) {
                    if (!IgThemeEngine.isInitialized()) {
                        Context ctx = (Context) param.thisObject;
                        IgThemeEngine.ensureInitialized(ctx);
                    }
                    Integer override = IgThemeEngine.colorForResource(resId);
                    if (override != null) param.setResult(override);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || param.getThrowable() != null || !IgColorRemapEngine.isReady()) return;
                int resId = (Integer) param.args[0];
                if (!IgThemeEngine.looksLikeDirectColor(resId) && IgThemeEngine.looksLikeResourceId(resId)
                        && IgThemeEngine.colorForResource(resId) == null) {
                    Object result = param.getResult();
                    if (result instanceof Integer) {
                        int resolved = (Integer) result;
                        int remapped = IgColorRemapEngine.remap(resolved);
                        if (remapped != resolved) param.setResult(remapped);
                    }
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Context.class, "getColor", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookTypedArrayGetColor(final ClassLoader cl) {
        XposedHelpers.findAndHookMethod(TypedArray.class, "getColor", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (IgColorRemapEngine.isBypassing() || !FeatureFlags.customThemeEnabled) return;
                try {
                    TypedArray ta = (TypedArray) param.thisObject;
                    int index = (Integer) param.args[0];
                    int[] attrs = typedArrayAttributes(ta);
                    if (attrs == null || index < 0 || index >= attrs.length) return;
                    if (!IgThemeEngine.isInitialized()) {
                        Resources res = typedArrayResources(ta);
                        if (res != null) IgThemeEngine.ensureInitialized(res, cl);
                    }
                    Integer override = IgThemeEngine.colorForAttr(attrs[index]);
                    if (override != null) {
                        param.setResult(override);
                        return;
                    }
                    if (IgColorRemapEngine.isReady()) {
                        Object result = param.getResult();
                        if (result instanceof Integer) {
                            int resolved = (Integer) result;
                            int remapped = IgColorRemapEngine.remap(resolved);
                            if (remapped != resolved) param.setResult(remapped);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    /** Handles ColorStateList-backed selected/activated colors such as the liked-heart. */
    private void hookStatefulColors(final ClassLoader cl) {
        XC_MethodHook resourceListHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || param.getThrowable() != null) return;
                Object result = param.getResult();
                if (result instanceof ColorStateList) {
                    param.setResult(remapColorStateList((ColorStateList) result));
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getColorStateList", int.class,
                    Resources.Theme.class, resourceListHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getColorStateList", int.class,
                    resourceListHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Context.class, "getColorStateList", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                                    || param.getThrowable() != null) return;
                            Object result = param.getResult();
                            if (result instanceof ColorStateList) {
                                param.setResult(remapColorStateList((ColorStateList) result));
                            }
                        }
                    });
        } catch (Throwable ignored) {}

        XC_MethodHook tintHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || param.args == null || param.args.length == 0) return;
                if (param.args[0] instanceof ColorStateList) {
                    param.args[0] = remapColorStateList((ColorStateList) param.args[0]);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(ImageView.class, "setImageTintList", ColorStateList.class, tintHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", ColorStateList.class, tintHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Drawable.class, "setTintList", ColorStateList.class, tintHook);
        } catch (Throwable ignored) {}
    }

    private static ColorStateList remapColorStateList(ColorStateList source) {
        if (source == null || IgColorRemapEngine.isBypassing()) return source;
        try {
            int[] colors = source.getColors();
            int[][] states = source.getStates();
            if (colors == null || states == null || colors.length == 0) return source;
            int[] remapped = new int[colors.length];
            boolean changed = false;
            for (int i = 0; i < colors.length; i++) {
                remapped[i] = IgColorRemapEngine.remap(colors[i]);
                changed |= remapped[i] != colors[i];
            }
            return changed ? new ColorStateList(states, remapped) : source;
        } catch (Throwable ignored) {
            return source;
        }
    }

    /** Covers colors passed directly by modern Instagram UI code instead of through resources. */
    private void hookDirectColorMutators() {
        try {
            XposedHelpers.findAndHookMethod(View.class, "setBackgroundColor", int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.shouldSkipRemap(param.thisObject)) return;
                    param.args[0] = IgColorRemapEngine.remap((Integer) param.args[0]);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                    param.args[0] = IgColorRemapEngine.remap((Integer) param.args[0]);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Drawable.class, "setTint", int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                    param.args[0] = IgColorRemapEngine.remap((Integer) param.args[0]);
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookActivityLifecycle() {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!IgThemeEngine.isActive()) return;
                Activity activity = (Activity) param.thisObject;
                IgThemeEngine.ensureInitialized(activity);
                IgColorRemapEngine.ensureBuilt(activity);
                applyWindowColors(activity);
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!IgThemeEngine.isActive()) return;
                Activity activity = (Activity) param.thisObject;
                IgThemeEngine.ensureInitialized(activity);
                IgColorRemapEngine.ensureBuilt(activity);
                applyWindowColors(activity);
            }
        });
    }

    private void hookPhoneWindowColors(ClassLoader cl) {
        XC_MethodHook statusHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (IgThemeEngine.isActive()) param.args[0] = IgThemeEngine.getActivePalette().statusBar;
            }
        };
        XC_MethodHook navHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (IgThemeEngine.isActive()) param.args[0] = IgThemeEngine.getActivePalette().navigation;
            }
        };
        if (!tryHookPhoneWindow(cl, statusHook, navHook)) tryHookPhoneWindow(null, statusHook, navHook);
    }

    private boolean tryHookPhoneWindow(ClassLoader cl, XC_MethodHook statusHook, XC_MethodHook navHook) {
        try {
            XposedHelpers.findAndHookMethod("com.android.internal.policy.PhoneWindow", cl, "setStatusBarColor", int.class, statusHook);
            XposedHelpers.findAndHookMethod("com.android.internal.policy.PhoneWindow", cl, "setNavigationBarColor", int.class, navHook);
            ModuleLog.line("(InstaEclipse | Theme): PhoneWindow color hooks installed");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void refreshCurrentActivity() {
        Activity activity = UIHookManager.getCurrentActivity();
        if (activity == null || activity.isFinishing()) return;
        IgThemeEngine.ensureInitialized(activity);
        if (!FeatureFlags.customThemeEnabled) return;
        IgColorRemapEngine.ensureBuilt(activity);
        applyWindowColors(activity);
        activity.getWindow().getDecorView().post(activity::recreate);
    }

    static void applyWindowColors(Activity activity) {
        if (activity == null || !IgThemeEngine.isActive()) return;
        try {
            IgThemePalette palette = IgThemeEngine.getActivePalette();
            Window window = activity.getWindow();
            if (window == null) return;
            window.setStatusBarColor(palette.statusBar);
            window.setNavigationBarColor(palette.navigation);
            View decor = window.getDecorView();
            if (decor != null) decor.setBackgroundColor(palette.background);
            if (Build.VERSION.SDK_INT >= 29) {
                window.setStatusBarContrastEnforced(false);
                window.setNavigationBarContrastEnforced(false);
            }
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    boolean lightBg = Color.red(palette.background) + Color.green(palette.background) + Color.blue(palette.background) > 382;
                    controller.setSystemBarsAppearance(lightBg ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
                return;
            }
            applyLegacySystemUiVisibility(window, palette.background);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private static void applyLegacySystemUiVisibility(Window window, int backgroundColor) {
        int flags = window.getDecorView().getSystemUiVisibility();
        boolean lightBg = Color.red(backgroundColor) + Color.green(backgroundColor) + Color.blue(backgroundColor) > 382;
        if (lightBg) {
            flags = flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags = flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private static int[] typedArrayAttributes(TypedArray ta) {
        try {
            Field field = typedArrayAttrsField;
            if (field == null) {
                field = TypedArray.class.getDeclaredField("mAttributes");
                field.setAccessible(true);
                typedArrayAttrsField = field;
            }
            return (int[]) field.get(ta);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Resources typedArrayResources(TypedArray ta) {
        try {
            Field field = typedArrayResourcesField;
            if (field == null) {
                field = TypedArray.class.getDeclaredField("mResources");
                field.setAccessible(true);
                typedArrayResourcesField = field;
            }
            return (Resources) field.get(ta);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
