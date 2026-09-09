package ps.reso.instaeclipse.plugin.api;

import android.content.Context;

/** Runtime services exposed to downloaded plugins. */
public final class PluginContext {
    private final Context appContext;
    private final ClassLoader instagramClassLoader;
    private final String instagramVersion;
    private final PluginLogger logger;

    public PluginContext(Context appContext, ClassLoader instagramClassLoader,
                         String instagramVersion, PluginLogger logger) {
        this.appContext = appContext;
        this.instagramClassLoader = instagramClassLoader;
        this.instagramVersion = instagramVersion;
        this.logger = logger;
    }

    public Context getAppContext() {
        return appContext;
    }

    public ClassLoader getInstagramClassLoader() {
        return instagramClassLoader;
    }

    public String getInstagramVersion() {
        return instagramVersion;
    }

    public PluginLogger getLogger() {
        return logger;
    }
}
