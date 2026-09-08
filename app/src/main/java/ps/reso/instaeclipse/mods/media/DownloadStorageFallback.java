package ps.reso.instaeclipse.mods.media;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Safe fallback when the user's persisted SAF tree permission is no longer writable. */
final class DownloadStorageFallback {
    private DownloadStorageFallback() {}

    static Uri saveToDownloads(Context context, File source, String filename, String mimeType) throws Exception {
        if (source == null || !source.isFile() || source.length() <= 0) {
            throw new Exception("fallback source file is missing or empty");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InstaEclipse");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            ModuleLog.line("(IE|DL|Storage) MediaStore fallback: inserting " + filename);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore insert returned null");

            try {
                long copied = 0;
                try (FileInputStream in = new FileInputStream(source);
                     OutputStream out = context.getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new Exception("MediaStore output stream unavailable");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                        copied += n;
                    }
                    out.flush();
                }

                if (copied != source.length()) {
                    throw new Exception("MediaStore copied " + copied + " bytes; expected " + source.length());
                }

                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Downloads.IS_PENDING, 0);
                int updated = context.getContentResolver().update(uri, ready, null, null);
                if (updated != 1) throw new Exception("MediaStore publish update failed");

                ModuleLog.line("(IE|DL|Storage) ✅ Saved to Downloads/InstaEclipse: "
                        + filename + " (" + copied + " bytes) uri=" + uri);
                return uri;
            } catch (Throwable t) {
                try { context.getContentResolver().delete(uri, null, null); } catch (Throwable ignored) {}
                ModuleLog.line("(IE|DL|Storage) ❌ MediaStore fallback failed: " + t);
                throw t;
            }
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File ieDir = new File(dir, "InstaEclipse");
        if (!ieDir.exists() && !ieDir.mkdirs()) throw new Exception("Cannot create Downloads/InstaEclipse");
        File target = new File(ieDir, filename);
        try (FileInputStream in = new FileInputStream(source);
             java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            out.flush();
        }
        ModuleLog.line("(IE|DL|Storage) ✅ Saved to Downloads/InstaEclipse: " + target.getAbsolutePath());
        return Uri.fromFile(target);
    }
}
