package ps.reso.instaeclipse.ui.theme;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ps.reso.instaeclipse.R;

/** Full color editor: HSV picker, per-channel RGBA sliders, and a hex text field, all kept in sync. */
public class AdvancedColorPickerDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_COLOR = "color";

    public interface Listener {
        void onColorPicked(int color);
    }

    private AdvancedColorPickerView pickerView;
    private View previewSwatch;
    private EditText hexInput;
    private SeekBar alphaBar;
    private SeekBar redBar;
    private SeekBar greenBar;
    private SeekBar blueBar;
    private TextView rgbLabel;
    private TextView alphaLabel;
    private boolean updating;

    public static AdvancedColorPickerDialog newInstance(String title, int color) {
        AdvancedColorPickerDialog dialog = new AdvancedColorPickerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_COLOR, color);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_advanced_color_picker, (ViewGroup) null);
        int initialColor = requireArguments().getInt(ARG_COLOR, Color.WHITE);
        String title = requireArguments().getString(ARG_TITLE, getString(R.string.theme_pick_color));

        pickerView = view.findViewById(R.id.color_picker_view);
        previewSwatch = view.findViewById(R.id.color_preview_swatch);
        hexInput = view.findViewById(R.id.color_hex_input);
        alphaBar = view.findViewById(R.id.color_alpha_bar);
        redBar = view.findViewById(R.id.color_red_bar);
        greenBar = view.findViewById(R.id.color_green_bar);
        blueBar = view.findViewById(R.id.color_blue_bar);
        rgbLabel = view.findViewById(R.id.color_rgb_label);
        alphaLabel = view.findViewById(R.id.color_alpha_label);

        hexInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        alphaBar.setMax(255);
        pickerView.setColor(initialColor);
        syncUiFromColor(initialColor);
        pickerView.setOnColorChangedListener(this::syncUiFromColor);
        hexInput.addTextChangedListener(hexWatcher());
        bindAlphaBar();
        bindRgbBar(redBar, 0);
        bindRgbBar(greenBar, 1);
        bindRgbBar(blueBar, 2);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.theme_apply_color, (d, w) -> {
                    Listener l = getListener();
                    if (l != null) l.onColorPicked(pickerView.getColor());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void bindAlphaBar() {
        alphaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || updating) return;
                updating = true;
                pickerView.setAlphaChannel(progress);
                syncUiFromColor(pickerView.getColor());
                updating = false;
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void bindRgbBar(SeekBar bar, int channel) {
        bar.setMax(255);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || updating) return;
                int color = pickerView.getColor();
                int a = Color.alpha(color);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (channel == 0) r = progress;
                else if (channel == 1) g = progress;
                else b = progress;
                int next = Color.argb(a, r, g, b);
                updating = true;
                pickerView.setColor(next);
                syncUiFromColor(next);
                updating = false;
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private TextWatcher hexWatcher() {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (updating) return;
                String raw = s.toString().trim();
                if (raw.startsWith("#")) raw = raw.substring(1);
                if (raw.length() != 6 && raw.length() != 8) return;
                try {
                    int color = raw.length() == 8 ? (int) Long.parseLong(raw, 16) : Color.parseColor("#FF" + raw);
                    updating = true;
                    pickerView.setColor(color);
                    syncUiFromColor(color);
                    updating = false;
                } catch (NumberFormatException ignored) {}
            }
        };
    }

    private void syncUiFromColor(int color) {
        if (previewSwatch != null) previewSwatch.setBackgroundColor(color);
        if (rgbLabel != null) {
            rgbLabel.setText(getString(R.string.theme_rgba_format, Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)));
        }
        if (alphaLabel != null) {
            alphaLabel.setText(getString(R.string.theme_alpha_format, Math.round((Color.alpha(color) / 255.0f) * 100.0f)));
        }
        if (alphaBar != null) alphaBar.setProgress(Color.alpha(color));
        if (redBar != null) redBar.setProgress(Color.red(color));
        if (greenBar != null) greenBar.setProgress(Color.green(color));
        if (blueBar != null) blueBar.setProgress(Color.blue(color));
        if (hexInput != null && !updating) {
            updating = true;
            hexInput.setText(String.format("#%08X", color));
            hexInput.setSelection(hexInput.getText().length());
            updating = false;
        }
    }

    private Listener getListener() {
        if (getParentFragment() instanceof Listener) return (Listener) getParentFragment();
        if (requireActivity() instanceof Listener) return (Listener) requireActivity();
        return null;
    }
}
