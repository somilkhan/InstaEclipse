package ps.reso.instaeclipse.utils.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Version-scoped cache for resolved Instagram methods.
 *
 * Cached reflection objects are never trusted blindly: callers should use
 * loadValidatedMethod/loadValidatedMethods and provide a cheap structural
 * predicate before installing a hook. A failed validation invalidates only
 * that feature's cached entry instead of destroying the complete cache.
 */
public final class DexKitCache {
    private static final String PREF_NAME = "instaeclipse_dexkit_cache";
    private static final String KEY_VER = "_v";
    private static final char SEP = '\u0000';

    private static SharedPreferences prefs;
    private static volatile boolean cacheValid;

    private DexKitCache() {}

    public static synchronized void init(Context context, String igVersion) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_VER, "");
        if (stored.equals(igVersion)) {
            cacheValid = true;
            ModuleLog.line("(DexKitCache) Cache valid for IG " + igVersion);
        } else {
            cacheValid = false;
            prefs.edit().clear().putString(KEY_VER, igVersion).apply();
            ModuleLog.line("(DexKitCache) Version " + stored + " → " + igVersion + ", cache cleared");
        }
    }

    public static boolean isCacheValid() { return cacheValid; }

    public static synchronized void clearCache() {
        if (prefs == null) return;
        prefs.edit().clear().apply();
        cacheValid = false;
        ModuleLog.line("(DexKitCache) Cache manually cleared");
    }

    public static void saveMethod(String key, Method method) {
        if (prefs == null || method == null) return;
        prefs.edit().putString("m_" + key, encode(method)).apply();
    }

    public static Method loadMethod(String key, ClassLoader loader) {
        if (prefs == null || !cacheValid) return null;
        String encoded = prefs.getString("m_" + key, null);
        return encoded == null ? null : decode(encoded, loader);
    }

    /**
     * Loads a cached method only when the caller's structural validation passes.
     * Invalid entries are removed immediately so future launches rediscover them.
     */
    public static Method loadValidatedMethod(String key, ClassLoader loader, MethodValidator validator) {
        Method method = loadMethod(key, loader);
        if (method == null) return null;
        try {
            if (validator != null && !validator.isValid(method)) {
                invalidateMethod(key);
                return null;
            }
            return method;
        } catch (Throwable t) {
            invalidateMethod(key);
            ModuleLog.line("(DexKitCache) Validation failed for " + key + ": " + t);
            return null;
        }
    }

    public static void saveMethods(String key, List<Method> methods) {
        if (prefs == null || methods == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("mc_" + key, methods.size());
        for (int i = 0; i < methods.size(); i++) {
            editor.putString("m_" + key + "_" + i, encode(methods.get(i)));
        }
        editor.apply();
    }

    public static List<Method> loadMethods(String key, ClassLoader loader) {
        if (prefs == null || !cacheValid) return null;
        int count = prefs.getInt("mc_" + key, -1);
        if (count < 0) return null;
        List<Method> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String encoded = prefs.getString("m_" + key + "_" + i, null);
            if (encoded == null) return null;
            Method method = decode(encoded, loader);
            if (method == null) return null;
            result.add(method);
        }
        return result;
    }

    public static List<Method> loadValidatedMethods(String key, ClassLoader loader, MethodValidator validator) {
        List<Method> methods = loadMethods(key, loader);
        if (methods == null) return null;
        try {
            if (validator != null) {
                for (Method method : methods) {
                    if (!validator.isValid(method)) {
                        invalidateMethods(key);
                        return null;
                    }
                }
            }
            return methods;
        } catch (Throwable t) {
            invalidateMethods(key);
            ModuleLog.line("(DexKitCache) Validation failed for " + key + ": " + t);
            return null;
        }
    }

    public static void invalidateMethod(String key) {
        if (prefs == null) return;
        prefs.edit().remove("m_" + key).apply();
        ModuleLog.line("(DexKitCache) Invalidated method cache: " + key);
    }

    public static void invalidateMethods(String key) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit().remove("mc_" + key);
        int count = prefs.getInt("mc_" + key, 0);
        for (int i = 0; i < count; i++) editor.remove("m_" + key + "_" + i);
        editor.apply();
        ModuleLog.line("(DexKitCache) Invalidated method-list cache: " + key);
    }

    public static void saveString(String key, String value) {
        if (prefs == null) return;
        prefs.edit().putString("s_" + key, value).apply();
    }

    public static String loadString(String key) {
        if (prefs == null || !cacheValid) return null;
        return prefs.getString("s_" + key, null);
    }

    private static String encode(Method method) {
        return method.getDeclaringClass().getName() + SEP + method.getName() + SEP + descriptor(method);
    }

    private static Method decode(String encoded, ClassLoader loader) {
        try {
            int first = encoded.indexOf(SEP);
            int second = encoded.indexOf(SEP, first + 1);
            if (first < 0 || second < 0) return null;
            String className = encoded.substring(0, first);
            String methodName = encoded.substring(first + 1, second);
            String desc = encoded.substring(second + 1);
            Class<?> clazz = Class.forName(className, false, loader);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && descriptor(method).equals(desc)) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String descriptor(Method method) {
        StringBuilder result = new StringBuilder("(");
        for (Class<?> parameter : method.getParameterTypes()) typeDesc(result, parameter);
        result.append(')');
        typeDesc(result, method.getReturnType());
        return result.toString();
    }

    private static void typeDesc(StringBuilder result, Class<?> type) {
        while (type.isArray()) { result.append('['); type = type.getComponentType(); }
        if (type.isPrimitive()) {
            if (type == void.class) result.append('V');
            else if (type == boolean.class) result.append('Z');
            else if (type == byte.class) result.append('B');
            else if (type == char.class) result.append('C');
            else if (type == short.class) result.append('S');
            else if (type == int.class) result.append('I');
            else if (type == long.class) result.append('J');
            else if (type == float.class) result.append('F');
            else if (type == double.class) result.append('D');
        } else {
            result.append('L').append(type.getName().replace('.', '/')).append(';');
        }
    }

    public interface MethodValidator {
        boolean isValid(Method method) throws Throwable;
    }
}
