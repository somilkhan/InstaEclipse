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

/** Safe fallback when the user's persisted SAF tree permission is no longer writable. */
final class DownloadStorageFallback {
    private DownloadStorageFallback() {}

    static Uri saveToDownloads(Context context, File source, String filename, String mimeType) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InstaEclipse");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore insert returned null");
            try {
                try (FileInputStream in = new FileInputStream(source);
                     OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new Exception("MediaStore output stream unavailable");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Downloads.IS_PENDING, 0);
                context.getContentResolver().update(uri, ready, null, null);
                return uri;
            } catch (Throwable t) {
                context.getContentResolver().delete(uri, null, null);
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
        }
        return Uri.fromFile(target);
    }
}
