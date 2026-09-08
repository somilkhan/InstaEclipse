package ps.reso.instaeclipse.utils.feature;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.Map;

import ps.reso.instaeclipse.R;

/** Built-in feature overview. New features ship with the InstaEclipse Core APK. */
public final class FeatureHub {
    private FeatureHub() {}

    public static void show(Fragment fragment) {
        Context context = fragment.requireContext();
        FeatureManager.refreshFeatureStatus();

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 28));
        root.setBackground(roundBackground(context, R.color.dark_gray, 24, R.color.gray));

        TextView title = text(context, "Features", 22, true);
        root.addView(title);

        TextView subtitle = text(context,
                "All features are built into InstaEclipse and updated with the Core APK.", 14, false);
        subtitle.setTextColor(ContextCompat.getColor(context, R.color.bottom_nav_icon));
        root.addView(subtitle, margins(context, 0, 4, 0, 16));

        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)));

        for (Map.Entry<String, Boolean> entry : FeatureStatusTracker.getStatus().entrySet()) {
            String label = FeatureStatusTracker.getLabel(context, entry.getKey());
            root.addView(featureRow(context, label, entry.getValue()), margins(context, 0, 10, 0, 0));
        }

        dialog.setContentView(root);
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
        });
        dialog.show();
    }

    private static MaterialCardView featureRow(Context context, String label, boolean hooked) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.black));
        card.setRadius(dp(context, 16));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(ContextCompat.getColor(context, R.color.gray));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));

        TextView name = text(context, label, 14, true);
        row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView status = text(context, hooked ? "Active" : "Enabled", 12, false);
        status.setTextColor(ContextCompat.getColor(context, hooked
                ? R.color.white : R.color.bottom_nav_icon));
        row.addView(status);
        card.addView(row);
        return card;
    }

    private static TextView text(Context context, String value, float size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(context, R.color.white));
        return view;
    }

    private static LinearLayout.LayoutParams margins(Context context, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        return params;
    }

    private static GradientDrawable roundBackground(Context context, int fillColor, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ContextCompat.getColor(context, fillColor));
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), ContextCompat.getColor(context, strokeColor));
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
