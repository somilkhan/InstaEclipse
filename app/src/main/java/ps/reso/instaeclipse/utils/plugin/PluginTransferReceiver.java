package ps.reso.instaeclipse.utils.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

/** Bridges pending plugin packages between the companion process and injected Instagram. */
public final class PluginTransferReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (PluginManager.ACTION_REQUEST_PENDING.equals(action)) {
            PluginDownloadManager.transferPending(context);
            return;
        }
        if (PluginManager.ACTION_PLUGIN_INSTALLED.equals(action)) {
            String id = intent.getStringExtra(PluginManager.EXTRA_ID);
            String version = intent.getStringExtra(PluginManager.EXTRA_VERSION);
            if (id == null || version == null) return;
            File file = new File(context.getFilesDir(), "plugin-pending/" + id + "-" + version + ".apk");
            if (file.exists()) file.delete();
        }
    }
}
