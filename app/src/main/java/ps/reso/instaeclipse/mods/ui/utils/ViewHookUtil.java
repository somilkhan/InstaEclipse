package ps.reso.instaeclipse.mods.ui.utils;

import ps.reso.instaeclipse.utils.log.ModuleLog;

public class ViewHookUtil {
    // LOGGER FOR DEV PURPOSES
    /*
    private static void logAllViewIds(View view, Resources res, String packageName, String indent) {
        if (view.getId() != View.NO_ID) {
            try {
                String idName = res.getResourceEntryName(view.getId());
                ModuleLog.line(indent + "View ID: " + idName + " (" + view.getClass().getSimpleName() + ")");
            } catch (Resources.NotFoundException e) {
                // Might be a generated ID or from another package
                ModuleLog.line(indent + "Unknown ID: " + view.getId() + " (" + view.getClass().getSimpleName() + ")");
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                logAllViewIds(group.getChildAt(i), res, packageName, indent + "  ");
            }
        }
    }
    */
}
