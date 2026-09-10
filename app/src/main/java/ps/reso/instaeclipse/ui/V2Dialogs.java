package ps.reso.instaeclipse.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import ps.reso.instaeclipse.BuildConfig;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

public final class V2Dialogs {

    public interface OnPackageSelectedListener {
        void onPackageSelected(String packageName);
    }

    private V2Dialogs() {}

    public static void showAboutDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View zehenContact = view.findViewById(R.id.about_zehen_contact);
        if (zehenContact != null) {
            zehenContact.setOnClickListener(v -> openUrl(context, "https://t.me/Zehen0i"));
        }

        View btnTelegram = view.findViewById(R.id.about_btn_telegram);
        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(v -> openUrl(context, "https://t.me/InstaEclipsechat"));
        }

        View btnGithub = view.findViewById(R.id.about_btn_github);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> openUrl(context, "https://github.com/somilkhan/InstaEclipse"));
        }

        View btnClose = view.findViewById(R.id.about_btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public static void showUpdateDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_update, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView currentVersion = view.findViewById(R.id.update_current_version);
        if (currentVersion != null) {
            currentVersion.setText("Installed: v" + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ") — Stable");
        }

        View btnCheck = view.findViewById(R.id.update_btn_check);
        if (btnCheck != null) {
            btnCheck.setOnClickListener(v -> {
                Toast.makeText(context, "Checking for latest release...", Toast.LENGTH_SHORT).show();
                VersionCheckUtility.checkForUpdates(context);
            });
        }

        View btnDownload = view.findViewById(R.id.update_btn_download);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                dialog.dismiss();
                showApkInstallerDialog(context);
            });
        }

        dialog.show();
    }

    public static void showApkInstallerDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_apk_installer, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView checksumText = view.findViewById(R.id.apk_checksum_text);
        View btnCopy = view.findViewById(R.id.apk_btn_copy_hash);
        if (btnCopy != null && checksumText != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("SHA-256", checksumText.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "SHA-256 hash copied to clipboard", Toast.LENGTH_SHORT).show();
            });
        }

        View btnChannel = view.findViewById(R.id.apk_btn_channel);
        if (btnChannel != null) {
            btnChannel.setOnClickListener(v -> openUrl(context, "https://t.me/InstaEclipsechat"));
        }

        View btnDownload = view.findViewById(R.id.apk_btn_download);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                dialog.dismiss();
                openUrl(context, "https://github.com/somilkhan/InstaEclipse/releases/latest");
            });
        }

        dialog.show();
    }

    public static void showTargetPackageDialog(Context context, String currentPackage, OnPackageSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_target_package, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioGroup group = view.findViewById(R.id.target_package_radio_group);
        RadioButton rMain = view.findViewById(R.id.pkg_radio_main);
        RadioButton rLite = view.findViewById(R.id.pkg_radio_lite);
        RadioButton rGold = view.findViewById(R.id.pkg_radio_gold);

        if ("com.instagram.lite".equals(currentPackage) && rLite != null) {
            rLite.setChecked(true);
        } else if ("com.instagold.android".equals(currentPackage) && rGold != null) {
            rGold.setChecked(true);
        } else if (rMain != null) {
            rMain.setChecked(true);
        }

        View btnCancel = view.findViewById(R.id.pkg_btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSave = view.findViewById(R.id.pkg_btn_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String selected = "com.instagram.android";
                int checkedId = group != null ? group.getCheckedRadioButtonId() : -1;
                if (checkedId == R.id.pkg_radio_lite) {
                    selected = "com.instagram.lite";
                } else if (checkedId == R.id.pkg_radio_gold) {
                    selected = "com.instagold.android";
                }
                if (listener != null) {
                    listener.onPackageSelected(selected);
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    public static void openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(context, "Could not open URL: " + url, Toast.LENGTH_SHORT).show();
        }
    }
}
