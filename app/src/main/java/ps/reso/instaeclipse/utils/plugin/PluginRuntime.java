package ps.reso.instaeclipse.utils.plugin;

import android.content.Context;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

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
                // The companion app sends this broadcast into Instagram, so this receiver
                // must be exported. The sender targets Instagram explicitly.
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
                registered = true;
            }
            PluginManager.bootstrap(context, instagramClassLoader, instagramVersion);
        } catch (Throwable error) {
            ModuleLog.line("(InstaEclipse | Plugin): runtime start failed: " + error);
        }
    }
}
