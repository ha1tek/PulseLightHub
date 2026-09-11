package com.antigravity.pulselight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ModernSwitch extends View {

    private boolean isChecked = false;
    private float progress = 0.0f; // 0.0 (OFF) -> 1.0 (ON)
    private ValueAnimator animator;

    // Paints
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint trackBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final RectF trackRect = new RectF();
    private final float density;

    // Colors
    private static final int COLOR_OFF_TRACK = 0xFF22242D;
    private static final int COLOR_OFF_BORDER = 0xFF353846;
    private static final int DEFAULT_ON_TRACK = 0xFFCCFF00;
    private int mOnColor = DEFAULT_ON_TRACK;
    private static final int COLOR_THUMB = 0xFFFFFFFF;
    private static final int COLOR_SHADOW = 0x33000000;

    private final ThemeManager.OnThemeChangeListener mThemeListener = (bgColor, accentColor) -> {
        mOnColor = accentColor;
        postInvalidate();
    };

    public interface OnCheckedChangeListener {
        void onCheckedChanged(ModernSwitch view, boolean isChecked);
    }

    private OnCheckedChangeListener listener;

    public ModernSwitch(Context context) {
        this(context, null);
    }

    public ModernSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ModernSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        mOnColor = ThemeManager.getAccentColor(context);

        trackPaint.setStyle(Paint.Style.FILL);
        trackBorderPaint.setStyle(Paint.Style.STROKE);
        trackBorderPaint.setStrokeWidth(1.2f * density);
        trackBorderPaint.setColor(COLOR_OFF_BORDER);

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(COLOR_THUMB);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(COLOR_SHADOW);

        setClickable(true);
        setFocusable(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOnColor = ThemeManager.getAccentColor(getContext());
        ThemeManager.addListener(mThemeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ThemeManager.removeListener(mThemeListener);
    }

    public void setAccentColor(int color) {
        this.mOnColor = color;
        invalidate();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked == checked && progress == (checked ? 1.0f : 0.0f)) {
            return;
        }
        this.isChecked = checked;

        if (animate && isAttachedToWindow()) {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            float target = checked ? 1.0f : 0.0f;
            animator = ValueAnimator.ofFloat(progress, target);
            animator.setDuration(200);
            animator.setInterpolator(new DecelerateInterpolator(1.8f));
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            progress = checked ? 1.0f : 0.0f;
            invalidate();
        }
    }

    public void toggle() {
        toggle(true);
    }

    public void toggle(boolean animate) {
        setChecked(!isChecked, animate);
        if (listener != null) {
            listener.onCheckedChanged(this, isChecked);
        }
    }

    @Override
    public boolean performClick() {
        toggle(true);
        return super.performClick();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(50 * density);
        int desiredHeight = Math.round(28 * density);

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float radius = h / 2f;
        trackRect.set(0, 0, w, h);

        // 1. Draw Track
        int trackColor = blendColors(COLOR_OFF_TRACK, mOnColor, progress);
        trackPaint.setColor(trackColor);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);



        // 3. Thumb Geometry
        float padding = 3.0f * density;
        float thumbDiameter = h - (padding * 2f);
        float thumbRadius = thumbDiameter / 2f;

        float startX = padding + thumbRadius;
        float endX = w - padding - thumbRadius;
        float currentThumbCenterX = startX + (endX - startX) * progress;
        float centerY = h / 2f;

        // 4. Subtle Drop Shadow for physical depth
        float shadowOffsetY = 1.0f * density;
        canvas.drawCircle(currentThumbCenterX, centerY + shadowOffsetY, thumbRadius, shadowPaint);

        // 5. Crisp Pure White Thumb
        canvas.drawCircle(currentThumbCenterX, centerY, thumbRadius, thumbPaint);
    }

    private static int blendColors(int from, int to, float ratio) {
        float inverse = 1.0f - ratio;
        float a = Color.alpha(from) * inverse + Color.alpha(to) * ratio;
        float r = Color.red(from) * inverse + Color.red(to) * ratio;
        float g = Color.green(from) * inverse + Color.green(to) * ratio;
        float b = Color.blue(from) * inverse + Color.blue(to) * ratio;
        return Color.argb(Math.round(a), Math.round(r), Math.round(g), Math.round(b));
    }
}
