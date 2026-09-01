package ps.reso.instaeclipse.utils.log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates a shareable ZIP containing the logs currently shown in the log viewer. */
public final class LogZipExporter {
    private LogZipExporter() {}

    public static Uri export(Context context, String logs) throws Exception {
        File dir = new File(context.getCacheDir(), "log-export");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create export directory");
        File zip = new File(dir, "instaeclipse-logs-" + System.currentTimeMillis() + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("logs.txt"));
            byte[] bytes = (logs == null ? "" : logs).getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.closeEntry();
            out.putNextEntry(new ZipEntry("export-info.txt"));
            String info = "InstaEclipse log export\nGenerated: " + System.currentTimeMillis() + "\n";
            out.write(info.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", zip);
    }

    public static void share(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Export InstaEclipse logs"));
    }
}
