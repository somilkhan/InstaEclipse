package ps.reso.instaeclipse.plugin.api;

/** Stable executable-plugin contract. Keep this API backwards compatible. */
public interface InstaEclipsePlugin {
    String getId();
    String getVersion();
    void onLoad(PluginContext context) throws Throwable;
    default void onUnload() throws Throwable {}
}
