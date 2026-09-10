package com.antigravity.pulselight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ColorSliderView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int color, boolean fromUser);
        default void onColorChangeStop(int color) {}
    }

    private OnColorChangeListener mListener;

    // Ordered strictly left-to-right as requested:
    // Белый, Розовый, Красный, Оранжевый, Желтый, Зеленый, Голубой, Синий, Фиолетовый
    public static final int[] PALETTE = new int[]{
            0xFFFDFFFB, // 0: Белый (Pure White)
            0xFFFF2D7A, // 1: Розовый (Cyber Pink)
            0xFFFF3B30, // 2: Красный (Racing Red)
            0xFFFF9500, // 3: Оранжевый (Cyber Amber)
            0xFFFFEA00, // 4: Желтый (Neon Yellow)
            0xFF00E676, // 5: Зеленый (Matrix Green)
            0xFF71BBFF, // 6: Голубой (Realme GT Blue)
            0xFF2979FF, // 7: Синий (Electric Blue)
            0xFFA820FF  // 8: Фиолетовый (GT Purple)
    };

    private final Paint mSegmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mSegRect = new RectF();
    private final float[] mAnimFractions = new float[PALETTE.length];
    private ValueAnimator mAnimator = null;

    private int mSelectedIndex = 6; // Default to Голубой (Realme GT Blue, index 6)
    private int mSelectedColor = PALETTE[6];

    public ColorSliderView(Context context) {
        super(context);
        init();
    }

    public ColorSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Enable clean shadow layers

        mSegmentPaint.setStyle(Paint.Style.FILL);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        float density = getResources().getDisplayMetrics().density;
        mBorderPaint.setStrokeWidth(1.2f * density);
        mBorderPaint.setColor(0x33FFFFFF);

        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(2.5f * density);
        mHighlightPaint.setColor(Color.WHITE);

        mShadowPaint.setStyle(Paint.Style.FILL);

        mSelectedIndex = 6; // Голубой
        mSelectedColor = PALETTE[mSelectedIndex];
        mAnimFractions[mSelectedIndex] = 1.0f;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mListener = listener;
    }

    public void setColor(int color) {
        int bestIndex = 6;
        int minDistance = Integer.MAX_VALUE;
        int tr = (color >> 16) & 0xFF;
        int tg = (color >> 8) & 0xFF;
        int tb = color & 0xFF;

        for (int i = 0; i < PALETTE.length; i++) {
            int c = PALETTE[i];
            int dr = tr - ((c >> 16) & 0xFF);
            int dg = tg - ((c >> 8) & 0xFF);
            int db = tb - (c & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestIndex = i;
            }
        }

        if (bestIndex != mSelectedIndex) {
            selectIndexAnimated(bestIndex, false);
        } else {
            mSelectedColor = PALETTE[mSelectedIndex];
            invalidate();
        }
    }

    public int getColor() {
        return mSelectedColor;
    }

    private void selectIndexAnimated(int newIndex, boolean fromUser) {
        if (newIndex < 0 || newIndex >= PALETTE.length) return;
        final int prevIndex = mSelectedIndex;
        mSelectedIndex = newIndex;
        mSelectedColor = PALETTE[mSelectedIndex];

        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        final float startPrev = mAnimFractions[prevIndex];
        final float startNew = mAnimFractions[newIndex];

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(190);
        mAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        mAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            for (int i = 0; i < PALETTE.length; i++) {
                if (i == mSelectedIndex) {
                    mAnimFractions[i] = startNew + t * (1.0f - startNew);
                } else if (i == prevIndex) {
                    mAnimFractions[i] = startPrev + t * (0.0f - startPrev);
                } else {
                    mAnimFractions[i] = Math.max(0f, mAnimFractions[i] - t * 0.2f);
                }
            }
            postInvalidateOnAnimation();
        });
        mAnimator.start();

        if (fromUser) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (mListener != null) {
                mListener.onColorChanged(mSelectedColor, true);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int count = PALETTE.length;
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float padX = 8f * density;
        float gap = 5f * density;

        float totalAvailableW = w - 2 * padX - (count - 1) * gap;
        if (totalAvailableW <= 0) return;

        float segW = totalAvailableW / count;
        float baseH = 34f * density;
        float expandedH = 50f * density;
        float baseCorner = 9f * density;
        float expandedCorner = 13f * density;
        float centerY = h / 2.0f;

        // 1. Draw non-selected segments first
        for (int i = 0; i < count; i++) {
            if (i == mSelectedIndex) continue;

            float f = mAnimFractions[i];
            drawTile(canvas, i, f, padX, gap, segW, baseH, expandedH, baseCorner, expandedCorner, centerY, density);
        }

        // 2. Draw selected segment on top with elevated pop and highlight
        float selectedF = mAnimFractions[mSelectedIndex];
        drawTile(canvas, mSelectedIndex, selectedF, padX, gap, segW, baseH, expandedH, baseCorner, expandedCorner, centerY, density);
    }

    private void drawTile(Canvas canvas, int index, float f, float padX, float gap,
                          float segW, float baseH, float expandedH, float baseCorner,
                          float expandedCorner, float centerY, float density) {
        float segLeft = padX + index * (segW + gap);
        float segRight = segLeft + segW;

        // Height expands smoothly
        float curH = baseH + f * (expandedH - baseH);
        float curCorner = baseCorner + f * (expandedCorner - baseCorner);

        float segTop = centerY - curH / 2.0f;
        float segBottom = centerY + curH / 2.0f;

        mSegRect.set(segLeft, segTop, segRight, segBottom);

        // Soft elevated drop shadow under expanded tile
        if (f > 0.05f) {
            mShadowPaint.setColor(Color.argb((int) (60 * f), 0, 0, 0));
            mShadowPaint.setShadowLayer(8f * density * f, 0, 3f * density * f, 0x88000000);
            canvas.drawRoundRect(mSegRect, curCorner, curCorner, mShadowPaint);
        }

        // Color tile body
        mSegmentPaint.setColor(PALETTE[index]);
        canvas.drawRoundRect(mSegRect, curCorner, curCorner, mSegmentPaint);

        // Subtle resting border
        mBorderPaint.setColor(Color.argb((int) (40 * (1.0f - f)), 255, 255, 255));
        canvas.drawRoundRect(mSegRect, curCorner, curCorner, mBorderPaint);

        // Crisp white elevated highlight on active popped tile
        if (f > 0.05f) {
            mHighlightPaint.setAlpha((int) (255 * f));
            mHighlightPaint.setStrokeWidth(2.5f * density);
            mHighlightPaint.setShadowLayer(4f * density * f, 0, 1.5f * density, 0x66000000);
            canvas.drawRoundRect(mSegRect, curCorner, curCorner, mHighlightPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                handleTouch(x);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                handleTouch(x);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (mListener != null) {
                    mListener.onColorChangeStop(mSelectedColor);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTouch(float touchX) {
        int count = PALETTE.length;
        float w = getWidth();
        float density = getResources().getDisplayMetrics().density;
        float padX = 8f * density;
        float gap = 5f * density;

        float totalAvailableW = w - 2 * padX - (count - 1) * gap;
        if (totalAvailableW <= 0) return;
        float segW = totalAvailableW / count;
        float slotW = segW + gap;

        float relX = touchX - padX;
        int targetIndex = (int) (relX / slotW);
        targetIndex = Math.max(0, Math.min(count - 1, targetIndex));

        if (targetIndex != mSelectedIndex) {
            selectIndexAnimated(targetIndex, true);
        }
    }
}
