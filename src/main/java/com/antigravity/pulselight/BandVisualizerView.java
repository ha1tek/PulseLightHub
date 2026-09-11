package com.antigravity.pulselight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BandVisualizerView extends View {

    private final float[] mLevels = new float[4];
    private final float[] mTargetLevels = new float[4];
    private final String[] mLabels = new String[]{"SUB", "KICK", "SNARE", "TREBLE"};

    private Paint mBarPaint;
    private Paint mBgBarPaint;
    private Paint mTextPaint;
    private RectF mBarRect = new RectF();
    private int mBarColor = 0xFFCCFF00;

    public BandVisualizerView(Context context) {
        super(context);
        init();
    }

    public BandVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BandVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBarColor = ThemeManager.getAccentColor(getContext());
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(mBarColor);

        mBgBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgBarPaint.setStyle(Paint.Style.FILL);
        mBgBarPaint.setColor(Color.parseColor("#1B1E26"));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.parseColor("#8E909A"));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setLetterSpacing(0.08f);
    }

    public void setBarColor(int color) {
        if (mBarPaint != null && color != 0) {
            mBarColor = color;
            mBarPaint.setColor(color);
            postInvalidateOnAnimation();
        }
    }

    public void setLevels(float[] levels) {
        if (levels == null || levels.length < 4) return;
        for (int i = 0; i < 4; i++) {
            mTargetLevels[i] = Math.max(0f, Math.min(1f, levels[i]));
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float labelHeight = 28f;
        float barMaxHeight = h - labelHeight - 16f;

        float totalBarSpace = w * 0.85f;
        float barWidth = (totalBarSpace / 4f) * 0.70f;
        float barSpacing = totalBarSpace / 4f;
        float startX = (w - totalBarSpace) / 2f + (barSpacing - barWidth) / 2f;

        mTextPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);

        boolean needRedraw = false;

        for (int i = 0; i < 4; i++) {
            // Smooth lerp for visual bars
            float diff = mTargetLevels[i] - mLevels[i];
            if (Math.abs(diff) > 0.01f) {
                mLevels[i] += diff * (diff > 0 ? 0.45f : 0.20f);
                needRedraw = true;
            } else {
                mLevels[i] = mTargetLevels[i];
            }

            float cx = startX + i * barSpacing + barWidth / 2f;
            float barLeft = cx - barWidth / 2f;
            float barRight = cx + barWidth / 2f;
            float barBottom = h - labelHeight - 8f;
            float barTop = barBottom - barMaxHeight;

            // Background slot
            mBarRect.set(barLeft, barTop, barRight, barBottom);
            canvas.drawRoundRect(mBarRect, 8f, 8f, mBgBarPaint);

            // Active bar
            float activeHeight = Math.max(4f, barMaxHeight * mLevels[i]);
            float activeTop = barBottom - activeHeight;
            mBarRect.set(barLeft, activeTop, barRight, barBottom);

            mBarPaint.setColor(mBarColor);
            if (i == 0 || i == 1) {
                mBarPaint.setAlpha(255);
            } else {
                mBarPaint.setAlpha(190);
            }
            canvas.drawRoundRect(mBarRect, 8f, 8f, mBarPaint);

            // Label
            canvas.drawText(mLabels[i], cx, h - 6f, mTextPaint);
        }

        if (needRedraw) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (int i = 0; i < 4; i++) {
            mLevels[i] = 0f;
            mTargetLevels[i] = 0f;
        }
    }
}
