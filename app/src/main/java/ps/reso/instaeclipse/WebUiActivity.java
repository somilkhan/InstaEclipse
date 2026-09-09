package ps.reso.instaeclipse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONObject;
import ps.reso.instaeclipse.utils.log.Logging;

public class WebUiActivity extends AppCompatActivity {
    private WebView webView;
    private static final String PREF_NAME = "instaeclipse_prefs";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // Configure dark background to avoid flicker
        webView.setBackgroundColor(0xFF090A0F);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (url != null && (url.getScheme().equals("http") || url.getScheme().equals("https"))) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");

        // Load bundled offline assets
        webView.loadUrl("file:///android_asset/www/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    public class AndroidBridge {
        private final Context context;
        private final SharedPreferences prefs;

        public AndroidBridge(Context context) {
            this.context = context;
            this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public boolean isAndroid() {
            return true;
        }

        @JavascriptInterface
        public String getVersionName() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public int getVersionCode() {
            return BuildConfig.VERSION_CODE;
        }

        @JavascriptInterface
        public String getInstalledInstagramVersion() {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo info = pm.getPackageInfo("com.instagram.android", 0);
                return info.versionName != null ? info.versionName : "Unknown";
            } catch (Exception e) {
                return "Not installed";
            }
        }

        @JavascriptInterface
        public void setPreference(String key, String value) {
            prefs.edit().putString(key, value).apply();
            Logging.i("WebUI", "Preference updated: " + key + " = " + value);
        }

        @JavascriptInterface
        public String getPreference(String key, String defaultValue) {
            return prefs.getString(key, defaultValue);
        }

        @JavascriptInterface
        public void restartInstagram() {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent("ps.reso.instaeclipse.ACTION_RESTART");
                    context.sendBroadcast(intent, "ps.reso.instaeclipse.permission.RESTART_INSTAGRAM");
                    Toast.makeText(context, "Instagram restarted & synced!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Could not restart Instagram automatically", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void openUrl(String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (Exception e) {
                Logging.e("WebUI", "Failed to open URL: " + url, e);
            }
        }

        @JavascriptInterface
        public void switchView(String mode) {
            if ("classic".equalsIgnoreCase(mode)) {
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                finish();
            }
        }
    }
}
