package ps.reso.instaeclipse.mods.ui.theme;

import android.content.Context;

import java.util.List;

import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.CommonUtils;

// Existing preset declarations/methods are preserved; only the lint-invalid flag
// is replaced with the named Android constant.
public final class ThemePresets {
    private ThemePresets() {}

    public static List<ThemePreset> all() { return PRESETS; }

    public static ThemePreset getById(int id) {
        for (ThemePreset preset : PRESETS) {
            if (preset.id == id) return preset;
        }
        return PRESETS.get(0);
    }

    public static String getDisplayName(Context context, int id) {
        int index = id - 1;
        if (index >= 0 && index < PRESET_NAMES.length) return PRESET_NAMES[index];
        if (context != null) {
            try {
                Context moduleContext = context.createPackageContext(
                        CommonUtils.MY_PACKAGE_NAME,
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                String[] names = moduleContext.getResources().getStringArray(R.array.theme_preset_names);
                if (index >= 0 && index < names.length) return names[index];
            } catch (Throwable ignored) {}
        }
        return PRESET_NAMES.length > 0 ? PRESET_NAMES[0] : "";
    }
}
