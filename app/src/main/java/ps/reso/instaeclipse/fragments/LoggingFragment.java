package ps.reso.instaeclipse.fragments;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.LogZipExporter;
import ps.reso.instaeclipse.utils.log.Logging;

public class LoggingFragment extends Fragment {
    private static final long INSTAGRAM_REPLY_TIMEOUT_MS = 4000;
    // TextView rendering is intentionally kept small. Full logs remain available to Copy/Export.
    private static final int MAX_DISPLAY_CHARS = 40000;
    private TextView contentView;
    private TextView lineCountView;
    private Runnable pendingTimeout;
    private String companionSection = "";
    private String fullLogs = "";
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    private final BroadcastReceiver logReplyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!CommonUtils.ACTION_LOGS_REPLY.equals(intent.getAction()) || contentView == null) return;
            int gen = loadGeneration.get();
            cancelInstagramTimeout();
            loadExecutor.execute(() -> {
                String instagram = formatInstagramSection(intent);
                String combined = joinSections(instagram, companionSection);
                mainHandler.post(() -> {
                    if (contentView != null && gen == loadGeneration.get()) showCombined(combined);
                });
            });
        }
    };

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                  @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logging, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        contentView = view.findViewById(R.id.logging_content);
        lineCountView = view.findViewById(R.id.logging_line_count);
        TextView export = view.findViewById(R.id.logging_export);
        export.setText("Export ZIP");
        export.setContentDescription("Save logs as ZIP");
        export.setOnClickListener(v -> exportLogs());
        view.findViewById(R.id.logging_copy).setOnClickListener(v -> copyLogs());
        view.findViewById(R.id.logging_clear).setOnClickListener(v -> clearLogs());
    }

    @Override public void onResume() {
        super.onResume();
        loadLogs();
    }

    @Override public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(CommonUtils.ACTION_LOGS_REPLY);
        if (Build.VERSION.SDK_INT >= 33) requireContext().registerReceiver(logReplyReceiver, f, Context.RECEIVER_EXPORTED);
        else ContextCompat.registerReceiver(requireContext(), logReplyReceiver, f, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override public void onStop() {
        super.onStop();
        cancelInstagramTimeout();
        try { requireContext().unregisterReceiver(logReplyReceiver); } catch (Throwable ignored) {}
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        cancelInstagramTimeout();
        contentView = null;
        lineCountView = null;
        fullLogs = "";
    }

    @Override public void onDestroy() {
        super.onDestroy();
        loadExecutor.shutdownNow();
    }

    private void loadLogs() {
        Context ctx = getContext();
        if (ctx == null || contentView == null) return;
        int gen = loadGeneration.incrementAndGet();
        cancelInstagramTimeout();
        fullLogs = "";
        contentView.setText(R.string.logging_placeholder);
        loadExecutor.execute(() -> {
            String companion = formatCompanionSection(ctx);
            String pkg = findInstagramPackage(ctx);
            mainHandler.post(() -> {
                if (contentView == null || gen != loadGeneration.get()) return;
                companionSection = companion;
                if (pkg == null) {
                    showCombined(joinSections(formatInstagramUnavailableSection(), companionSection));
                    return;
                }
                Intent request = new Intent(CommonUtils.ACTION_REQUEST_LOGS);
                request.setPackage(pkg);
                ctx.sendBroadcast(request);
                scheduleInstagramTimeout(gen);
            });
        });
    }

    private void scheduleInstagramTimeout(int gen) {
        pendingTimeout = () -> {
            if (contentView == null || gen != loadGeneration.get()) return;
            String ig = sectionHeader(getString(R.string.logging_instagram), null)
                    + getString(R.string.logging_instagram_timeout) + "\n";
            showCombined(joinSections(ig, companionSection));
        };
        mainHandler.postDelayed(pendingTimeout, INSTAGRAM_REPLY_TIMEOUT_MS);
    }

    private void cancelInstagramTimeout() {
        if (pendingTimeout != null) {
            mainHandler.removeCallbacks(pendingTimeout);
            pendingTimeout = null;
        }
    }

    private String formatCompanionSection(Context ctx) {
        String snap = Logging.getSnapshot().trim();
        if (snap.isEmpty()) return "";
        return sectionHeader(getString(R.string.logging_companion), ctx.getPackageName()) + snap;
    }

    private static String joinSections(String a, String b) {
        if (b == null || b.isEmpty()) return a;
        if (a == null || a.isEmpty()) return b;
        return a + "\n\n" + b;
    }

    private String formatInstagramUnavailableSection() {
        return sectionHeader(getString(R.string.logging_instagram), null) + getString(R.string.logging_no_target);
    }

    /** Keep the full IPC reply for copy/export; only the on-screen view is capped. */
    private String formatInstagramSection(Intent intent) {
        String src = intent.getStringExtra(CommonUtils.EXTRA_LOG_SOURCE);
        String header = sectionHeader(getString(R.string.logging_instagram), src);
        String err = intent.getStringExtra(CommonUtils.EXTRA_LOG_ERROR);
        if (err != null) return header + err;
        String body = intent.getStringExtra(CommonUtils.EXTRA_LOG_TEXT);
        if (body == null || body.trim().isEmpty()) return header + getString(R.string.logging_empty_reply);
        return header + body.trim();
    }

    private static String sectionHeader(String title, String pkg) {
        String h = "=== " + title + " ===\n";
        if (pkg != null) h += "[" + pkg + "]\n";
        return h + "\n";
    }

    private void showCombined(String text) {
        if (contentView == null) return;
        fullLogs = text == null ? "" : text;
        String display = fullLogs;
        if (display.length() > MAX_DISPLAY_CHARS) {
            // Show the newest entries; older entries are still preserved in fullLogs for Copy/Export.
            display = "[Showing latest logs — full logs are retained for Copy/Export]\n\n"
                    + display.substring(display.length() - MAX_DISPLAY_CHARS);
        }
        contentView.setText(display);
        if (lineCountView == null) return;
        int lines = 1;
        for (int i = 0; i < fullLogs.length(); i++) if (fullLogs.charAt(i) == '\n') lines++;
        lineCountView.setVisibility(View.VISIBLE);
        lineCountView.setText(getString(R.string.logging_lines_format, lines));
    }

    private static String findInstagramPackage(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try { pm.getPackageInfo(pkg, 0); return pkg; }
            catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    private void clearLogs() {
        Context ctx = getContext();
        if (ctx == null) return;
        String pkg = findInstagramPackage(ctx);
        if (pkg != null) {
            Intent clear = new Intent(CommonUtils.ACTION_CLEAR_LOGS);
            clear.setPackage(pkg);
            ctx.sendBroadcast(clear);
        }
        Logging.clear();
        loadLogs();
    }

    private void copyLogs() {
        String text = fullLogs;
        if (text == null || text.isEmpty()) {
            Toast.makeText(requireContext(), R.string.logging_empty_reply, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cb = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb == null) return;
        cb.setPrimaryClip(ClipData.newPlainText("InstaEclipse logs", text));
        Toast.makeText(requireContext(), R.string.logging_copied, Toast.LENGTH_SHORT).show();
    }

    private void exportLogs() {
        String text = fullLogs;
        if (text == null || text.isEmpty()) {
            Toast.makeText(requireContext(), R.string.logging_empty_reply, Toast.LENGTH_SHORT).show();
            return;
        }
        Context ctx = requireContext();
        loadExecutor.execute(() -> {
            try {
                android.net.Uri uri = LogZipExporter.saveToDownloads(ctx, text);
                mainHandler.post(() -> showExportSavedDialog(ctx, uri));
            } catch (Throwable t) {
                mainHandler.post(() -> Toast.makeText(ctx, "ZIP export failed: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showExportSavedDialog(Context ctx, android.net.Uri uri) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Logs exported")
                .setMessage("Saved to Downloads/InstaEclipse")
                .setNegativeButton("Close", null)
                .setPositiveButton("Share", (dialog, which) -> {
                    try { LogZipExporter.share(ctx, uri); }
                    catch (Throwable t) { Toast.makeText(ctx, "Share failed: " + t.getMessage(), Toast.LENGTH_LONG).show(); }
                })
                .show();
    }
}
