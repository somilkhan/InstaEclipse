package ps.reso.instaeclipse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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
import ps.reso.instaeclipse.ui.V2Dialogs;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_REQUEST = 4101;
    private static final String PREFS = "instaeclipse_setup";
    private static final String KEY_STORAGE_PROMPTED = "storage_permission_prompted";

    private BottomNavigationView bottomNavigation;
    private boolean isNavigatingInternally = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        VersionCheckUtility.checkForUpdates(this);
        setContentView(R.layout.activity_main);
        requestLegacyStoragePermissionOnFirstLaunch();

        Toolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        TextView toolbarVersion = findViewById(R.id.toolbar_version_badge);
        if (toolbarVersion != null) {
            toolbarVersion.setText("v" + BuildConfig.VERSION_NAME);
        }

        ImageView actionTelegram = findViewById(R.id.action_telegram);
        if (actionTelegram != null) {
            actionTelegram.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/InstaEclipse")));
                } catch (Exception ignored) {}
            });
        }

        ImageView actionUpdate = findViewById(R.id.action_update);
        if (actionUpdate != null) {
            actionUpdate.setOnClickListener(v -> VersionCheckUtility.checkForUpdates(this));
        }

        ImageView actionAbout = findViewById(R.id.action_about);
        if (actionAbout != null) {
            actionAbout.setOnClickListener(v -> V2Dialogs.showAboutDialog(this));
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        if (bottomNavigation != null && fragmentContainer != null) {
            bottomNavigation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                int navHeight = v.getHeight();
                int bottomPadding = navHeight + dp(16);
                if (fragmentContainer.getPaddingBottom() != bottomPadding) {
                    fragmentContainer.setPadding(dp(12), 0, dp(12), bottomPadding);
                }
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                if (isNavigatingInternally) {
                    return true;
                }
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_features) {
                    selectedFragment = new FeaturesFragment();
                } else if (itemId == R.id.nav_logs) {
                    selectedFragment = new LoggingFragment();
                } else if (itemId == R.id.nav_help) {
                    selectedFragment = new HelpFragment();
                }
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            });
        }
    }

    private void selectBottomNavWithoutTriggeringListener(int navItemId) {
        if (bottomNavigation != null) {
            isNavigatingInternally = true;
            bottomNavigation.setSelectedItemId(navItemId);
            isNavigatingInternally = false;
        }
    }

    public void navigateToFeatures(int categoryId) {
        selectBottomNavWithoutTriggeringListener(R.id.nav_features);
        FeaturesFragment fragment = FeaturesFragment.newInstance(categoryId);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Compatibility entry point used by HomeFragment category shortcuts. */
    public void navigateToFeatureCategory(int categoryId) {
        navigateToFeatures(categoryId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestLegacyStoragePermissionOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_STORAGE_PROMPTED, false)) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_STORAGE_PROMPTED, true).apply();
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }
}
