package ps.reso.instaeclipse.mods.media;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.users.UserUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Corrects username resolution where Instagram exposes both username and full_name accessors. */
public final class UsernameResolverPatch {
    private static final int USERNAME_FIELD_HASH = "username".hashCode();
    private static final int FULL_NAME_FIELD_HASH = "full_name".hashCode();
    private static final String CACHE_KEY = "UsernameGetter_v2";
    private static volatile boolean mediaBridgeInstalled;

    private UsernameResolverPatch() {}

    public static void install(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod(CACHE_KEY, classLoader);
                if (cached != null) {
                    cached.setAccessible(true);
                    UserUtils.userUsernameGetter = cached;
                    ModuleLog.line("(IE|DL|Username) ✅ username getter restored from cache");
                    installMediaUsernameBridge(bridge, classLoader);
                    return;
                }
            }

            List<MethodData> username = findUserFieldGetters(bridge, USERNAME_FIELD_HASH);
            if (username.isEmpty()) {
                ModuleLog.line("(IE|DL|Username) ❌ username getter candidates not found");
                installMediaUsernameBridge(bridge, classLoader);
                return;
            }

            if (username.size() > 1) {
                Set<String> fullNameDescriptors = new HashSet<>();
                for (MethodData md : findUserFieldGetters(bridge, FULL_NAME_FIELD_HASH)) {
                    fullNameDescriptors.add(md.getDescriptor());
                }

                List<MethodData> pure = new ArrayList<>();
                for (MethodData md : username) {
                    if (!fullNameDescriptors.contains(md.getDescriptor())) pure.add(md);
                }
                if (!pure.isEmpty()) username = pure;
            }

            Method resolved = username.get(0).getMethodInstance(classLoader);
            resolved.setAccessible(true);
            UserUtils.userUsernameGetter = resolved;
            DexKitCache.saveMethod(CACHE_KEY, resolved);
            ModuleLog.line("(IE|DL|Username) ✅ resolved actual username getter: "
                    + resolved.getDeclaringClass().getName() + "." + resolved.getName());
        } catch (Throwable t) {
            ModuleLog.line("(IE|DL|Username) ❌ patch failed: " + t);
        }

        installMediaUsernameBridge(bridge, classLoader);
    }

    /**
     * Current Instagram 443 stores the post author directly on Media's Pando model
     * (A3M() -> User, field marker "user"). FeedVideoDownloadHook still has a legacy
     * dictionary path, so bridge its public username extraction through the verified
     * Media-level getter. This removes the repeated dict-user hierarchy failure without
     * disturbing the carousel/media dictionary resolver used by downloads.
     */
    private static void installMediaUsernameBridge(DexKitBridge bridge, ClassLoader classLoader) {
        if (mediaBridgeInstalled) return;
        synchronized (UsernameResolverPatch.class) {
            if (mediaBridgeInstalled) return;
            try {
                Class<?> mediaClass = classLoader.loadClass("com.instagram.feed.media.Media");
                Class<?> feedClass = classLoader.loadClass("ps.reso.instaeclipse.mods.media.FeedVideoDownloadHook");
                Method extract = feedClass.getDeclaredMethod("extractUsernameFromMediaObject", Object.class);
                extract.setAccessible(true);

                Method mediaUserGetter = null;
                try {
                    List<MethodData> candidates = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .declaredClass("com.instagram.feed.media.Media")
                                    .returnType("com.instagram.user.model.User")
                                    .paramCount(0)
                                    .usingEqStrings(List.of("user"))));
                    for (MethodData md : candidates) {
                        try {
                            Method m = md.getMethodInstance(classLoader);
                            if (m.getParameterCount() == 0
                                    && mediaClass.isAssignableFrom(m.getDeclaringClass())
                                    && "com.instagram.user.model.User".equals(m.getReturnType().getName())) {
                                mediaUserGetter = m;
                                break;
                            }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}

                // APK 443 ground-truth fallback: Media.A3M() is the zero-arg User getter
                // containing the exact "user" Pando field access.
                if (mediaUserGetter == null) {
                    try {
                        Method m = mediaClass.getDeclaredMethod("A3M");
                        if (m.getParameterCount() == 0
                                && "com.instagram.user.model.User".equals(m.getReturnType().getName())) {
                            mediaUserGetter = m;
                        }
                    } catch (Throwable ignored) {}
                }

                if (mediaUserGetter == null) {
                    ModuleLog.line("(IE|DL|Username) ⚠️ Media user getter unavailable; legacy resolver retained");
                    return;
                }
                mediaUserGetter.setAccessible(true);
                final Method getter = mediaUserGetter;

                XposedBridge.hookMethod(extract, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object media = param.args.length == 0 ? null : param.args[0];
                        if (media == null || !getter.getDeclaringClass().isInstance(media)) return;
                        try {
                            Object user = getter.invoke(media);
                            String username = UserUtils.callUsernameGetter(user);
                            if (username != null && !username.isEmpty()) {
                                param.setResult(username);
                            }
                        } catch (Throwable ignored) {}
                    }
                });
                mediaBridgeInstalled = true;
                ModuleLog.line("(IE|DL|Username) ✅ Media user bridge → "
                        + getter.getDeclaringClass().getName() + "." + getter.getName());
            } catch (Throwable t) {
                ModuleLog.line("(IE|DL|Username) ⚠️ Media user bridge unavailable: " + t.getMessage());
            }
        }
    }

    private static List<MethodData> findUserFieldGetters(DexKitBridge bridge, int fieldHash) {
        return bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .declaredClass("com.instagram.user.model.User")
                        .returnType("java.lang.String")
                        .paramCount(0)
                        .usingNumbers(fieldHash)));
    }
}
