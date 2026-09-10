package com.antigravity.pulselight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class RealmeGlyphView extends View {

    public interface OnSegmentClickListener {
        void onSegmentToggled(int segmentBitmask, int color);
    }

    private OnSegmentClickListener mListener;

    // Segment active intensities (0.0f = off, 1.0f = fully glowing)
    private float mIntensityA = 0.0f;
    private float mIntensityB = 0.0f;
    private float mIntensityC = 0.0f;
    private float mIntensityD = 0.0f;

    // Always-On state: if true, resting intensity is 0.85f; if false, resting intensity is 0.0f
    private boolean mAlwaysOnState = false;
    private float mRestingIntensity = 0.0f;

    private ValueAnimator mAnimA;
    private ValueAnimator mAnimB;
    private ValueAnimator mAnimC;
    private ValueAnimator mAnimD;

    // Hardware calibrated violet/purple matching physical Realme GT 5 glyphs
    public static final int GLYPH_PURPLE = 0xFFA820FF; // #A820FF

    // Segment colors (defaults to GT Purple matching physical LEDs)
    private int mColorA = GLYPH_PURPLE;
    private int mColorB = GLYPH_PURPLE;
    private int mColorC = GLYPH_PURPLE;
    private int mColorD = GLYPH_PURPLE;

    // Drawing Paints
    private Paint mChassisBgPaint;
    private Paint mChassisBorderPaint;
    private Paint mCarbonStripePaint;
    private Paint mBadgeFillPaint;
    private Paint mBadgeStrokePaint;
    private Paint mSnapdragonPaint;
    private Paint mWatermarkPaint;
    private Paint mTrackOffPaint;
    private Paint mHaloGlowPaint;
    private Paint mHaloCorePaint;
    private Paint mWhiteCorePaint;
    private Paint mBracketPaint;

    // Geometry
    private final RectF mCardBounds = new RectF();
    private final RectF mChipBadge = new RectF();
    private final RectF mSnapdragonFlame = new RectF();

    private float mHaloLeft, mHaloTop, mHaloRight, mHaloBottom;
    private float mCornerRadius, mGap, mTubeStroke;

    // 4 LED Paths
    private final Path mPathA = new Path(); // Top bracket
    private final Path mPathB = new Path(); // Right bar
    private final Path mPathC = new Path(); // Bottom bracket
    private final Path mPathD = new Path(); // Left bar

    private int mActiveTouchSegment = 0;
    private Vibrator mVibrator;

    public RealmeGlyphView(Context context) {
        super(context);
        init(context);
    }

    public RealmeGlyphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RealmeGlyphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mChassisBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChassisBgPaint.setStyle(Paint.Style.FILL);
        mChassisBgPaint.setColor(Color.parseColor("#0C0E14"));

        mChassisBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChassisBorderPaint.setStyle(Paint.Style.STROKE);
        mChassisBorderPaint.setStrokeWidth(1.8f);
        mChassisBorderPaint.setColor(Color.parseColor("#1B202A"));

        mCarbonStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCarbonStripePaint.setStyle(Paint.Style.STROKE);
        mCarbonStripePaint.setStrokeWidth(1.2f);
        mCarbonStripePaint.setColor(Color.parseColor("#141822"));

        mBadgeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBadgeFillPaint.setStyle(Paint.Style.FILL);
        mBadgeFillPaint.setColor(Color.parseColor("#12151D"));

        mBadgeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBadgeStrokePaint.setStyle(Paint.Style.STROKE);
        mBadgeStrokePaint.setStrokeWidth(1.5f);
        mBadgeStrokePaint.setColor(Color.parseColor("#262E3D"));

        mSnapdragonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSnapdragonPaint.setStyle(Paint.Style.STROKE);
        mSnapdragonPaint.setStrokeWidth(2.2f);
        mSnapdragonPaint.setColor(Color.parseColor("#3B455B"));

        mWatermarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWatermarkPaint.setColor(Color.parseColor("#222836"));
        mWatermarkPaint.setLetterSpacing(0.14f);
        mWatermarkPaint.setTextSize(22f);

        mBracketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBracketPaint.setStyle(Paint.Style.FILL);
        mBracketPaint.setColor(Color.parseColor("#080A0E"));

        // Off-state dark frosted diffuser track
        mTrackOffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackOffPaint.setStyle(Paint.Style.STROKE);
        mTrackOffPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrackOffPaint.setStrokeJoin(Paint.Join.ROUND);
        mTrackOffPaint.setColor(Color.parseColor("#1C202C"));

        // Lit glowing paints
        mHaloGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHaloGlowPaint.setStyle(Paint.Style.STROKE);
        mHaloGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        mHaloGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        mHaloCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHaloCorePaint.setStyle(Paint.Style.STROKE);
        mHaloCorePaint.setStrokeCap(Paint.Cap.ROUND);
        mHaloCorePaint.setStrokeJoin(Paint.Join.ROUND);

        mWhiteCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWhiteCorePaint.setStyle(Paint.Style.STROKE);
        mWhiteCorePaint.setStrokeCap(Paint.Cap.ROUND);
        mWhiteCorePaint.setStrokeJoin(Paint.Join.ROUND);
        mWhiteCorePaint.setColor(Color.WHITE);

        updateColorsFromManager();
    }

    public void setOnSegmentClickListener(OnSegmentClickListener listener) {
        mListener = listener;
    }

    public void updateColorsFromManager() {
        Context ctx = getContext();
        int color = (ctx != null) ? GlyphColorManager.getUnifiedColor(ctx) : GLYPH_PURPLE;

        mColorA = color;
        mColorB = color;
        mColorC = color;
        mColorD = color;

        mIntensityA = mRestingIntensity;
        mIntensityB = mRestingIntensity;
        mIntensityC = mRestingIntensity;
        mIntensityD = mRestingIntensity;

        postInvalidate();
    }

    public void setPower(boolean on) {
        mAlwaysOnState = on;
        mRestingIntensity = on ? 0.85f : 0.0f;
        cancelSegmentAnim(RealmeGlyphDriver.LED_ALL);

        ValueAnimator anim = ValueAnimator.ofFloat(mIntensityA, mRestingIntensity);
        anim.setDuration(350);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float val = (float) a.getAnimatedValue();
            mIntensityA = val;
            mIntensityB = val;
            mIntensityC = val;
            mIntensityD = val;
            postInvalidateOnAnimation();
        });
        registerAnim(RealmeGlyphDriver.LED_ALL, anim);
        anim.start();
    }

    /**
     * Preview a selected color.
     * If Always-On is active, updates color and stays glowing.
     * If Always-On is off, smoothly pulses to demonstrate color and fades back to off.
     */
    public void setPreviewColor(int color) {
        mColorA = color;
        mColorB = color;
        mColorC = color;
        mColorD = color;

        if (mAlwaysOnState) {
            mIntensityA = 0.85f;
            mIntensityB = 0.85f;
            mIntensityC = 0.85f;
            mIntensityD = 0.85f;
            postInvalidate();
        } else {
            pulsePreview(color);
        }
    }

    /**
     * Live color update while dragging color wheel: glows at 0.9f in live color.
     */
    public void setLiveColor(int color) {
        mColorA = color;
        mColorB = color;
        mColorC = color;
        mColorD = color;
        cancelSegmentAnim(RealmeGlyphDriver.LED_ALL);
        mIntensityA = 0.90f;
        mIntensityB = 0.90f;
        mIntensityC = 0.90f;
        mIntensityD = 0.90f;
        postInvalidate();
    }

    /**
     * Color pick finished: smoothly decays to resting state (0.0f if Always-On off, 0.85f if on).
     */
    public void onColorPickFinished(int color) {
        mColorA = color;
        mColorB = color;
        mColorC = color;
        mColorD = color;
        cancelSegmentAnim(RealmeGlyphDriver.LED_ALL);

        ValueAnimator anim = ValueAnimator.ofFloat(0.90f, mRestingIntensity);
        anim.setDuration(400);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float val = (float) a.getAnimatedValue();
            mIntensityA = val;
            mIntensityB = val;
            mIntensityC = val;
            mIntensityD = val;
            postInvalidateOnAnimation();
        });
        registerAnim(RealmeGlyphDriver.LED_ALL, anim);
        anim.start();
    }

    /**
     * Pulses all segments once to demonstrate the new color, then smoothly fades out.
     */
    public void pulsePreview(int color) {
        cancelSegmentAnim(RealmeGlyphDriver.LED_ALL);
        mColorA = color;
        mColorB = color;
        mColorC = color;
        mColorD = color;

        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, mRestingIntensity);
        anim.setDuration(550);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float val = (float) a.getAnimatedValue();
            mIntensityA = val;
            mIntensityB = val;
            mIntensityC = val;
            mIntensityD = val;
            postInvalidateOnAnimation();
        });
        registerAnim(RealmeGlyphDriver.LED_ALL, anim);
        anim.start();
    }

    public void setSegmentIntensity(int segmentBitmask, float intensity, int color) {
        float val = Math.max(0f, Math.min(1f, intensity));
        if ((segmentBitmask & RealmeGlyphDriver.LED_A) != 0) {
            mIntensityA = val;
            if (color != 0) mColorA = color;
        }
        if ((segmentBitmask & RealmeGlyphDriver.LED_B) != 0) {
            mIntensityB = val;
            if (color != 0) mColorB = color;
        }
        if ((segmentBitmask & RealmeGlyphDriver.LED_C) != 0) {
            mIntensityC = val;
            if (color != 0) mColorC = color;
        }
        if ((segmentBitmask & RealmeGlyphDriver.LED_D) != 0) {
            mIntensityD = val;
            if (color != 0) mColorD = color;
        }
        postInvalidateOnAnimation();
    }

    public void flashSegment(int segmentBitmask, int color, int durationMs) {
        cancelSegmentAnim(segmentBitmask);
        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, mRestingIntensity);
        anim.setDuration(durationMs);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            setSegmentIntensity(segmentBitmask, val, color);
        });
        registerAnim(segmentBitmask, anim);
        anim.start();
    }

    private void cancelSegmentAnim(int mask) {
        if ((mask & RealmeGlyphDriver.LED_A) != 0 && mAnimA != null) { mAnimA.cancel(); mAnimA = null; }
        if ((mask & RealmeGlyphDriver.LED_B) != 0 && mAnimB != null) { mAnimB.cancel(); mAnimB = null; }
        if ((mask & RealmeGlyphDriver.LED_C) != 0 && mAnimC != null) { mAnimC.cancel(); mAnimC = null; }
        if ((mask & RealmeGlyphDriver.LED_D) != 0 && mAnimD != null) { mAnimD.cancel(); mAnimD = null; }
    }

    private void registerAnim(int mask, ValueAnimator anim) {
        if ((mask & RealmeGlyphDriver.LED_A) != 0) mAnimA = anim;
        if ((mask & RealmeGlyphDriver.LED_B) != 0) mAnimB = anim;
        if ((mask & RealmeGlyphDriver.LED_C) != 0) mAnimC = anim;
        if ((mask & RealmeGlyphDriver.LED_D) != 0) mAnimD = anim;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildRealmeGeometry(w, h);
    }

    private void buildRealmeGeometry(int width, int height) {
        float cx = width / 2.0f;
        float cy = height / 2.0f;

        // Proportional GT 5 transparent camera window (approx 1.55:1 aspect ratio)
        float cardWidth = Math.min(width * 0.88f, (height - 16f) * 1.55f);
        float cardHeight = cardWidth / 1.55f;

        mCardBounds.set(cx - cardWidth / 2.0f, cy - cardHeight / 2.0f, cx + cardWidth / 2.0f, cy + cardHeight / 2.0f);

        // Halo sits neatly centered inside the transparent window with breathing padding
        float padX = cardWidth * 0.11f;
        float padY = cardHeight * 0.12f;

        mHaloLeft = mCardBounds.left + padX;
        mHaloTop = mCardBounds.top + padY;
        mHaloRight = mCardBounds.right - padX;
        mHaloBottom = mCardBounds.bottom - padY;

        float haloH = mHaloBottom - mHaloTop;

        // Snapdragon chipset badge in the center of the halo
        float badgeSize = haloH * 0.52f;
        mChipBadge.set(cx - badgeSize / 2.0f, cy - badgeSize / 2.0f, cx + badgeSize / 2.0f, cy + badgeSize / 2.0f);

        float flameR = badgeSize * 0.28f;
        mSnapdragonFlame.set(cx - flameR, cy - flameR, cx + flameR, cy + flameR);

        // LED light tube stroke and corner radius
        mTubeStroke = haloH * 0.075f;
        mTrackOffPaint.setStrokeWidth(mTubeStroke);
        mHaloCorePaint.setStrokeWidth(mTubeStroke);
        mHaloGlowPaint.setStrokeWidth(mTubeStroke * 2.5f);
        mWhiteCorePaint.setStrokeWidth(mTubeStroke * 0.35f);

        mCornerRadius = haloH * 0.18f;
        mGap = haloH * 0.08f;

        // 1. TOP SEGMENT A (Inverted U bracket: Left curve, Top bar, Right curve)
        mPathA.reset();
        mPathA.moveTo(mHaloLeft, mHaloTop + mCornerRadius);
        mPathA.arcTo(new RectF(mHaloLeft, mHaloTop, mHaloLeft + 2 * mCornerRadius, mHaloTop + 2 * mCornerRadius), 180, 90, false);
        mPathA.lineTo(mHaloRight - mCornerRadius, mHaloTop);
        mPathA.arcTo(new RectF(mHaloRight - 2 * mCornerRadius, mHaloTop, mHaloRight, mHaloTop + 2 * mCornerRadius), 270, 90, false);
        mPathA.lineTo(mHaloRight, mHaloTop + mCornerRadius);

        // 2. BOTTOM SEGMENT C (U bracket: Left curve, Bottom bar, Right curve)
        mPathC.reset();
        mPathC.moveTo(mHaloLeft, mHaloBottom - mCornerRadius);
        mPathC.arcTo(new RectF(mHaloLeft, mHaloBottom - 2 * mCornerRadius, mHaloLeft + 2 * mCornerRadius, mHaloBottom), 180, -90, false);
        mPathC.lineTo(mHaloRight - mCornerRadius, mHaloBottom);
        mPathC.arcTo(new RectF(mHaloRight - 2 * mCornerRadius, mHaloBottom - 2 * mCornerRadius, mHaloRight, mHaloBottom), 90, -90, false);
        mPathC.lineTo(mHaloRight, mHaloBottom - mCornerRadius);

        // 3. LEFT SEGMENT D (Straight vertical bar)
        float leftBarTop = mHaloTop + mCornerRadius + mGap;
        float leftBarBottom = mHaloBottom - mCornerRadius - mGap;
        mPathD.reset();
        mPathD.moveTo(mHaloLeft, leftBarTop);
        mPathD.lineTo(mHaloLeft, leftBarBottom);

        // 4. RIGHT SEGMENT B (Straight vertical bar)
        float rightBarTop = mHaloTop + mCornerRadius + mGap;
        float rightBarBottom = mHaloBottom - mCornerRadius - mGap;
        mPathB.reset();
        mPathB.moveTo(mHaloRight, rightBarTop);
        mPathB.lineTo(mHaloRight, rightBarBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCardBounds.isEmpty()) return;

        // 1. Transparent Camera Glass Window (Dark Metallic Base)
        canvas.drawRoundRect(mCardBounds, 24f, 24f, mChassisBgPaint);
        canvas.drawRoundRect(mCardBounds, 24f, 24f, mChassisBorderPaint);

        // 6. Realistic Unlit LED Diffuser Tracks (always visible at base)
        canvas.drawPath(mPathA, mTrackOffPaint);
        canvas.drawPath(mPathB, mTrackOffPaint);
        canvas.drawPath(mPathC, mTrackOffPaint);
        canvas.drawPath(mPathD, mTrackOffPaint);

        // 7. Hardware clamp brackets at 4 segment gaps
        float halfGap = mGap * 0.5f;
        drawGapBracket(canvas, mHaloLeft, mHaloTop + mCornerRadius + halfGap);
        drawGapBracket(canvas, mHaloLeft, mHaloBottom - mCornerRadius - halfGap);
        drawGapBracket(canvas, mHaloRight, mHaloTop + mCornerRadius + halfGap);
        drawGapBracket(canvas, mHaloRight, mHaloBottom - mCornerRadius - halfGap);

        // 8. Lit Glowing Neon Segments
        drawSegmentGlow(canvas, mPathA, mColorA, mIntensityA);
        drawSegmentGlow(canvas, mPathB, mColorB, mIntensityB);
        drawSegmentGlow(canvas, mPathC, mColorC, mIntensityC);
        drawSegmentGlow(canvas, mPathD, mColorD, mIntensityD);
    }

    private void drawGapBracket(Canvas canvas, float cx, float cy) {
        float bw = mTubeStroke * 1.4f;
        float bh = mGap * 0.65f;
        RectF bracketRect = new RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f);
        canvas.drawRoundRect(bracketRect, 2f, 2f, mBracketPaint);
        canvas.drawRoundRect(bracketRect, 2f, 2f, mChassisBorderPaint);
    }

    private void drawSegmentGlow(Canvas canvas, Path path, int color, float intensity) {
        if (intensity <= 0.02f) return;

        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Core saturated neon
        int coreAlpha = (int) (255 * intensity);
        mHaloCorePaint.setColor(Color.argb(coreAlpha, r, g, b));
        canvas.drawPath(path, mHaloCorePaint);

        // Inner white filament (intense shine)
        if (intensity > 0.40f) {
            int whiteAlpha = (int) (230 * (intensity - 0.40f) / 0.60f);
            mWhiteCorePaint.setColor(Color.argb(whiteAlpha, 255, 255, 255));
            canvas.drawPath(path, mWhiteCorePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                int hit = hitTest(x, y);
                if (hit != 0) {
                    mActiveTouchSegment = hit;
                    handleSegmentTouch(hit, true);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                int currentHit = hitTest(x, y);
                if (currentHit != 0 && currentHit != mActiveTouchSegment) {
                    if (mActiveTouchSegment != 0) {
                        handleSegmentTouch(mActiveTouchSegment, false);
                    }
                    mActiveTouchSegment = currentHit;
                    handleSegmentTouch(currentHit, true);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (mActiveTouchSegment != 0) {
                    handleSegmentTouch(mActiveTouchSegment, false);
                    mActiveTouchSegment = 0;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int hitTest(float x, float y) {
        if (!mCardBounds.contains(x, y)) return 0;

        float haloH = mHaloBottom - mHaloTop;
        float haloW = mHaloRight - mHaloLeft;
        float cx = mCardBounds.centerX();

        if (y < mHaloTop + haloH * 0.30f) {
            return RealmeGlyphDriver.LED_A; // Top bracket
        } else if (y > mHaloBottom - haloH * 0.30f) {
            return RealmeGlyphDriver.LED_C; // Bottom bracket
        } else {
            // Middle horizontal slice
            if (x < cx - haloW * 0.18f) {
                return RealmeGlyphDriver.LED_D; // Left bar
            } else if (x > cx + haloW * 0.18f) {
                return RealmeGlyphDriver.LED_B; // Right bar
            } else {
                return RealmeGlyphDriver.LED_ALL; // Center Snapdragon badge taps all
            }
        }
    }

    public int getSegmentColor(int segmentMask) {
        if ((segmentMask & RealmeGlyphDriver.LED_A) != 0) return mColorA;
        if ((segmentMask & RealmeGlyphDriver.LED_B) != 0) return mColorB;
        if ((segmentMask & RealmeGlyphDriver.LED_C) != 0) return mColorC;
        if ((segmentMask & RealmeGlyphDriver.LED_D) != 0) return mColorD;
        return mColorA;
    }

    private void handleSegmentTouch(int segmentMask, boolean isDown) {
        int color = getSegmentColor(segmentMask);
        if (isDown) {
            triggerHaptic();
            cancelSegmentAnim(segmentMask);
            setSegmentIntensity(segmentMask, 1.0f, color);

            // Instant physical hardware LED flash on Realme GT 5!
            RealmeGlyphDriver.flashSegment(segmentMask, color);

            if (mListener != null) {
                mListener.onSegmentToggled(segmentMask, color);
            }
        } else {
            // Finger lifted: smoothly fade back to resting intensity (0.0f)
            flashSegment(segmentMask, color, 280);
            RealmeGlyphDriver.turnOff();
        }
    }

    private void triggerHaptic() {
        try {
            if (mVibrator != null && mVibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mVibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    mVibrator.vibrate(20);
                }
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelSegmentAnim(RealmeGlyphDriver.LED_ALL);
        mIntensityA = 0f;
        mIntensityB = 0f;
        mIntensityC = 0f;
        mIntensityD = 0f;
    }
}
