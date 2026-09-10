package ps.reso.instaeclipse.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ps.reso.instaeclipse.MainActivity;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.mods.devops.config.JsonExportActivity;
import ps.reso.instaeclipse.mods.devops.config.JsonImportActivity;
import ps.reso.instaeclipse.ui.V2Dialogs;
import ps.reso.instaeclipse.ui.theme.ThemeCustomizerActivity;

public class HomeFragment extends Fragment {

    private String activePackage = "com.instagram.android";
    private TextView txtInstagramVersion;
    private TextView txtTargetVariant;
    private TextView txtHookBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtInstagramVersion = view.findViewById(R.id.txt_instagram_version);
        txtTargetVariant = view.findViewById(R.id.txt_target_variant);
        txtHookBadge = view.findViewById(R.id.txt_hook_badge);

        // 1. Banner Actions
        View btnBannerDownload = view.findViewById(R.id.btn_banner_download);
        if (btnBannerDownload != null) {
            btnBannerDownload.setOnClickListener(v -> V2Dialogs.showApkInstallerDialog(requireContext()));
        }

        View btnBannerTelegram = view.findViewById(R.id.btn_banner_telegram);
        if (btnBannerTelegram != null) {
            btnBannerTelegram.setOnClickListener(v -> V2Dialogs.openUrl(requireContext(), "https://t.me/InstaEclipsechat"));
        }

        // 2. Instagram Status Actions
        View btnLaunchInstagram = view.findViewById(R.id.btn_launch_instagram);
        if (btnLaunchInstagram != null) {
            btnLaunchInstagram.setOnClickListener(v -> launchInstagram());
        }

        View btnDownloadApk = view.findViewById(R.id.btn_download_apk);
        if (btnDownloadApk != null) {
            btnDownloadApk.setOnClickListener(v -> V2Dialogs.showApkInstallerDialog(requireContext()));
        }

        View btnSwitchTarget = view.findViewById(R.id.btn_switch_target);
        if (btnSwitchTarget != null) {
            btnSwitchTarget.setOnClickListener(v -> {
                V2Dialogs.showTargetPackageDialog(requireContext(), activePackage, selectedPackage -> {
                    activePackage = selectedPackage;
                    updateTargetPackageDisplay();
                    Toast.makeText(requireContext(), "Active target switched to: " + activePackage, Toast.LENGTH_SHORT).show();
                });
            });
        }

        View btnInstagramInfo = view.findViewById(R.id.btn_instagram_info);
        if (btnInstagramInfo != null) {
            btnInstagramInfo.setOnClickListener(v -> showInstagramPackageDetails());
        }

        // 3. Leadership & Credits
        View btnFullCredits = view.findViewById(R.id.btn_full_credits);
        if (btnFullCredits != null) {
            btnFullCredits.setOnClickListener(v -> V2Dialogs.showAboutDialog(requireContext()));
        }

        View btnCopyZehen = view.findViewById(R.id.btn_copy_zehen_tg);
        if (btnCopyZehen != null) {
            btnCopyZehen.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Telegram", "@Zehen0i");
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Copied @Zehen0i to clipboard", Toast.LENGTH_SHORT).show();
            });
        }

        View btnContactZehen = view.findViewById(R.id.btn_contact_zehen);
        if (btnContactZehen != null) {
            btnContactZehen.setOnClickListener(v -> V2Dialogs.openUrl(requireContext(), "https://t.me/Zehen0i"));
        }

        View btnSomilTg = view.findViewById(R.id.btn_somil_tg);
        if (btnSomilTg != null) {
            btnSomilTg.setOnClickListener(v -> V2Dialogs.openUrl(requireContext(), "https://t.me/InstaEclipsechat"));
        }

        View btnJoinChat = view.findViewById(R.id.btn_join_telegram_chat);
        if (btnJoinChat != null) {
            btnJoinChat.setOnClickListener(v -> V2Dialogs.openUrl(requireContext(), "https://t.me/InstaEclipsechat"));
        }

        // 4. Spotlight Navigation
        View spotGhost = view.findViewById(R.id.spotlight_ghost);
        if (spotGhost != null) {
            spotGhost.setOnClickListener(v -> openCategoryInFeatures(1)); // Ghost Mode
        }

        View spotDownloader = view.findViewById(R.id.spotlight_downloader);
        if (spotDownloader != null) {
            spotDownloader.setOnClickListener(v -> openCategoryInFeatures(7)); // Downloader
        }

        View spotTheme = view.findViewById(R.id.spotlight_theme);
        if (spotTheme != null) {
            spotTheme.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), ThemeCustomizerActivity.class));
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Theme Customizer unavailable", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View spotDexKit = view.findViewById(R.id.spotlight_dexkit);
        if (spotDexKit != null) {
            spotDexKit.setOnClickListener(v -> openCategoryInFeatures(0)); // Developer Options
        }

        // 5. Quick Actions
        View actionRestart = view.findViewById(R.id.action_restart_ig);
        if (actionRestart != null) {
            actionRestart.setOnClickListener(v -> {
                try {
                    Intent restartIntent = new Intent("ps.reso.instaeclipse.ACTION_RESTART");
                    requireContext().sendBroadcast(restartIntent);
                    Toast.makeText(requireContext(), "Restart broadcast dispatched to Instagram", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Failed to send restart broadcast", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View actionBackup = view.findViewById(R.id.action_backup_settings);
        if (actionBackup != null) {
            actionBackup.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), JsonExportActivity.class));
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Failed to open Backup settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View actionRestore = view.findViewById(R.id.action_restore_settings);
        if (actionRestore != null) {
            actionRestore.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), JsonImportActivity.class));
                } catch (Throwable t) {
                    Toast.makeText(requireContext(), "Failed to open Restore settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View actionUpdates = view.findViewById(R.id.action_check_updates);
        if (actionUpdates != null) {
            actionUpdates.setOnClickListener(v -> V2Dialogs.showUpdateDialog(requireContext()));
        }

        updateTargetPackageDisplay();
        return view;
    }

    private void updateTargetPackageDisplay() {
        if (getContext() == null) return;
        PackageManager pm = requireContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(activePackage, 0);
            if (txtInstagramVersion != null) {
                txtInstagramVersion.setText(info.versionName + " (" + activePackage + ")");
            }
            if (txtTargetVariant != null) {
                txtTargetVariant.setText("Target: " + activePackage);
            }
            if (txtHookBadge != null) {
                txtHookBadge.setText("Hook Active");
                txtHookBadge.setVisibility(View.VISIBLE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (txtInstagramVersion != null) {
                txtInstagramVersion.setText("Not Installed (" + activePackage + ")");
            }
            if (txtTargetVariant != null) {
                txtTargetVariant.setText("Target: " + activePackage + " (Not Found)");
            }
            if (txtHookBadge != null) {
                txtHookBadge.setText("Inactive");
                txtHookBadge.setVisibility(View.VISIBLE);
            }
        }
    }

    private void launchInstagram() {
        PackageManager pm = requireContext().getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(activePackage);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } else {
            Toast.makeText(requireContext(), "Target package " + activePackage + " is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInstagramPackageDetails() {
        PackageManager pm = requireContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(activePackage, 0);
            String message = "Package: " + info.packageName + "\n"
                    + "Version: " + info.versionName + "\n"
                    + "Build Code: " + info.versionCode + "\n"
                    + "Target SDK: " + info.applicationInfo.targetSdkVersion;
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(requireContext(), "Target package " + activePackage + " is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCategoryInFeatures(int categoryId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFeatureCategory(categoryId);
        }
    }
}
