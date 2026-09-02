package ps.reso.instaeclipse.Xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ps.reso.instaeclipse.utils.plugin.PluginManager;

/** Receives a temporary FileProvider URI and hands it to the verified Core installer. */
public final class PluginInstallReceiver extends BroadcastReceiver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "InstaEclipse-PluginInstall");
        thread.setDaemon(true);
        return thread;
    });

    private final ClassLoader instagramClassLoader;
    private final String instagramVersion;

    public PluginInstallReceiver(ClassLoader instagramClassLoader, String instagramVersion) {
        this.instagramClassLoader = instagramClassLoader;
        this.instagramVersion = instagramVersion;
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (!PluginManager.ACTION_INSTALL_PLUGIN.equals(intent.getAction())) return;
        String uriString = intent.getStringExtra(PluginManager.EXTRA_URI);
        if (uriString == null && intent.getData() != null) uriString = intent.getData().toString();
        if (uriString == null) return;
        final Uri uri = Uri.parse(uriString);
        final String id = intent.getStringExtra(PluginManager.EXTRA_ID);
        final String version = intent.getStringExtra(PluginManager.EXTRA_VERSION);
        final String packageName = intent.getStringExtra(PluginManager.EXTRA_PACKAGE);
        final String sha256 = intent.getStringExtra(PluginManager.EXTRA_SHA256);
        final BroadcastReceiver.PendingResult pending = goAsync();
        EXECUTOR.execute(() -> {
            try {
                PluginManager.installFromUri(context.getApplicationContext(), instagramClassLoader,
                        instagramVersion, uri, id, version, packageName, sha256);
            } finally {
                pending.finish();
            }
        });
    }
}
