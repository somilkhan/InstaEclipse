package ps.reso.instaeclipse.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ps.reso.instaeclipse.BuildConfig;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.CommonUtils;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        // Setup FAQ Accordions
        setupFaq(view.findViewById(R.id.faq_item_1), view.findViewById(R.id.faq_answer_1), view.findViewById(R.id.faq_icon_1));
        setupFaq(view.findViewById(R.id.faq_item_2), view.findViewById(R.id.faq_answer_2), view.findViewById(R.id.faq_icon_2));
        setupFaq(view.findViewById(R.id.faq_item_3), view.findViewById(R.id.faq_answer_3), view.findViewById(R.id.faq_icon_3));
        setupFaq(view.findViewById(R.id.faq_item_4), view.findViewById(R.id.faq_answer_4), view.findViewById(R.id.faq_icon_4));

        // Support Links
        View btnTelegram = view.findViewById(R.id.btn_support_telegram);
        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(v -> openUrl("https://t.me/InstaEclipsechat"));
        }

        View btnZehen = view.findViewById(R.id.btn_support_zehen);
        if (btnZehen != null) {
            btnZehen.setOnClickListener(v -> openUrl("https://t.me/Zehen0i"));
        }

        View btnGithub = view.findViewById(R.id.btn_support_github);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> openUrl("https://github.com/somilkhan/InstaEclipse"));
        }

        // Diagnostics
        TextView tvAppVersion = view.findViewById(R.id.info_app_version);
        if (tvAppVersion != null) {
            tvAppVersion.setText(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        }

        TextView tvTargetIg = view.findViewById(R.id.info_target_ig);
        if (tvTargetIg != null) {
            Context ctx = getContext();
            String installedPkg = findInstagramPackage(ctx);
            if (installedPkg != null) {
                try {
                    PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(installedPkg, 0);
                    tvTargetIg.setText(installedPkg + " (" + pInfo.versionName + ")");
                } catch (Exception e) {
                    tvTargetIg.setText(installedPkg);
                }
            } else {
                tvTargetIg.setText("Not Installed");
            }
        }

        TextView tvAndroidEnv = view.findViewById(R.id.info_android_env);
        if (tvAndroidEnv != null) {
            tvAndroidEnv.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        }

        TextView tvDeviceModel = view.findViewById(R.id.info_device_model);
        if (tvDeviceModel != null) {
            tvDeviceModel.setText(Build.MANUFACTURER + " " + Build.MODEL);
        }

        return view;
    }

    private void setupFaq(View item, View answer, ImageView icon) {
        if (item == null || answer == null) return;
        item.setOnClickListener(v -> {
            boolean isVisible = answer.getVisibility() == View.VISIBLE;
            answer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            if (icon != null) {
                icon.animate().rotation(isVisible ? 0f : 90f).setDuration(200).start();
            }
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private static String findInstagramPackage(Context ctx) {
        if (ctx == null) return null;
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }
}
