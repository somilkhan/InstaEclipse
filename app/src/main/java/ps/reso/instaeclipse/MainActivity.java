package ps.reso.instaeclipse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

        TextView toolbarVersion = findViewById(R.id.toolbar_version);
        toolbarVersion.setText("v" + BuildConfig.VERSION_NAME);
        ImageButton download = findViewById(R.id.header_download_apk_btn);
        ImageButton restart = findViewById(R.id.header_restart_btn);
        ImageButton about = findViewById(R.id.header_about_btn);
        download.setOnClickListener(v -> openUrl("https://github.com/somilkhan/InstaEclipse/releases/tag/android/v" + BuildConfig.VERSION_NAME));
        restart.setOnClickListener(v -> { v.animate().rotationBy(360f).setDuration(450).start(); recreate(); });
        about.setOnClickListener(v -> new MaterialAlertDialogBuilder(this).setTitle("InstaEclipse").setMessage("Version " + BuildConfig.VERSION_NAME + "\n\nNative Android interface for the InstaEclipse companion.\n\nZehen — Active Project Continuation Lead\nSomil Khan — Original Founder").setPositiveButton("Close", null).show());

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int navHeight = v.getHeight();
            int bottomPadding = navHeight + dp(10);
            if (fragmentContainer.getPaddingBottom() != bottomPadding) fragmentContainer.setPadding(dp(12), 0, dp(12), bottomPadding);
        });
        if (savedInstanceState == null) showFragment(new HomeFragment());
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selected = new HomeFragment();
            else if (id == R.id.nav_features) selected = new FeaturesFragment();
            else if (id == R.id.nav_logs) selected = new LoggingFragment();
            else if (id == R.id.nav_help) selected = new HelpFragment();
            if (selected != null) showFragment(selected);
            return selected != null;
        });
    }

    private void showFragment(Fragment fragment) { getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).replace(R.id.fragment_container, fragment).commit(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {} }
    private void requestLegacyStoragePermissionOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_STORAGE_PROMPTED, false)) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_STORAGE_PROMPTED, true).apply();
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }
}