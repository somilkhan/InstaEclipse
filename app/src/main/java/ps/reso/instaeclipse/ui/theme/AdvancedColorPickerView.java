package ps.reso.instaeclipse.ui.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Self-contained HSV color picker: hue strip + saturation/value box + live preview swatch. */
public class AdvancedColorPickerView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private final Paint svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF svRect = new RectF();
    private final RectF hueRect = new RectF();
    private final RectF previewRect = new RectF();

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private int alpha = 255;
    private OnColorChangedListener listener;

    public AdvancedColorPickerView(Context context) {
        super(context);
        init();
    }

    public AdvancedColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(4.0f);
        cursorPaint.setColor(Color.WHITE);
        previewPaint.setStyle(Paint.Style.FILL);
        checkerPaint.setStyle(Paint.Style.FILL);
        checkerPaint.setColor(-2039584);
    }

    public void setColor(int color) {
        alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        invalidate();
    }

    public int getColor() {
        int rgb = Color.HSVToColor(new float[]{hue, saturation, value});
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    public void setAlphaChannel(int a) {
        alpha = Math.max(0, Math.min(255, a));
        notifyChanged();
        invalidate();
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = dp(12.0f);
        float previewSize = dp(48.0f);
        previewRect.set(pad, pad, pad + previewSize, pad + previewSize);
        hueRect.set(pad + previewSize + dp(16.0f), dp(8.0f) + pad, w - pad, dp(28.0f) + pad);
        svRect.set(pad, previewRect.bottom + dp(16.0f), w - pad, h - pad);
        updateShaders();
    }

    private void updateShaders() {
        if (svRect.width() <= 0.0f || hueRect.width() <= 0.0f) return;
        int hueColor = Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});
        Shader whiteGrad = new LinearGradient(svRect.left, svRect.top, svRect.right, svRect.top, Color.WHITE, hueColor, Shader.TileMode.CLAMP);
        Shader blackGrad = new LinearGradient(svRect.left, svRect.top, svRect.left, svRect.bottom, 0, Color.BLACK, Shader.TileMode.CLAMP);
        svPaint.setShader(new ComposeShader(whiteGrad, blackGrad, PorterDuff.Mode.MULTIPLY));
        int[] hueColors = {Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.BLUE, Color.YELLOW, Color.RED};
        huePaint.setShader(new LinearGradient(hueRect.left, 0.0f, hueRect.right, 0.0f, hueColors, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(previewRect, dp(12.0f), dp(12.0f), checkerPaint);
        previewPaint.setColor(getColor());
        canvas.drawRoundRect(previewRect, dp(12.0f), dp(12.0f), previewPaint);
        canvas.drawRoundRect(svRect, dp(16.0f), dp(16.0f), svPaint);
        canvas.drawRoundRect(hueRect, dp(10.0f), dp(10.0f), huePaint);
        float svX = svRect.left + (saturation * svRect.width());
        float svY = svRect.top + ((1.0f - value) * svRect.height());
        cursorPaint.setColor(Color.WHITE);
        canvas.drawCircle(svX, svY, dp(10.0f), cursorPaint);
        cursorPaint.setColor(Color.BLACK);
        canvas.drawCircle(svX, svY, dp(8.0f), cursorPaint);
        cursorPaint.setColor(Color.WHITE);
        float hueX = hueRect.left + ((hue / 360.0f) * hueRect.width());
        canvas.drawCircle(hueX, hueRect.centerY(), dp(10.0f), cursorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onTouchEvent(event);
        }
        float x = event.getX();
        float y = event.getY();
        if (hueRect.contains(x, y)) {
            hue = clamp01((x - hueRect.left) / hueRect.width()) * 360.0f;
            updateShaders();
            notifyChanged();
            invalidate();
            return true;
        }
        if (svRect.contains(x, y)) {
            saturation = clamp01((x - svRect.left) / svRect.width());
            value = 1.0f - clamp01((y - svRect.top) / svRect.height());
            notifyChanged();
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void notifyChanged() {
        if (listener != null) listener.onColorChanged(getColor());
    }

    private float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private float dp(float v) {
        return getResources().getDisplayMetrics().density * v;
    }
}
