package ps.reso.instaeclipse.utils.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Bridges pending plugin packages between the companion process and injected Instagram. */
public final class PluginTransferReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PluginManager.ACTION_REQUEST_PENDING.equals(intent.getAction())) return;
        PluginDownloadManager.transferPending(context);
    }
}
