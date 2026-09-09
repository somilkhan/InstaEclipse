package ps.reso.instaeclipse.plugin.api;

public interface PluginLogger {
    void info(String message);
    void warn(String message);
    void error(String message, Throwable throwable);
}
