package com.antigravity.pulselight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SpectrumVisualizerView extends View {

    public static final int MODE_NARROW = 0; // 4 bands
    public static final int MODE_WIDE = 1;   // 10 bands + spline curve
    public static final int MODE_BOTH = 2;   // 10 bands + 4 narrow indicators

    public interface OnBandSelectedListener {
        void onBandSelected(int bandIndex, boolean isWide);
    }

    private OnBandSelectedListener mBandSelectedListener;

    private int mMode = MODE_NARROW;
    private int mBarColor = 0xFFCCFF00;

    // Narrow 4 bands
    private final float[] mNarrowLevels = new float[4];
    private final float[] mNarrowTarget = new float[4];
    private final boolean[] mNarrowEnabled = new boolean[]{true, true, true, true};
    private static final String[] NARROW_LABELS = new String[]{"SUB", "KICK", "SNARE", "TREB"};

    // Wide 12 bands
    private final float[] mWideLevels = new float[12];
    private final float[] mWideTarget = new float[12];
    private final boolean[] mWideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
    private static final String[] WIDE_LABELS = new String[]{"30", "60", "120", "250", "500", "1k", "2k", "4k", "6k", "9k", "12k", "16k"};

    private int mSelectedBand = 0;

    // Drawing objects
    private Paint mBarPaint;
    private Paint mBgBarPaint;
    private Paint mCurvePaint;
    private Paint mTextPaint;
    private Paint mHighlightPaint;
    private final RectF mBarRect = new RectF();
    private final Path mSplinePath = new Path();

    private final ThemeManager.OnThemeChangeListener mThemeListener = (bgColor, accentColor) -> {
        setBarColor(accentColor);
    };

    public SpectrumVisualizerView(Context context) {
        super(context);
        init();
    }

    public SpectrumVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpectrumVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setBarColor(ThemeManager.getAccentColor(getContext()));
        ThemeManager.addListener(mThemeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ThemeManager.removeListener(mThemeListener);
    }

    private void init() {
        mBarColor = ThemeManager.getAccentColor(getContext());
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(mBarColor);

        mBgBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgBarPaint.setStyle(Paint.Style.FILL);
        mBgBarPaint.setColor(Color.parseColor("#151821"));

        mCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurvePaint.setStyle(Paint.Style.STROKE);
        mCurvePaint.setStrokeWidth(3.5f * getResources().getDisplayMetrics().density);
        mCurvePaint.setColor(Color.WHITE);
        mCurvePaint.setStrokeCap(Paint.Cap.ROUND);
        mCurvePaint.setStrokeJoin(Paint.Join.ROUND);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.parseColor("#8E909A"));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setLetterSpacing(0.06f);

        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        mHighlightPaint.setColor(Color.WHITE);
    }

    public void setOnBandSelectedListener(OnBandSelectedListener listener) {
        mBandSelectedListener = listener;
    }

    public void setSpectrumMode(int mode) {
        mMode = mode;
        mSelectedBand = 0;
        postInvalidateOnAnimation();
    }

    public int getSpectrumMode() {
        return mMode;
    }

    public void setSelectedBand(int index) {
        mSelectedBand = index;
        postInvalidateOnAnimation();
    }

    public int getSelectedBand() {
        return mSelectedBand;
    }

    public void setBarColor(int color) {
        if (color != 0) {
            mBarColor = color;
            mBarPaint.setColor(color);
            mCurvePaint.setColor(color);
            postInvalidateOnAnimation();
        }
    }

    public void updateData(AudioAnalyzer.AnalysisResult result) {
        if (result == null) return;

        if (result.bandLevels != null) {
            int len = Math.min(4, result.bandLevels.length);
            for (int i = 0; i < len; i++) {
                mNarrowTarget[i] = Math.max(0f, Math.min(1f, result.bandLevels[i]));
            }
        }

        if (result.wideLevels != null) {
            int len = Math.min(12, result.wideLevels.length);
            for (int i = 0; i < len; i++) {
                mWideTarget[i] = Math.max(0f, Math.min(1f, result.wideLevels[i]));
            }
        }

        if (result.narrowEnabled != null) {
            int len = Math.min(4, result.narrowEnabled.length);
            System.arraycopy(result.narrowEnabled, 0, mNarrowEnabled, 0, len);
        }

        if (result.wideEnabled != null) {
            int len = Math.min(12, result.wideEnabled.length);
            System.arraycopy(result.wideEnabled, 0, mWideEnabled, 0, len);
        }

        postInvalidateOnAnimation();
    }

    public void setBandEnabled(int bandIndex, boolean isWide, boolean enabled) {
        if (isWide && bandIndex >= 0 && bandIndex < 12) {
            mWideEnabled[bandIndex] = enabled;
            postInvalidateOnAnimation();
        } else if (!isWide && bandIndex >= 0 && bandIndex < 4) {
            mNarrowEnabled[bandIndex] = enabled;
            postInvalidateOnAnimation();
        }
    }

    public boolean isBandEnabled(int bandIndex, boolean isWide) {
        if (isWide && bandIndex >= 0 && bandIndex < 12) {
            return mWideEnabled[bandIndex];
        } else if (!isWide && bandIndex >= 0 && bandIndex < 4) {
            return mNarrowEnabled[bandIndex];
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int w = getWidth();
            if (w <= 0) return super.onTouchEvent(event);

            float x = event.getX();
            int count = (mMode == MODE_NARROW) ? 4 : 12;
            float totalSpace = w * 0.90f;
            float startX = (w - totalSpace) / 2f;
            float colWidth = totalSpace / count;

            int clickedIndex = (int) ((x - startX) / colWidth);
            if (clickedIndex >= 0 && clickedIndex < count) {
                mSelectedBand = clickedIndex;
                if (mBandSelectedListener != null) {
                    mBandSelectedListener.onBandSelected(mSelectedBand, mMode != MODE_NARROW);
                }
                postInvalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float labelHeight = 22f * density;
        float barBottom = h - labelHeight - 4f;
        float barMaxHeight = barBottom - 16f;

        boolean isWide = (mMode != MODE_NARROW);
        int count = isWide ? 12 : 4;
        String[] labels = isWide ? WIDE_LABELS : NARROW_LABELS;
        float[] levels = isWide ? mWideLevels : mNarrowLevels;
        float[] targets = isWide ? mWideTarget : mNarrowTarget;

        float totalSpace = w * 0.92f;
        float stepX = totalSpace / count;
        float barWidth = stepX * (isWide ? 0.60f : 0.72f);
        float startX = (w - totalSpace) / 2f + (stepX - barWidth) / 2f;

        mTextPaint.setTextSize((isWide ? 8.5f : 11.5f) * getResources().getDisplayMetrics().scaledDensity);

        boolean needRedraw = false;

        for (int i = 0; i < count; i++) {
            float diff = targets[i] - levels[i];
            if (Math.abs(diff) > 0.008f) {
                levels[i] += diff * (diff > 0 ? 0.45f : 0.22f);
                needRedraw = true;
            } else {
                levels[i] = targets[i];
            }
        }

        float[] curveX = new float[count];
        float[] curveY = new float[count];

        for (int i = 0; i < count; i++) {
            float cx = startX + i * stepX + barWidth / 2f;
            float barLeft = cx - barWidth / 2f;
            float barRight = cx + barWidth / 2f;
            float barTop = barBottom - (levels[i] * barMaxHeight);

            curveX[i] = cx;
            curveY[i] = Math.max(16f, barTop);

            boolean isColEnabled = isWide ? mWideEnabled[i] : mNarrowEnabled[i];
            if (!isColEnabled) {
                mBgBarPaint.setAlpha(60);
                mBarPaint.setAlpha(65);
                mTextPaint.setAlpha(80);
            } else {
                mBgBarPaint.setAlpha(255);
                mBarPaint.setAlpha(255);
                mTextPaint.setAlpha(255);
            }

            mBarRect.set(barLeft, barBottom - barMaxHeight, barRight, barBottom);
            canvas.drawRoundRect(mBarRect, 6f * density, 6f * density, mBgBarPaint);

            if (levels[i] > 0.02f) {
                mBarRect.set(barLeft, barTop, barRight, barBottom);
                canvas.drawRoundRect(mBarRect, 6f * density, 6f * density, mBarPaint);
            }

            if (i == mSelectedBand) {
                mBarRect.set(barLeft - 2f * density, barBottom - barMaxHeight - 2f * density,
                        barRight + 2f * density, barBottom + 2f * density);
                canvas.drawRoundRect(mBarRect, 8f * density, 8f * density, mHighlightPaint);
            }

            mTextPaint.setColor(i == mSelectedBand ? Color.WHITE : Color.parseColor("#8E909A"));
            if (!isColEnabled) mTextPaint.setAlpha(80);
            canvas.drawText(labels[i], cx, h - 4f, mTextPaint);
        }
        mBgBarPaint.setAlpha(255);
        mBarPaint.setAlpha(255);
        mTextPaint.setAlpha(255);

        if (isWide && count >= 2) {
            mSplinePath.reset();
            mSplinePath.moveTo(curveX[0], curveY[0]);

            for (int i = 0; i < count - 1; i++) {
                float x1 = curveX[i];
                float y1 = curveY[i];
                float x2 = curveX[i + 1];
                float y2 = curveY[i + 1];

                float cx1 = x1 + (x2 - x1) / 2f;
                float cy1 = y1;
                float cx2 = x1 + (x2 - x1) / 2f;
                float cy2 = y2;

                mSplinePath.cubicTo(cx1, cy1, cx2, cy2, x2, y2);
            }

            canvas.drawPath(mSplinePath, mCurvePaint);
        }

        if (mMode == MODE_BOTH) {
            float topY = 10f * density;
            float indRadius = 3.5f * density;
            for (int j = 0; j < 4; j++) {
                float ix = (w * 0.25f) + j * (w * 0.50f / 3f);
                float narrowLvl = mNarrowLevels[j];
                mBarPaint.setAlpha((int) (60 + narrowLvl * 195));
                canvas.drawCircle(ix, topY, indRadius, mBarPaint);
            }
            mBarPaint.setAlpha(255);
        }

        if (needRedraw) {
            postInvalidateOnAnimation();
        }
    }
}
