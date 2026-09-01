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

import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.users.UserUtils;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Corrects username resolution where Instagram exposes both username and full_name accessors. */
public final class UsernameResolverPatch {
    private static final int USERNAME_FIELD_HASH = "username".hashCode();
    private static final int FULL_NAME_FIELD_HASH = "full_name".hashCode();
    private static final String CACHE_KEY = "UsernameGetter_v2";

    private UsernameResolverPatch() {}

    public static void install(DexKitBridge bridge, ClassLoader classLoader) {
        try {
            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod(CACHE_KEY, classLoader);
                if (cached != null) {
                    cached.setAccessible(true);
                    UserUtils.userUsernameGetter = cached;
                    ModuleLog.line("(IE|DL|Username) ✅ username getter restored from cache");
                    return;
                }
            }

            List<MethodData> username = findUserFieldGetters(bridge, USERNAME_FIELD_HASH);
            if (username.isEmpty()) {
                ModuleLog.line("(IE|DL|Username) ❌ username getter candidates not found");
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
