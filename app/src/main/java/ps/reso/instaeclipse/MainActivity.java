package ps.reso.instaeclipse;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ps.reso.instaeclipse.fragments.FeaturesFragment;
import ps.reso.instaeclipse.fragments.HelpFragment;
import ps.reso.instaeclipse.fragments.HomeFragment;
import ps.reso.instaeclipse.fragments.LoggingFragment;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_REQUEST = 4101;
    private static final String PREFS = "instaeclipse_setup";
    private static final String KEY_STORAGE_PROMPTED = "storage_permission_prompted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android 15+ enforces edge-to-edge for targetSdk 35; this keeps the same
        // layout behavior on older Android releases without relying on a newer
        // WindowCompat API than the project's AndroidX Core version provides.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        VersionCheckUtility.checkForUpdates(this);
        setContentView(R.layout.activity_main);
        requestLegacyStoragePermissionOnFirstLaunch();

        Toolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayShowTitleEnabled(false);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int navHeight = v.getHeight();
            int bottomPadding = navHeight + dp(8);
            if (fragmentContainer.getPaddingBottom() != bottomPadding) {
                fragmentContainer.setPadding(dp(12), 0, dp(12), bottomPadding);
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()).commit();
        }
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (item.getItemId() == R.id.nav_features) selectedFragment = new FeaturesFragment();
            else if (item.getItemId() == R.id.nav_logs) selectedFragment = new LoggingFragment();
            else if (item.getItemId() == R.id.nav_help) selectedFragment = new HelpFragment();
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Android 10+ uses MediaStore/scoped storage and must not receive a legacy prompt. */
    private void requestLegacyStoragePermissionOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_STORAGE_PROMPTED, false)) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_STORAGE_PROMPTED, true).apply();
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }
}