package ps.reso.instaeclipse.fragments;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.log.Logging;

/** In-app viewer and diagnostic export for companion + Instagram logs. */
public class LoggingFragment extends Fragment {

    private static final long INSTAGRAM_REPLY_TIMEOUT_MS = 4000;
    private static final int MAX_DISPLAY_CHARS = 100000;
    private static final int CREATE_ZIP_REQUEST = 2401;

    private TextView contentView;
    private TextView lineCountView;
    private Runnable pendingTimeout;
    private String companionSection = "";

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger loadGeneration = new AtomicInteger(0);
    private File pendingExportZip;

    private final BroadcastReceiver logReplyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!CommonUtils.ACTION_LOGS_REPLY.equals(intent.getAction()) || contentView == null) return;
            int gen = loadGeneration.get();
            cancelInstagramTimeout();
            loadExecutor.execute(() -> {
                String instagram = formatInstagramSection(intent);
                String combined = joinSections(instagram, companionSection);
                mainHandler.post(() -> {
                    if (contentView == null || gen != loadGeneration.get()) return;
                    showCombined(combined);
                });
            });
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logging, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contentView = view.findViewById(R.id.logging_content);
        lineCountView = view.findViewById(R.id.logging_line_count);
        view.findViewById(R.id.logging_copy).setOnClickListener(v -> copyLogs());
        view.findViewById(R.id.logging_clear).setOnClickListener(v -> clearLogs());
        view.findViewById(R.id.logging_export).setOnClickListener(v -> exportLogsZip());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(CommonUtils.ACTION_LOGS_REPLY);
        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(logReplyReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), logReplyReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelInstagramTimeout();
        try { requireContext().unregisterReceiver(logReplyReceiver); } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelInstagramTimeout();
        contentView = null;
        lineCountView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loadExecutor.shutdownNow();
        if (pendingExportZip != null) {
            try { pendingExportZip.delete(); } catch (Throwable ignored) {}
            pendingExportZip = null;
        }
    }

    private void loadLogs() {
        Context ctx = getContext();
        if (ctx == null || contentView == null) return;
        int gen = loadGeneration.incrementAndGet();
        cancelInstagramTimeout();
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

    private static String joinSections(String first, String second) {
        if (second == null || second.isEmpty()) return first;
        if (first == null || first.isEmpty()) return second;
        return first + "\n\n" + second;
    }

    private String formatInstagramUnavailableSection() {
        return sectionHeader(getString(R.string.logging_instagram), null) + getString(R.string.logging_no_target);
    }

    private String formatInstagramSection(Intent intent) {
        String src = intent.getStringExtra(CommonUtils.EXTRA_LOG_SOURCE);
        String header = sectionHeader(getString(R.string.logging_instagram), src);
        String err = intent.getStringExtra(CommonUtils.EXTRA_LOG_ERROR);
        if (err != null) return header + err;
        String body = intent.getStringExtra(CommonUtils.EXTRA_LOG_TEXT);
        if (body == null || body.trim().isEmpty()) return header + getString(R.string.logging_empty_reply);
        if (body.length() > MAX_DISPLAY_CHARS) {
            body = body.substring(0, MAX_DISPLAY_CHARS) + "\n\n[Truncated - logs too large to display]";
        }
        return header + body.trim();
    }

    private static String sectionHeader(String title, String packageName) {
        String header = "=== " + title + " ===\n";
        if (packageName != null) header += "[" + packageName + "]\n";
        return header + "\n";
    }

    private void showCombined(String text) {
        if (contentView == null) return;
        contentView.setText(text);
        if (lineCountView == null) return;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') lines++;
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
        if (contentView == null) return;
        String text = contentView.getText().toString();
        if (text.isEmpty() || getString(R.string.logging_placeholder).contentEquals(text)) {
            Toast.makeText(requireContext(), R.string.logging_empty_reply, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("logs", text));
        Toast.makeText(requireContext(), R.string.logging_copied, Toast.LENGTH_SHORT).show();
    }

    /** Creates a real ZIP file off the UI thread, then lets Android's document picker save it. */
    private void exportLogsZip() {
        if (contentView == null) return;
        final String logs = contentView.getText().toString();
        if (logs.isEmpty() || getString(R.string.logging_placeholder).contentEquals(logs)) {
            Toast.makeText(requireContext(), R.string.logging_empty_reply, Toast.LENGTH_SHORT).show();
            return;
        }

        final Context context = requireContext().getApplicationContext();
        Toast.makeText(context, R.string.logging_export_preparing, Toast.LENGTH_SHORT).show();
        loadExecutor.execute(() -> {
            File zip = null;
            try {
                File dir = new File(context.getCacheDir(), "log_exports");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Unable to create export directory");
                zip = new File(dir, "instaeclipse-logs-" + timestamp() + ".zip");
                try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                    putTextEntry(out, "logs.txt", logs);
                    putTextEntry(out, "companion.log", Logging.getSnapshot());
                    putTextEntry(out, "export-info.txt", "InstaEclipse diagnostic log export\nGenerated: " + new Date() + "\n");
                }
                File result = zip;
                mainHandler.post(() -> openCreateDocument(result));
            } catch (Throwable t) {
                if (zip != null) try { zip.delete(); } catch (Throwable ignored) {}
                String error = String.valueOf(t.getMessage());
                mainHandler.post(() -> Toast.makeText(context, getString(R.string.logging_export_failed, error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private static void putTextEntry(ZipOutputStream out, String name, String text) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(text == null ? "" : text);
        writer.flush();
        out.closeEntry();
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }

    private void openCreateDocument(File zip) {
        if (!isAdded()) return;
        pendingExportZip = zip;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, zip.getName());
        try {
            startActivityForResult(intent, CREATE_ZIP_REQUEST);
        } catch (Throwable t) {
            pendingExportZip = null;
            try { zip.delete(); } catch (Throwable ignored) {}
            Toast.makeText(requireContext(), R.string.logging_export_failed_picker, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_ZIP_REQUEST) return;
        File zip = pendingExportZip;
        pendingExportZip = null;
        if (zip == null) return;
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) {
            try { zip.delete(); } catch (Throwable ignored) {}
            return;
        }
        Uri destination = data.getData();
        Context context = requireContext().getApplicationContext();
        loadExecutor.execute(() -> {
            try (OutputStream out = context.getContentResolver().openOutputStream(destination);
                 java.io.InputStream in = new java.io.FileInputStream(zip)) {
                if (out == null) throw new IllegalStateException("Unable to open destination");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                out.flush();
                try { zip.delete(); } catch (Throwable ignored) {}
                mainHandler.post(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), R.string.logging_exported, Toast.LENGTH_SHORT).show();
                });
            } catch (Throwable t) {
                String error = String.valueOf(t.getMessage());
                mainHandler.post(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), getString(R.string.logging_export_failed, error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
