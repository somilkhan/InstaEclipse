package ps.reso.instaeclipse.mods.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ps.reso.instaeclipse.mods.media.FeedVideoDownloadHook;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Adds a native-looking InstaEclipse action to Instagram profile headers. */
public final class ProfilePageToolsHook {
    private static final String BUTTON_TAG = "ie_profile_tools_button";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "InstaEclipse-ProfileTools");
        t.setDaemon(true);
        return t;
    });
    private static final WeakHashMap<Activity, Boolean> WIRED = new WeakHashMap<>();
    private static volatile boolean installed;

    private ProfilePageToolsHook() {}

    public static void install() {
        if (installed) return;
        installed = true;
        ModuleLog.line("(InstaEclipse | ProfileTools): installer ready");
    }

    /** Called from the existing UI lifecycle. */
    public static void setup(Activity activity) {
        if (!FeatureFlags.enableProfileDownload || activity == null || activity.isFinishing()) return;
        MAIN.post(() -> wireWhenReady(activity));
    }

    private static void wireWhenReady(Activity activity) {
        if (!FeatureFlags.enableProfileDownload || activity.isFinishing()) return;
        final View root;
        try { root = activity.getWindow().getDecorView(); } catch (Throwable t) { return; }
        if (root == null) return;

        InjectionTarget target = findInjectionTarget(root);
        if (target != null) {
            inject(activity, target);
            WIRED.put(activity, true);
            return;
        }

        if (!Boolean.TRUE.equals(WIRED.get(activity))) {
            root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                int attempts;
                @Override public void onGlobalLayout() {
                    if (++attempts > 18 || activity.isFinishing()) {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        return;
                    }
                    InjectionTarget t = findInjectionTarget(root);
                    if (t != null) {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        inject(activity, t);
                        WIRED.put(activity, true);
                    }
                }
            });
        }
    }

    private static void inject(Activity activity, InjectionTarget target) {
        ViewGroup parent = target.parent;
        if (parent == null || parent.findViewWithTag(BUTTON_TAG) != null) return;

        ImageButton button = new ImageButton(activity);
        button.setTag(BUTTON_TAG);
        button.setImageResource(android.R.drawable.ic_menu_manage);
        button.setBackgroundResource(android.R.drawable.list_selector_background_transparent);
        button.setContentDescription("InstaEclipse profile tools");
        button.setPadding(dp(activity, 9), dp(activity, 9), dp(activity, 9), dp(activity, 9));
        try {
            TypedValue tv = new TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
                int color = tv.resourceId != 0 ? activity.getResources().getColor(tv.resourceId, activity.getTheme()) : tv.data;
                button.setImageTintList(ColorStateList.valueOf(color));
            }
        } catch (Throwable ignored) {}

        int size = target.anchor.getMeasuredHeight() > 0 ? target.anchor.getMeasuredHeight() : dp(activity, 40);
        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (target.anchor.getLayoutParams() instanceof ViewGroup.MarginLayoutParams old) {
                lp.leftMargin = old.leftMargin;
                lp.rightMargin = old.rightMargin;
                lp.topMargin = old.topMargin;
                lp.bottomMargin = old.bottomMargin;
            }
            lp.gravity = Gravity.CENTER_VERTICAL;
            button.setLayoutParams(lp);
        } else {
            button.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        }

        button.setOnClickListener(v -> showProfileTools(activity, target.root));
        try {
            parent.addView(button, Math.min(target.indexAfterAnchor, parent.getChildCount()));
            button.bringToFront();
            ModuleLog.line("(InstaEclipse | ProfileTools): button injected");
        } catch (Throwable e) {
            ModuleLog.line("(InstaEclipse | ProfileTools): injection failed: " + e.getMessage());
        }
    }

    private static InjectionTarget findInjectionTarget(View root) {
        List<View> views = flatten(root, 650);
        View notification = null;
        View options = null;
        int screenHeight = Math.max(root.getHeight(), 1);

        for (View v : views) {
            if (!v.isShown() || v.getWidth() <= 0 || v.getHeight() <= 0) continue;
            int[] loc = new int[2];
            try { v.getLocationOnScreen(loc); } catch (Throwable ignored) { continue; }
            if (loc[1] > screenHeight * 0.45f) continue;
            String marker = normalized(description(v) + " " + resourceName(v));
            if (containsAny(marker, "notification", "notifications")) notification = betterRight(notification, v);
            if (containsAny(marker, "more options", "more_option", "overflow", "profile options", "options")) options = betterRight(options, v);
        }

        View anchor = betterRight(notification, options);
        if (anchor == null || !isProfileContext(views)) return null;
        ViewGroup parent = findActionLinearParent(anchor);
        if (parent == null) return null;
        int idx = parent.indexOfChild(anchor);
        if (idx < 0) return null;
        return new InjectionTarget(root, parent, anchor, idx + 1);
    }

    private static boolean isProfileContext(List<View> views) {
        int profileScore = 0;
        int followScore = 0;
        for (View v : views) {
            if (!v.isShown()) continue;
            String marker = normalized(description(v) + " " + resourceName(v) + " " + v.getClass().getName());
            if (containsAny(marker, "profile", "avatar", "profile picture", "user profile")) profileScore += 2;
            if (containsAny(marker, "follow", "following", "message")) followScore++;
        }
        return profileScore >= 2 || followScore >= 2;
    }

    private static ViewGroup findActionLinearParent(View anchor) {
        View current = anchor;
        for (int i = 0; i < 4 && current.getParent() instanceof ViewGroup; i++) {
            ViewGroup p = (ViewGroup) current.getParent();
            if (p instanceof LinearLayout && p.getChildCount() >= 2 && p.getChildCount() <= 12) return p;
            current = p;
        }
        return anchor.getParent() instanceof ViewGroup ? (ViewGroup) anchor.getParent() : null;
    }

    private static View betterRight(View a, View b) {
        if (a == null) return b;
        if (b == null) return a;
        return screenX(b) >= screenX(a) ? b : a;
    }

    private static int screenX(View v) {
        int[] loc = new int[2];
        try { v.getLocationOnScreen(loc); } catch (Throwable ignored) {}
        return loc[0];
    }

    private static void showProfileTools(Activity activity, View root) {
        if (activity == null || activity.isFinishing()) return;
        ProfileData data = collectProfileData(root);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));

        TextView header = new TextView(activity);
        header.setText(data.username == null || data.username.isEmpty() ? "Profile" : "@" + data.username);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(dp(activity, 4), dp(activity, 8), dp(activity, 4), dp(activity, 12));
        content.addView(header);

        addAction(content, activity, "Copy Bio", android.R.drawable.ic_menu_edit,
                data.bio != null && !data.bio.isEmpty(), () -> copy(activity, "Bio", data.bio));
        addAction(content, activity, "Profile Download", android.R.drawable.ic_menu_save,
                data.profileImageUrl != null, () -> downloadProfile(activity, data));
        addAction(content, activity, "Copy Username", android.R.drawable.ic_menu_myplaces,
                data.username != null && !data.username.isEmpty(), () -> copy(activity, "Username", data.username));
        addAction(content, activity, "Follow Back", android.R.drawable.ic_input_add,
                data.followBackView != null, () -> followBack(data.followBackView));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity).setView(content).create();
        dialog.show();
    }

    private static void addAction(LinearLayout parent, Activity activity, String title, int icon,
                                  boolean enabled, Runnable action) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        row.setMinimumHeight(dp(activity, 54));
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.42f);
        row.setBackground(roundBg(0x12000000, dp(activity, 16)));

        ImageView iv = new ImageView(activity);
        iv.setImageResource(icon);
        iv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        row.addView(iv, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));

        TextView tv = new TextView(activity);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(dp(activity, 12), 0, 0, 0);
        row.addView(tv, new LinearLayout.LayoutParams(0, dp(activity, 54), 1f));

        if (enabled) row.setOnClickListener(v -> action.run());
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(activity, 58)));
        parent.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(activity, 6)));
    }

    private static void copy(Activity activity, String label, String value) {
        if (value == null || value.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, value));
    }

    private static void followBack(View button) {
        if (button == null || !button.isShown() || !button.isEnabled()) return;
        try { button.performClick(); } catch (Throwable ignored) {}
    }

    private static void downloadProfile(Activity activity, ProfileData data) {
        if (data.profileImageUrl == null) return;
        String filename = FeedVideoDownloadHook.buildFilename(data.username, "profile", null, false);
        IO.execute(() -> {
            try {
                FeedVideoDownloadHook.downloadAndSave(activity, data.profileImageUrl, filename, false, data.username);
                ModuleLog.line("(InstaEclipse | ProfileTools): profile saved");
            } catch (Throwable e) {
                ModuleLog.line("(InstaEclipse | ProfileTools): profile download failed: " + e.getMessage());
            }
        });
    }

    private static ProfileData collectProfileData(View root) {
        ProfileData data = new ProfileData();
        List<View> views = flatten(root, 900);
        for (View v : views) {
            if (!v.isShown()) continue;
            String text = text(v);
            String desc = normalized(description(v) + " " + resourceName(v));
            if (data.followBackView == null && containsAny(desc, "follow back", "follow_back")) data.followBackView = v;
            if (v instanceof ImageView) {
                String url = extractUrl(v);
                if (url != null && (containsAny(desc, "profile", "avatar", "profile_pic") || isSquare(v))) {
                    if (data.profileImageUrl == null || containsAny(desc, "profile", "avatar")) data.profileImageUrl = url;
                }
            }
            if (text != null && !text.isEmpty()) {
                String lower = text.trim().toLowerCase(Locale.US);
                if (data.username == null && looksLikeUsername(lower) && !lower.matches("\\d+")) data.username = text.trim();
            }
        }

        Activity activity = activityFromContext(root.getContext());
        if (activity != null) {
            String title = String.valueOf(activity.getTitle()).trim();
            if (looksLikeUsername(title)) data.username = title;
        }
        data.bio = findLikelyBio(views, data.username);
        return data;
    }

    private static String findLikelyBio(List<View> views, String username) {
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        for (View v : views) {
            if (!(v instanceof TextView) || !v.isShown()) continue;
            String value = text(v);
            if (value == null) continue;
            value = value.trim();
            if (value.isEmpty() || value.length() > 800) continue;
            String lower = value.toLowerCase(Locale.US);
            if (username != null && lower.equals(username.toLowerCase(Locale.US))) continue;
            if (looksLikeCount(lower) || containsAny(lower, "follow", "message", "edit profile", "share profile", "posts", "followers", "following")) continue;
            int score = value.contains("\n") ? 5 : 0;
            if (value.length() >= 20) score += 3;
            if (containsAny(lower, "http", ".com", "www", "@")) score += 2;
            if (value.length() <= 140) score += 1;
            if (score > bestScore) { bestScore = score; best = value; }
        }
        return best;
    }

    private static boolean looksLikeCount(String s) {
        return s.matches("\\d+[kKmM]?\\s*(posts|followers|following)?") || s.matches("\\d+[.,]?\\d*[kKmM]?");
    }

    private static boolean looksLikeUsername(String s) {
        return s != null && s.length() >= 2 && s.length() <= 30 && s.matches("[a-zA-Z0-9._]+") && !s.matches("\\d+");
    }

    private static String text(View v) { return v instanceof TextView tv && tv.getText() != null ? tv.getText().toString() : null; }
    private static String description(View v) { CharSequence d = v.getContentDescription(); return d == null ? "" : d.toString(); }
    private static String resourceName(View v) { try { return v.getResources().getResourceEntryName(v.getId()); } catch (Throwable ignored) { return ""; } }
    private static String normalized(String s) { return s == null ? "" : s.toLowerCase(Locale.US).replace('-', '_'); }
    private static boolean containsAny(String s, String... needles) { if (s == null) return false; for (String n : needles) if (s.contains(n)) return true; return false; }

    private static List<View> flatten(View root, int max) {
        List<View> out = new ArrayList<>();
        ArrayDeque<View> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty() && out.size() < max) {
            View v = q.removeFirst();
            out.add(v);
            if (v instanceof ViewGroup vg) for (int i = 0; i < vg.getChildCount() && out.size() + q.size() < max; i++) q.addLast(vg.getChildAt(i));
        }
        return out;
    }

    private static boolean isSquare(View v) { return v.getWidth() > 0 && v.getHeight() > 0 && Math.abs(v.getWidth() - v.getHeight()) <= dp(v.getContext(), 10); }

    private static String extractUrl(View view) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        String direct = inspectObject(view, visited, 0);
        if (direct != null) return direct;
        try {
            Object tag = view.getTag();
            if (tag instanceof Uri) return validUrl(tag.toString());
            if (tag instanceof String) return validUrl((String) tag);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String inspectObject(Object object, Set<Object> visited, int depth) {
        if (object == null || depth > 2 || visited.contains(object)) return null;
        visited.add(object);
        if (object instanceof String) return validUrl((String) object);
        if (object instanceof Uri) return validUrl(object.toString());
        Class<?> cls = object.getClass();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    String n = f.getName().toLowerCase(Locale.US);
                    if (!(n.contains("url") || n.contains("uri") || n.contains("image") || n.contains("avatar") || n.equals("a0e") || n.equals("a0d") || n.equals("a0c"))) continue;
                    f.setAccessible(true);
                    String u = inspectValue(f.get(object), visited, depth);
                    if (u != null) return u;
                } catch (Throwable ignored) {}
            }
        }
        for (String name : new String[]{"getUrl", "getImageUrl", "getUri", "getImageUri"}) {
            try {
                Method m = cls.getMethod(name);
                if (m.getParameterCount() == 0) {
                    String u = inspectValue(m.invoke(object), visited, depth);
                    if (u != null) return u;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String inspectValue(Object value, Set<Object> visited, int depth) {
        if (value instanceof String s) return validUrl(s);
        if (value instanceof Uri u) return validUrl(u.toString());
        return inspectObject(value, visited, depth + 1);
    }

    private static String validUrl(String s) {
        if (s == null || !(s.startsWith("http://") || s.startsWith("https://"))) return null;
        return isInstagramImageUrl(s) ? s : null;
    }

    private static boolean isInstagramImageUrl(String s) {
        try {
            String host = new URL(s).getHost().toLowerCase(Locale.US);
            return host.equals("instagram.com") || host.endsWith(".instagram.com")
                    || host.equals("cdninstagram.com") || host.endsWith(".cdninstagram.com")
                    || host.equals("fbcdn.net") || host.endsWith(".fbcdn.net")
                    || host.equals("fbsbx.com") || host.endsWith(".fbsbx.com");
        } catch (Throwable ignored) { return false; }
    }

    private static Activity activityFromContext(Context c) {
        Context current = c;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity a) return a;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static GradientDrawable roundBg(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private static int dp(Context c, int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()); }

    private static final class InjectionTarget {
        final View root; final ViewGroup parent; final View anchor; final int indexAfterAnchor;
        InjectionTarget(View root, ViewGroup parent, View anchor, int indexAfterAnchor) { this.root = root; this.parent = parent; this.anchor = anchor; this.indexAfterAnchor = indexAfterAnchor; }
    }

    private static final class ProfileData {
        String username;
        String bio;
        String profileImageUrl;
        View followBackView;
    }
}
