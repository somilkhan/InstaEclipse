package ps.reso.instaeclipse.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import ps.reso.instaeclipse.R;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        MaterialCardView githubCard = view.findViewById(R.id.github_card);
        MaterialCardView telegramCard = view.findViewById(R.id.telegram_card);
        TextView moduleNotWorkingDescription = view.findViewById(R.id.module_not_working_description);

        moduleNotWorkingDescription.setText(Html.fromHtml(
                getString(R.string.module_not_working_description), Html.FROM_HTML_MODE_LEGACY));
        moduleNotWorkingDescription.setMovementMethod(LinkMovementMethod.getInstance());
        moduleNotWorkingDescription.setLinkTextColor(getResources().getColor(R.color.white));

        githubCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/ReSo7200/InstaEclipse"));
            startActivity(intent);
        });

        telegramCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/InstaEclipse"));
            startActivity(intent);
        });

        return view;
    }
}
