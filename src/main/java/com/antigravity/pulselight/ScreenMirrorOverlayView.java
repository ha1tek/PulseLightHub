package com.antigravity.pulselight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ScreenMirrorOverlayView extends View {

    private float mIntensityTop = 0f;
    private float mIntensityBottom = 0f;
    private float mIntensityLeft = 0f;
    private float mIntensityRight = 0f;

    private int mColorTop = 0xFFCCFF00;
    private int mColorBottom = 0xFFCCFF00;
    private int mColorLeft = 0xFFCCFF00;
    private int mColorRight = 0xFFCCFF00;

    private float mEdgeThicknessPx;

    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator mAnimTop;
    private ValueAnimator mAnimBottom;
    private ValueAnimator mAnimLeft;
    private ValueAnimator mAnimRight;
    private ValueAnimator mHintAnim;

    private float mHintAlpha = 1.0f;
    private Runnable mDismissListener;
    private android.view.GestureDetector mGestureDetector;

    private final RectF mRectTop = new RectF();
    private final RectF mRectBottom = new RectF();
    private final RectF mRectLeft = new RectF();
    private final RectF mRectRight = new RectF();

    private final Runnable mHintFadeRunnable = () -> {
        if (mHintAnim != null) mHintAnim.cancel();
        mHintAnim = ValueAnimator.ofFloat(mHintAlpha, 0f);
        mHintAnim.setDuration(800);
        mHintAnim.addUpdateListener(anim -> {
            mHintAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        mHintAnim.start();
    };

    public ScreenMirrorOverlayView(Context context) {
        super(context);
        init();
    }

    public ScreenMirrorOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScreenMirrorOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mEdgeThicknessPx = 30f * density;

        mGlowPaint.setStyle(Paint.Style.FILL);

        mTextPaint.setColor(0xBBFFFFFF);
        mTextPaint.setTextSize(14f * density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setLetterSpacing(0.04f);

        setClickable(true);
        setFocusable(true);

        mGestureDetector = new android.view.GestureDetector(getContext(), new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                showTapHint();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mDismissListener != null) {
                    mDismissListener.run();
                }
                return true;
            }
        });
    }

    private void showTapHint() {
        removeCallbacks(mHintFadeRunnable);
        if (mHintAnim != null) mHintAnim.cancel();
        mHintAlpha = 1.0f;
        postDelayed(mHintFadeRunnable, 1800);
        invalidate();
    }

    public void setDismissListener(Runnable listener) {
        mDismissListener = listener;
    }

    public void show() {
        removeCallbacks(mHintFadeRunnable);
        if (mHintAnim != null) mHintAnim.cancel();
        mHintAlpha = 1.0f;
        postDelayed(mHintFadeRunnable, 2200);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRectTop.set(0, 0, w, mEdgeThicknessPx);
        mRectBottom.set(0, h - mEdgeThicknessPx, w, h);
        mRectLeft.set(0, 0, mEdgeThicknessPx, h);
        mRectRight.set(w - mEdgeThicknessPx, 0, w, h);
    }

    public void setSegmentIntensity(int mask, float intensity, int color) {
        setSegmentIntensity(mask, intensity, color, color, color, color);
    }

    public void setSegmentIntensity(int mask, float intensity, int colorTop, int colorRight, int colorBottom, int colorLeft) {
        float val = Math.max(0f, Math.min(1f, intensity));
        if ((mask & RealmeGlyphDriver.LED_A) != 0) {
            if (mAnimTop != null) { mAnimTop.cancel(); mAnimTop = null; }
            mIntensityTop = val;
            if (colorTop != 0) mColorTop = colorTop;
        }
        if ((mask & RealmeGlyphDriver.LED_B) != 0) {
            if (mAnimLeft != null) { mAnimLeft.cancel(); mAnimLeft = null; }
            mIntensityLeft = val;
            if (colorRight != 0) mColorLeft = colorRight;
        }
        if ((mask & RealmeGlyphDriver.LED_C) != 0) {
            if (mAnimBottom != null) { mAnimBottom.cancel(); mAnimBottom = null; }
            mIntensityBottom = val;
            if (colorBottom != 0) mColorBottom = colorBottom;
        }
        if ((mask & RealmeGlyphDriver.LED_D) != 0) {
            if (mAnimRight != null) { mAnimRight.cancel(); mAnimRight = null; }
            mIntensityRight = val;
            if (colorLeft != 0) mColorRight = colorLeft;
        }
        postInvalidateOnAnimation();
    }

    public void fadeSegmentToResting(int mask, int durationMs) {
        if ((mask & RealmeGlyphDriver.LED_A) != 0) animateSegment(RealmeGlyphDriver.LED_A, 0.0f, durationMs);
        if ((mask & RealmeGlyphDriver.LED_B) != 0) animateSegment(RealmeGlyphDriver.LED_B, 0.0f, durationMs);
        if ((mask & RealmeGlyphDriver.LED_C) != 0) animateSegment(RealmeGlyphDriver.LED_C, 0.0f, durationMs);
        if ((mask & RealmeGlyphDriver.LED_D) != 0) animateSegment(RealmeGlyphDriver.LED_D, 0.0f, durationMs);
    }

    private void animateSegment(int segment, float targetIntensity, int durationMs) {
        int effDuration = Math.max(25, durationMs);
        if (segment == RealmeGlyphDriver.LED_A) {
            if (mAnimTop != null) mAnimTop.cancel();
            mAnimTop = ValueAnimator.ofFloat(mIntensityTop, targetIntensity);
            mAnimTop.setDuration(effDuration);
            mAnimTop.setInterpolator(new DecelerateInterpolator());
            mAnimTop.addUpdateListener(a -> {
                mIntensityTop = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            mAnimTop.start();
        } else if (segment == RealmeGlyphDriver.LED_B) {
            if (mAnimLeft != null) mAnimLeft.cancel();
            mAnimLeft = ValueAnimator.ofFloat(mIntensityLeft, targetIntensity);
            mAnimLeft.setDuration(effDuration);
            mAnimLeft.setInterpolator(new DecelerateInterpolator());
            mAnimLeft.addUpdateListener(a -> {
                mIntensityLeft = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            mAnimLeft.start();
        } else if (segment == RealmeGlyphDriver.LED_C) {
            if (mAnimBottom != null) mAnimBottom.cancel();
            mAnimBottom = ValueAnimator.ofFloat(mIntensityBottom, targetIntensity);
            mAnimBottom.setDuration(effDuration);
            mAnimBottom.setInterpolator(new DecelerateInterpolator());
            mAnimBottom.addUpdateListener(a -> {
                mIntensityBottom = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            mAnimBottom.start();
        } else if (segment == RealmeGlyphDriver.LED_D) {
            if (mAnimRight != null) mAnimRight.cancel();
            mAnimRight = ValueAnimator.ofFloat(mIntensityRight, targetIntensity);
            mAnimRight.setDuration(effDuration);
            mAnimRight.setInterpolator(new DecelerateInterpolator());
            mAnimRight.addUpdateListener(a -> {
                mIntensityRight = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            mAnimRight.start();
        }
    }

    public void turnOff() {
        removeCallbacks(mHintFadeRunnable);
        if (mHintAnim != null) { mHintAnim.cancel(); mHintAnim = null; }
        if (mAnimTop != null) { mAnimTop.cancel(); mAnimTop = null; }
        if (mAnimBottom != null) { mAnimBottom.cancel(); mAnimBottom = null; }
        if (mAnimLeft != null) { mAnimLeft.cancel(); mAnimLeft = null; }
        if (mAnimRight != null) { mAnimRight.cancel(); mAnimRight = null; }
        mIntensityTop = 0f;
        mIntensityBottom = 0f;
        mIntensityLeft = 0f;
        mIntensityRight = 0f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // OLED pitch black background
        canvas.drawColor(0xFF000000);

        float t = mEdgeThicknessPx;

        // Top Edge (LED_A)
        if (mIntensityTop > 0.005f) {
            int rgb = mColorTop & 0x00FFFFFF;
            int cSolid = rgb | (((int) (255 * mIntensityTop)) << 24);
            int cMid = rgb | (((int) (210 * mIntensityTop)) << 24);
            int cFade = rgb;
            LinearGradient gradTop = new LinearGradient(
                    0, 0, 0, t,
                    new int[]{cSolid, cMid, cFade},
                    new float[]{0.0f, 0.45f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mGlowPaint.setShader(gradTop);
            canvas.drawRect(mRectTop, mGlowPaint);
        }

        // Bottom Edge (LED_C)
        if (mIntensityBottom > 0.005f) {
            int rgb = mColorBottom & 0x00FFFFFF;
            int cSolid = rgb | (((int) (255 * mIntensityBottom)) << 24);
            int cMid = rgb | (((int) (210 * mIntensityBottom)) << 24);
            int cFade = rgb;
            LinearGradient gradBottom = new LinearGradient(
                    0, h, 0, h - t,
                    new int[]{cSolid, cMid, cFade},
                    new float[]{0.0f, 0.45f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mGlowPaint.setShader(gradBottom);
            canvas.drawRect(mRectBottom, mGlowPaint);
        }

        // Left Edge (LED_D)
        if (mIntensityLeft > 0.005f) {
            int rgb = mColorLeft & 0x00FFFFFF;
            int cSolid = rgb | (((int) (255 * mIntensityLeft)) << 24);
            int cMid = rgb | (((int) (210 * mIntensityLeft)) << 24);
            int cFade = rgb;
            LinearGradient gradLeft = new LinearGradient(
                    0, 0, t, 0,
                    new int[]{cSolid, cMid, cFade},
                    new float[]{0.0f, 0.45f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mGlowPaint.setShader(gradLeft);
            canvas.drawRect(mRectLeft, mGlowPaint);
        }

        // Right Edge (LED_B)
        if (mIntensityRight > 0.005f) {
            int rgb = mColorRight & 0x00FFFFFF;
            int cSolid = rgb | (((int) (255 * mIntensityRight)) << 24);
            int cMid = rgb | (((int) (210 * mIntensityRight)) << 24);
            int cFade = rgb;
            LinearGradient gradRight = new LinearGradient(
                    w, 0, w - t, 0,
                    new int[]{cSolid, cMid, cFade},
                    new float[]{0.0f, 0.45f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mGlowPaint.setShader(gradRight);
            canvas.drawRect(mRectRight, mGlowPaint);
        }

        mGlowPaint.setShader(null);

        // Center hint text that gracefully fades into pure black
        if (mHintAlpha > 0.01f) {
            mTextPaint.setAlpha((int) (200 * mHintAlpha));
            canvas.drawText("Двойное нажатие для выхода", w / 2f, h / 2f, mTextPaint);
        }
    }
}
