package ps.reso.instaeclipse;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

/**
 * Hosts the production Web Manager inside the APK. The native Android/Xposed
 * implementation remains in the project and the Web Manager is the primary UI.
 */
public class MainActivity extends AppCompatActivity {
    private static final String WEB_ENTRY = "file:///android_asset/web/index.html";
    private static final String PREFS = "instaeclipse_setup";
    private static final String KEY_STORAGE_PROMPTED = "storage_permission_prompted";
    private static final int STORAGE_PERMISSION_REQUEST = 4101;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        VersionCheckUtility.checkForUpdates(this);

        if (!hasEmbeddedWebManager()) {
            // Keep the native manager as a safe developer/build fallback.
            setContentView(R.layout.activity_main);
            return;
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 7, 9));
        configureWebView(webView);
        setContentView(webView);
        webView.loadUrl(WEB_ENTRY);
        requestLegacyStoragePermissionOnFirstLaunch();
    }

    private boolean hasEmbeddedWebManager() {
        try {
            return getAssets().open("web/index.html") != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No app can open this link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "InstaEclipse UI failed to load", Toast.LENGTH_LONG).show();
                }
            }
        });
        view.setWebChromeClient(new WebChromeClient());
        view.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open download", Toast.LENGTH_SHORT).show();
            }
        });
        view.addJavascriptInterface(new AndroidBridge(this), "InstaEclipseAndroid");
    }

    public static class AndroidBridge {
        private final Context context;

        AndroidBridge(Context context) {
            this.context = context.getApplicationContext();
        }

        @android.webkit.JavascriptInterface
        public String getInstalledPackages() {
            String[] supported = new String[] {
                    "com.instagram.android", "com.instagold.android", "com.instaflux.app",
                    "com.myinsta.android", "cc.honista.app", "com.instaprime.android",
                    "com.instafel.android", "com.instadm.android", "com.dfistagram.android",
                    "com.Instander.android", "com.aero.instagram", "com.instapro.android",
                    "com.instaflow.android", "com.instagram1.android", "com.instagram2.android",
                    "com.instagramclone.android", "com.instaclone.android"
            };
            JSONArray result = new JSONArray();
            PackageManager pm = context.getPackageManager();
            for (String pkg : supported) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    result.put(pkg);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            return result.toString();
        }

        @android.webkit.JavascriptInterface
        public boolean launchInstagram(String packageName) {
            try {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launch == null) return false;
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @android.webkit.JavascriptInterface
        public String getVersionName(String packageName) {
            try {
                return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void requestLegacyStoragePermissionOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return;
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_STORAGE_PROMPTED, false)) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_STORAGE_PROMPTED, true).apply();
        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }
}
