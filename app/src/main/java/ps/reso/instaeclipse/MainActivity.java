package ps.reso.instaeclipse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_REQUEST = 4101;
    private static final String PREFS = "instaeclipse_setup";
    private static final String KEY_STORAGE_PROMPTED = "storage_permission_prompted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        VersionCheckUtility.checkForUpdates(this);
        requestLegacyStoragePermissionOnFirstLaunch();

        // The React/Web Manager is the current primary UI. Keep MainActivity as the
        // launcher entry point so existing installs retain their component identity,
        // then hand off immediately to the modern interface.
        startActivity(new Intent(this, WebUiActivity.class));
        finish();
    }

    private void requestLegacyStoragePermissionOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_STORAGE_PROMPTED, false)) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_STORAGE_PROMPTED, true).apply();
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }
}
