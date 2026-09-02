package ps.reso.instaeclipse.fragments;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.CommonUtils;
import ps.reso.instaeclipse.utils.core.Contributor;
import ps.reso.instaeclipse.utils.feature.FeatureHub;

public class HomeFragment extends Fragment {
    private static final float SCROLL_SPEED_DP_PER_SEC = 48f;

    private MaterialButton launchInstagramButton;
    private MaterialCardView instagramStatusCard;
    private TextView instagramStatusText;
    private TextView instagramVariantText;
    private MaterialButton instagramMultiButton;
    private ImageView instagramLogo, instagramInfoIcon;
    private String activePackage;
    private List<String> installedPackages;
    private ValueAnimator contributorsAnimator;
    private ValueAnimator specialThanksAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        launchInstagramButton = view.findViewById(R.id.launch_instagram_button);
        MaterialButton downloadButton = view.findViewById(R.id.download_instagram_button);
        MaterialCardView featureHubCard = view.findViewById(R.id.feature_hub_card);

        instagramStatusCard = view.findViewById(R.id.instagram_status_card);
        instagramStatusText = view.findViewById(R.id.instagram_status_text);
        instagramVariantText = view.findViewById(R.id.instagram_variant_text);
        instagramMultiButton = view.findViewById(R.id.instagram_multi_button);
        instagramLogo = view.findViewById(R.id.instagram_logo);
        instagramInfoIcon = view.findViewById(R.id.instagram_info_icon);

        checkInstagramStatus();

        downloadButton.setOnClickListener(v -> {
            String url = "https://www.apkmirror.com/uploads/?appcategory=instagram-instagram";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        featureHubCard.setOnClickListener(v -> {
            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(70).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new DecelerateInterpolator()).start()).start();
            FeatureHub.show(this);
        });

        setupContributorsAndSpecialThanks(view);
        return view;
    }

    @Override public void onResume() { super.onResume(); resumeAnimators(); }
    @Override public void onPause() { super.onPause(); pauseAnimators(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contributorsAnimator != null) contributorsAnimator.cancel();
        if (specialThanksAnimator != null) specialThanksAnimator.cancel();
    }

    @SuppressLint("SetTextI18n")
    private void checkInstagramStatus() {
        PackageManager pm = requireContext().getPackageManager();
        installedPackages = new ArrayList<>();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try { pm.getPackageInfo(pkg, 0); installedPackages.add(pkg); }
            catch (PackageManager.NameNotFoundException ignored) { }
        }

        if (installedPackages.isEmpty()) {
            instagramStatusText.setText(getString(R.string.not_installed_instagram));
            instagramStatusText.setTypeface(null, android.graphics.Typeface.BOLD);
            instagramStatusCard.setCardBackgroundColor(getResources().getColor(R.color.dark_red));
            instagramLogo.setImageResource(R.drawable.ic_cancel);
            launchInstagramButton.setEnabled(false);
            return;
        }

        activePackage = installedPackages.contains(CommonUtils.IG_PACKAGE_NAME)
                ? CommonUtils.IG_PACKAGE_NAME : installedPackages.get(0);
        instagramStatusCard.setCardBackgroundColor(getResources().getColor(R.color.green));
        instagramLogo.setImageResource(R.drawable.ic_instagram_logo);
        instagramVariantText.setVisibility(View.VISIBLE);

        if (installedPackages.size() > 1) {
            instagramMultiButton.setVisibility(View.VISIBLE);
            instagramMultiButton.setOnClickListener(v -> showDetectedVersionsDialog(pm));
        } else {
            instagramMultiButton.setVisibility(View.GONE);
        }
        bindPackageActions(pm, activePackage);
    }

    @SuppressLint("SetTextI18n")
    private void bindPackageActions(PackageManager pm, String pkg) {
        activePackage = pkg;
        try {
            String versionName = pm.getPackageInfo(pkg, 0).versionName;
            String installedText = getString(R.string.installed_instagram_version);
            String versionText = getString(R.string.instagram_version) + ": " + versionName;
            String fullText = installedText + "\n" + versionText;
            SpannableString sp = new SpannableString(fullText);
            sp.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, installedText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sp.setSpan(new android.text.style.RelativeSizeSpan(0.85f), installedText.length() + 1, fullText.length(), 0);
            instagramStatusText.setText(sp);
        } catch (PackageManager.NameNotFoundException e) {
            instagramStatusText.setText(getString(R.string.installed_instagram_version));
        }

        instagramVariantText.setText(CommonUtils.getVariantLabel(pkg));
        instagramInfoIcon.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        launchInstagramButton.setOnClickListener(v -> {
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) startActivity(launchIntent);
            else Toast.makeText(getActivity(), getString(R.string.not_installed_instagram), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupContributorsAndSpecialThanks(View rootView) {
        HorizontalScrollView contributorsScroll = rootView.findViewById(R.id.contributors_scroll);
        HorizontalScrollView specialThanksScroll = rootView.findViewById(R.id.special_thanks_scroll);
        LinearLayout contributorsContainer = rootView.findViewById(R.id.contributors_container);
        LinearLayout specialThanksContainer = rootView.findViewById(R.id.special_thanks_container);

        List<Contributor> contributors = Arrays.asList(
                new Contributor("ReSo7200", "https://github.com/ReSo7200", "https://linkedin.com/in/abdalhaleem-altamimi", null),
                new Contributor("swakwork", "https://github.com/swakwork", null, null), new Contributor("isma3iloiso", "https://github.com/isma3iloiso", null, null),
                new Contributor("Placeholder6", "https://github.com/Placeholder6", null, null), new Contributor("frknkrc44", "https://github.com/frknkrc44", null, null),
                new Contributor("BrianML", "https://github.com/brianml31", null, "https://t.me/instamoon_channel"), new Contributor("silvzr", "https://github.com/silvzr", null, null),
                new Contributor("oct", "https://github.com/oct888", null, null), new Contributor("HalfManBear", "https://github.com/halfmanbear", null, null),
                new Contributor("ar5to", "https://github.com/ar5to", null, "https://t.me/ar5to"), new Contributor("particle-box", "https://github.com/particle-box", null, null), new Contributor("rsr", null, null, "https://t.me/rsr1337")
        );
        List<Contributor> specialThanks = Arrays.asList(
                new Contributor("xHookman", "https://github.com/xHookman", null, null), new Contributor("Bluepapilte", null, null, "https://t.me/instasmashrepo"),
                new Contributor("BdrcnAYYDIN", null, null, "https://t.me/BdrcnAYYDIN"), new Contributor("Amàzing World", null, null, null)
        );

        inflateCards(contributors, contributorsContainer); inflateCards(contributors, contributorsContainer);
        inflateCards(specialThanks, specialThanksContainer); inflateCards(specialThanks, specialThanksContainer);

        contributorsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { contributorsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this); startLoopingScroll(contributorsScroll, contributorsContainer, true); }
        });
        specialThanksContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { specialThanksContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this); startLoopingScroll(specialThanksScroll, specialThanksContainer, false); }
        });
    }

    private void startLoopingScroll(HorizontalScrollView scroll, LinearLayout container, boolean forward) {
        int contentWidth = container.getWidth() / 2;
        if (contentWidth <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        long duration = Math.max(8000L, (long) ((contentWidth / density) / SCROLL_SPEED_DP_PER_SEC * 1000));
        ValueAnimator animator = ValueAnimator.ofInt(0, contentWidth);
        animator.setDuration(duration); animator.setInterpolator(new android.view.animation.LinearInterpolator()); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> { int x = (Integer) a.getAnimatedValue(); scroll.scrollTo(forward ? x : contentWidth - x, 0); });
        if (forward) contributorsAnimator = animator; else specialThanksAnimator = animator;
        animator.start();
    }

    private void resumeAnimators() { if (contributorsAnimator != null && !contributorsAnimator.isRunning()) contributorsAnimator.start(); if (specialThanksAnimator != null && !specialThanksAnimator.isRunning()) specialThanksAnimator.start(); }
    private void pauseAnimators() { if (contributorsAnimator != null) contributorsAnimator.pause(); if (specialThanksAnimator != null) specialThanksAnimator.pause(); }

    // Existing contributor card implementation continues below in the original project.
    // This method is intentionally retained through the helper below.
    private void inflateCards(List<Contributor> contributors, LinearLayout container) {
        for (Contributor contributor : contributors) {
            TextView card = new TextView(requireContext());
            card.setText(contributor.getName());
            card.setTextColor(android.graphics.Color.WHITE);
            card.setTextSize(13);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(24, 14, 24, 14);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(getResources().getColor(R.color.dark_gray)); bg.setCornerRadius(40);
            card.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, 8, 0); container.addView(card, lp);
            if (contributor.getGithub() != null || contributor.getTelegram() != null) {
                card.setOnClickListener(v -> {
                    String url = contributor.getGithub() != null ? contributor.getGithub() : contributor.getTelegram();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                });
            }
        }
    }

    private void showDetectedVersionsDialog(PackageManager pm) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.multiple_versions_detected))
                .setItems(installedPackages.toArray(new String[0]), (dialog, which) -> bindPackageActions(pm, installedPackages.get(which)))
                .show();
    }
}
