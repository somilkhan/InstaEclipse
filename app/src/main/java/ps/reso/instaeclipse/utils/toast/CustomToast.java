package ps.reso.instaeclipse.utils.toast;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class CustomToast {

    public static boolean toastShown = false;

    public static void showCustomToast(Context context, String message) {
        if (context == null) {
            ModuleLog.line("❌ CustomToast: Context is null!");
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Context safeContext = new android.view.ContextThemeWrapper(context, android.R.style.Theme_Material_Light);
                TextView toastText = new TextView(safeContext);
                toastText.setText(message);
                toastText.setTextColor(Color.WHITE);
                toastText.setBackgroundColor(Color.parseColor("#CC000000")); // semi-transparent black
                toastText.setPadding(40, 25, 40, 25);
                toastText.setTextSize(16);
                toastText.setGravity(Gravity.CENTER);

                android.widget.Toast toast = new android.widget.Toast(context);
                toast.setView(toastText);
                toast.setDuration(android.widget.Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 150);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, 1500);

            } catch (Throwable t) {
                ModuleLog.line("❌ Failed to show custom toast: " + Log.getStackTraceString(t));
            }
        });
    }

}
