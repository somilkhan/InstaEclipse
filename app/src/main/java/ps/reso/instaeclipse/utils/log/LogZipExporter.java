package ps.reso.instaeclipse.utils.log;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates and persists a shareable ZIP containing the complete logs supplied by the caller. */
public final class LogZipExporter {
    private static final String DOWNLOAD_SUBDIR = Environment.DIRECTORY_DOWNLOADS + "/InstaEclipse";

    private LogZipExporter() {}

    /**
     * Saves the ZIP to shared Downloads/InstaEclipse so it survives app updates/uninstalls and
     * is visible to the user. Android 10+ uses MediaStore.Downloads (scoped-storage safe).
     */
    public static Uri saveToDownloads(Context context, String logs) throws Exception {
        String filename = "instaeclipse-logs-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                + ".zip";
        byte[] zipBytes = buildZip(logs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_SUBDIR);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore could not create Downloads file");
            boolean success = false;
            try {
                try (OutputStream out = resolver.openOutputStream(uri, "w")) {
                    if (out == null) throw new Exception("MediaStore output stream unavailable");
                    out.write(zipBytes);
                    out.flush();
                }
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, ready, null, null);
                success = true;
                return uri;
            } finally {
                if (!success) {
                    try { resolver.delete(uri, null, null); } catch (Throwable ignored) {}
                }
            }
        }

        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(downloads, "InstaEclipse");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create Downloads/InstaEclipse");
        File file = new File(dir, filename);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(zipBytes);
            out.flush();
        }
        return Uri.fromFile(file);
    }

    /** Creates a private cache copy for callers that explicitly need to share a FileProvider URI. */
    public static Uri export(Context context, String logs) throws Exception {
        File dir = new File(context.getCacheDir(), "log-export");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create export directory");
        File zip = new File(dir, "instaeclipse-logs-" + System.currentTimeMillis() + ".zip");
        try (FileOutputStream out = new FileOutputStream(zip)) {
            out.write(buildZip(logs));
            out.flush();
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", zip);
    }

    private static byte[] buildZip(String logs) throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            out.putNextEntry(new ZipEntry("logs.txt"));
            byte[] logBytes = (logs == null ? "" : logs).getBytes(StandardCharsets.UTF_8);
            out.write(logBytes);
            out.closeEntry();
            out.putNextEntry(new ZipEntry("export-info.txt"));
            String info = "InstaEclipse log export\nGenerated: " + System.currentTimeMillis()
                    + "\nLocation: Downloads/InstaEclipse\n";
            out.write(info.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return bytes.toByteArray();
    }

    public static void share(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uri != null) intent.setClipData(android.content.ClipData.newRawUri("logs", uri));
        context.startActivity(Intent.createChooser(intent, "Share InstaEclipse logs"));
    }
}
