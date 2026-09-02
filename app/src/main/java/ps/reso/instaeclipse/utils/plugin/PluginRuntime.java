package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import ps.reso.instaeclipse.Xposed.PluginInstallReceiver;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Installs the cross-process receiver and boots already-installed plugins. */
public final class PluginRuntime {
    private static boolean registered;

    private PluginRuntime() {}

    public static synchronized void start(Context context, ClassLoader instagramClassLoader, String instagramVersion) {
        try {
            if (!registered) {
                PluginInstallReceiver receiver = new PluginInstallReceiver(instagramClassLoader, instagramVersion);
                IntentFilter filter = new IntentFilter(PluginManager.ACTION_INSTALL_PLUGIN);
                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(receiver, filter);
                }
                registered = true;
            }
            PluginManager.bootstrap(context, instagramClassLoader, instagramVersion);
        } catch (Throwable error) {
            ModuleLog.line("(InstaEclipse | Plugin): runtime start failed: " + error);
        }
    }
}
