package ps.reso.instaeclipse.mods.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import ps.reso.instaeclipse.mods.ui.UIHookManager;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class IgThemeHook {
    private static volatile boolean installed;
    private static volatile Field typedArrayAttrsField;
    private static volatile Field typedArrayResourcesField;
    private static volatile Field colorStateListColorsField;
    private static volatile Field colorStateListStatesField;

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
            ModuleLog.line("(InstaEclipse | Theme): hook failed: " + t);
        }
    }

    private void hookResolveAttribute(final ClassLoader cl) {
        XposedHelpers.findAndHookMethod("android.content.res.Resources$Theme", cl, "resolveAttribute",
                int.class, TypedValue.class, boolean.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (IgColorRemapEngine.isBypassing() || !FeatureFlags.customThemeEnabled) return;
                        TypedValue out = (TypedValue) p.args[1];
                        if (out == null) return;
                        if (!IgThemeEngine.isInitialized()) {
                            try {
                                Resources r = (Resources) XposedHelpers.getObjectField(p.thisObject, "mResources");
                                if (r != null) IgThemeEngine.ensureInitialized(r, cl);
                            } catch (Throwable ignored) {}
                        }
                        Integer override = IgThemeEngine.colorForAttr((Integer) p.args[0]);
                        if (override != null) {
                            IgThemeEngine.applyAttrOverride((Integer) p.args[0], out);
                            p.setResult(true);
                        } else if (IgColorRemapEngine.isReady()
                                && (out.type == TypedValue.TYPE_INT_COLOR_ARGB8 || out.type == TypedValue.TYPE_INT_COLOR_RGB8)) {
                            int remapped = IgColorRemapEngine.remap(out.data);
                            if (remapped != out.data) { out.data = remapped; p.setResult(true); }
                        }
                    }
                });
    }

    private void hookGetColor(final ClassLoader cl) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                int id = (Integer) p.args[0];
                if (IgThemeEngine.looksLikeDirectColor(id)) { p.setResult(IgColorRemapEngine.remap(id)); return; }
                if (IgThemeEngine.looksLikeResourceId(id)) {
                    if (!IgThemeEngine.isInitialized()) IgThemeEngine.ensureInitialized((Resources) p.thisObject, cl);
                    Integer override = IgThemeEngine.colorForResource(id);
                    if (override != null) p.setResult(override);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || p.getThrowable() != null || !IgColorRemapEngine.isReady()) return;
                int id = (Integer) p.args[0];
                if (!IgThemeEngine.looksLikeDirectColor(id) && IgThemeEngine.looksLikeResourceId(id)
                        && IgThemeEngine.colorForResource(id) == null) {
                    Object result = p.getResult();
                    if (result instanceof Integer) p.setResult(IgColorRemapEngine.remap((Integer) result));
                }
            }
        };
        try { XposedHelpers.findAndHookMethod(Resources.class, "getColor", int.class, Resources.Theme.class, hook); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(Resources.class, "getColor", int.class, hook); } catch (Throwable ignored) {}
    }

    private void hookContextGetColor() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                int id = (Integer) p.args[0];
                if (IgThemeEngine.looksLikeDirectColor(id)) { p.setResult(IgColorRemapEngine.remap(id)); return; }
                if (IgThemeEngine.looksLikeResourceId(id)) {
                    if (!IgThemeEngine.isInitialized()) IgThemeEngine.ensureInitialized((Context) p.thisObject);
                    Integer override = IgThemeEngine.colorForResource(id);
                    if (override != null) p.setResult(override);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()
                        || p.getThrowable() != null || !IgColorRemapEngine.isReady()) return;
                int id = (Integer) p.args[0];
                if (!IgThemeEngine.looksLikeDirectColor(id) && IgThemeEngine.looksLikeResourceId(id)
                        && IgThemeEngine.colorForResource(id) == null && p.getResult() instanceof Integer) {
                    p.setResult(IgColorRemapEngine.remap((Integer) p.getResult()));
                }
            }
        };
        try { XposedHelpers.findAndHookMethod(Context.class, "getColor", int.class, hook); } catch (Throwable ignored) {}
    }

    private void hookTypedArrayGetColor(final ClassLoader cl) {
        XposedHelpers.findAndHookMethod(TypedArray.class, "getColor", int.class, int.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                try {
                    TypedArray ta = (TypedArray) p.thisObject;
                    int index = (Integer) p.args[0];
                    int[] attrs = typedArrayAttributes(ta);
                    if (attrs == null || index < 0 || index >= attrs.length) return;
                    if (!IgThemeEngine.isInitialized()) {
                        Resources r = typedArrayResources(ta);
                        if (r != null) IgThemeEngine.ensureInitialized(r, cl);
                    }
                    Integer override = IgThemeEngine.colorForAttr(attrs[index]);
                    if (override != null) { p.setResult(override); return; }
                    if (IgColorRemapEngine.isReady() && p.getResult() instanceof Integer) {
                        p.setResult(IgColorRemapEngine.remap((Integer) p.getResult()));
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private void hookStatefulColors(final ClassLoader cl) {
        XC_MethodHook listHook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing() || p.getThrowable() != null) return;
                if (p.getResult() instanceof ColorStateList) p.setResult(remapColorStateList((ColorStateList) p.getResult()));
            }
        };
        try { XposedHelpers.findAndHookMethod(Resources.class, "getColorStateList", int.class, Resources.Theme.class, listHook); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(Resources.class, "getColorStateList", int.class, listHook); } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(Context.class, "getColorStateList", int.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing() || p.getThrowable() != null) return;
                    if (p.getResult() instanceof ColorStateList) p.setResult(remapColorStateList((ColorStateList) p.getResult()));
                }
            });
        } catch (Throwable ignored) {}
        XC_MethodHook tintHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.isBypassing()) return;
                if (p.args != null && p.args.length > 0 && p.args[0] instanceof ColorStateList) {
                    p.args[0] = remapColorStateList((ColorStateList) p.args[0]);
                }
            }
        };
        try { XposedHelpers.findAndHookMethod(ImageView.class, "setImageTintList", ColorStateList.class, tintHook); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", ColorStateList.class, tintHook); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(Drawable.class, "setTintList", ColorStateList.class, tintHook); } catch (Throwable ignored) {}
    }

    private static ColorStateList remapColorStateList(ColorStateList source) {
        if (source == null || IgColorRemapEngine.isBypassing()) return source;
        try {
            Field colorsField = colorStateListColorsField;
            Field statesField = colorStateListStatesField;
            if (colorsField == null) {
                colorsField = ColorStateList.class.getDeclaredField("mColors");
                colorsField.setAccessible(true);
                colorStateListColorsField = colorsField;
            }
            if (statesField == null) {
                statesField = ColorStateList.class.getDeclaredField("mStateSpecs");
                statesField.setAccessible(true);
                colorStateListStatesField = statesField;
            }
            int[] colors = (int[]) colorsField.get(source);
            int[][] states = (int[][]) statesField.get(source);
            if (colors == null || states == null || colors.length == 0) return source;
            int[] remapped = colors.clone();
            boolean changed = false;
            for (int i = 0; i < remapped.length; i++) {
                int value = IgColorRemapEngine.remap(remapped[i]);
                changed |= value != remapped[i];
                remapped[i] = value;
            }
            return changed ? new ColorStateList(states, remapped) : source;
        } catch (Throwable ignored) {
            return source;
        }
    }

    private void hookDirectColorMutators() {
        try { XposedHelpers.findAndHookMethod(View.class, "setBackgroundColor", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (FeatureFlags.customThemeEnabled && !IgColorRemapEngine.shouldSkipRemap(p.thisObject)) p.args[0] = IgColorRemapEngine.remap((Integer) p.args[0]);
            }
        }); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(View.class, "setBackgroundResource", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!FeatureFlags.customThemeEnabled || IgColorRemapEngine.shouldSkipRemap(p.thisObject)) return;
                Integer override = IgThemeEngine.colorForResource((Integer) p.args[0]);
                if (override != null) { p.setResult(null); ((View) p.thisObject).setBackgroundColor(override); }
            }
        }); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(TextView.class, "setTextColor", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { if (FeatureFlags.customThemeEnabled && !IgColorRemapEngine.isBypassing()) p.args[0] = IgColorRemapEngine.remap((Integer) p.args[0]); }
        }); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(Drawable.class, "setTint", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { if (FeatureFlags.customThemeEnabled && !IgColorRemapEngine.isBypassing()) p.args[0] = IgColorRemapEngine.remap((Integer) p.args[0]); }
        }); } catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(GradientDrawable.class, "setColor", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { if (FeatureFlags.customThemeEnabled && !IgColorRemapEngine.isBypassing()) p.args[0] = IgColorRemapEngine.remap((Integer) p.args[0]); }
        }); } catch (Throwable ignored) {}
    }

    private void hookActivityLifecycle() {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { refreshActivity((Activity) p.thisObject, false); }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { refreshActivity((Activity) p.thisObject, false); }
        });
    }

    private static void refreshActivity(Activity activity, boolean recreate) {
        if (!IgThemeEngine.isActive() || activity == null || activity.isFinishing()) return;
        IgThemeEngine.ensureInitialized(activity);
        IgColorRemapEngine.ensureBuilt(activity);
        applyWindowColors(activity);
        if (recreate) activity.getWindow().getDecorView().post(activity::recreate);
    }

    private void hookPhoneWindowColors(ClassLoader cl) {
        XC_MethodHook status = new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { if (IgThemeEngine.isActive()) p.args[0] = IgThemeEngine.getActivePalette().statusBar; } };
        XC_MethodHook nav = new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { if (IgThemeEngine.isActive()) p.args[0] = IgThemeEngine.getActivePalette().navigation; } };
        try { XposedHelpers.findAndHookMethod("com.android.internal.policy.PhoneWindow", cl, "setStatusBarColor", int.class, status); XposedHelpers.findAndHookMethod("com.android.internal.policy.PhoneWindow", cl, "setNavigationBarColor", int.class, nav); } catch (Throwable ignored) {}
    }

    public static void refreshCurrentActivity() {
        Activity a = UIHookManager.getCurrentActivity();
        if (a != null && !a.isFinishing()) refreshActivity(a, true);
    }

    static void applyWindowColors(Activity activity) {
        if (activity == null || !IgThemeEngine.isActive()) return;
        try {
            IgThemePalette palette = IgThemeEngine.getActivePalette();
            Window w = activity.getWindow();
            w.setStatusBarColor(palette.statusBar);
            w.setNavigationBarColor(palette.navigation);
            if (w.getDecorView() != null) w.getDecorView().setBackgroundColor(palette.background);
            if (Build.VERSION.SDK_INT >= 29) { w.setStatusBarContrastEnforced(false); w.setNavigationBarContrastEnforced(false); }
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController c = w.getInsetsController();
                if (c != null) {
                    boolean light = Color.red(palette.background) + Color.green(palette.background) + Color.blue(palette.background) > 382;
                    c.setSystemBarsAppearance(light ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static int[] typedArrayAttributes(TypedArray ta) {
        try { if (typedArrayAttrsField == null) { typedArrayAttrsField = TypedArray.class.getDeclaredField("mAttributes"); typedArrayAttrsField.setAccessible(true); } return (int[]) typedArrayAttrsField.get(ta); } catch (Throwable ignored) { return null; }
    }
    private static Resources typedArrayResources(TypedArray ta) {
        try { if (typedArrayResourcesField == null) { typedArrayResourcesField = TypedArray.class.getDeclaredField("mResources"); typedArrayResourcesField.setAccessible(true); } return (Resources) typedArrayResourcesField.get(ta); } catch (Throwable ignored) { return null; }
    }
}
