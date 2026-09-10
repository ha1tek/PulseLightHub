package com.antigravity.pulselight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int color, boolean fromUser);
        default void onColorChangeStop(int color) {}
    }

    private OnColorChangeListener mListener;

    private Paint mWheelPaint;
    private Paint mThumbFillPaint;
    private Paint mThumbStrokePaint;

    private int mSelectedColor = 0xFF71BBFF; // Default Realme GT Blue
    private float mAngle = 0.0f;
    private float mCenterX = 0f;
    private float mCenterY = 0f;
    private float mTrackRadius = 0f;
    private float mWheelThickness = 0f;
    private final RectF mWheelRect = new RectF();

    // Exact color gradient palette and positions from Realme GT 5 / ColorOS BreathingLightColorPickRound
    public static final int[] DEFAULT_COLORS = new int[]{
            -1,         // 0xFFFFFFFF (White transition point)
            -32768,     // 0xFFFF8000 (Orange)
            -32768,     // 0xFFFF8000 (Orange)
            -256,       // 0xFFFFFF00 (Yellow)
            -8388864,   // 0xFF7FFF00 (Chartreuse)
            -16711936,  // 0xFF00FF00 (Green)
            -16711809,  // 0xFF00FF7F (Spring Green)
            -16711681,  // 0xFF00FFFF (Cyan)
            -16744193,  // 0xFF007FFF (Azure)
            -8388353,   // 0xFF7F00FF (Violet)
            -65281,     // 0xFFFF00FF (Magenta)
            -65409,     // 0xFFFF007F (Rose)
            -65536,     // 0xFFFF0000 (Red)
            -1          // 0xFFFFFFFF (White)
    };

    public static final float[] GRADIENT_POSITIONS = new float[]{
            0.005f, 0.02f, 0.09f, 0.18f, 0.27f, 0.36f, 0.45f, 0.55f, 0.64f, 0.73f, 0.82f, 0.91f, 0.99f, 1.0f
    };

    // The 26 hardware-calibrated colors recognized by Qualcomm Lights HAL (/odm/etc/misc/oplusLights.xml)
    public static final int[] OPTION_COLORS = new int[]{
            0xFFFDFFFB, // 0: White
            0xFFFFBE15, // 1: Orange
            0xFFFFBE14, // 2: Orange 1
            0xFFFFBE13, // 3: Orange 2
            0xFFFFBE12, // 4: Orange 3
            0xFFFFBE11, // 5: Orange 4
            0xFFFFFC3C, // 6: Yellow
            0xFFFFFC3B, // 7: Yellow 1
            0xFFFFFC3A, // 8: Yellow 2
            0xFFFFFC39, // 9: Yellow 3
            0xFF00FF1E, // 10: Green
            0xFF00FF1D, // 11: Green 1
            0xFF00FF1C, // 12: Green 2
            0xFF00FF1B, // 13: Green 3
            0xFF00FF1A, // 14: Green 4
            0xFF00FF19, // 15: Green 5
            0xFF00FF18, // 16: Green 6
            0xFF74BBFF, // 17: Blue
            0xFF73BBFF, // 18: Blue 1
            0xFF72BBFF, // 19: Blue 2
            0xFF71BBFF, // 20: Blue 3 (Realme GT Blue)
            0xFFFFA8FF, // 21: Purple
            0xFFFFA7FF, // 22: Purple 1
            0xFFFF8173, // 23: Red 2
            0xFFFF8174, // 24: Red 1
            0xFFFF8175  // 25: Red
    };

    // Thresholds from ColorOS BreathingLightMultiLedConstantUtils.MULTI_LED_RING_PERCENT
    public static final float[] MULTI_LED_RING_PERCENT = new float[]{
            0.015f, 0.095f, 0.1225f, 0.15f, 0.1775f, 0.2f, 0.245f, 0.26f, 0.3f, 0.46f,
            0.5f, 0.53f, 0.56f, 0.59f, 0.62f, 0.66f, 0.7f, 0.73f, 0.76f, 0.79f,
            0.82f, 0.86f, 0.9f, 0.94f, 0.98f, 0.995f, 1.0f
    };

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWheelPaint.setStyle(Paint.Style.STROKE);
        mWheelPaint.setDither(true);

        mThumbFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThumbFillPaint.setStyle(Paint.Style.FILL);

        mThumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThumbStrokePaint.setStyle(Paint.Style.STROKE);
        mThumbStrokePaint.setStrokeWidth(3.5f * getResources().getDisplayMetrics().density);
        mThumbStrokePaint.setColor(Color.WHITE);
        mThumbStrokePaint.setShadowLayer(4f * getResources().getDisplayMetrics().density, 0, 1.5f * getResources().getDisplayMetrics().density, 0x88000000);

        mAngle = findAngleForColor(mSelectedColor);
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mListener = listener;
    }

    public void setColor(int color) {
        mSelectedColor = toNearestHardwareColor(color);
        mAngle = findAngleForColor(mSelectedColor);
        invalidate();
    }

    public int getColor() {
        return mSelectedColor;
    }

    /**
     * Map angle (0 .. 2*PI) strictly to one of the 26 hardware calibrated colors
     * matching BreathingLightColorUtils.getColorsIndex.
     */
    public static int getCalibratedColorForAngle(float angleRad) {
        float f = (float) (angleRad / (2.0 * Math.PI));
        while (f < 0.0f) f += 1.0f;
        while (f >= 1.0f) f -= 1.0f;

        if (f <= MULTI_LED_RING_PERCENT[0] || f >= MULTI_LED_RING_PERCENT[MULTI_LED_RING_PERCENT.length - 1]) {
            return OPTION_COLORS[0];
        }
        if (f > MULTI_LED_RING_PERCENT[MULTI_LED_RING_PERCENT.length - 2] && f < MULTI_LED_RING_PERCENT[MULTI_LED_RING_PERCENT.length - 1]) {
            return OPTION_COLORS[OPTION_COLORS.length - 1];
        }
        for (int i = 1; i < MULTI_LED_RING_PERCENT.length; i++) {
            if (f > MULTI_LED_RING_PERCENT[i - 1] && f <= MULTI_LED_RING_PERCENT[i]) {
                int idx = Math.min(i, OPTION_COLORS.length - 1);
                return OPTION_COLORS[idx];
            }
        }
        return OPTION_COLORS[0];
    }

    public static int toNearestHardwareColor(int targetColor) {
        int tr = (targetColor >> 16) & 0xFF;
        int tg = (targetColor >> 8) & 0xFF;
        int tb = targetColor & 0xFF;

        int bestColor = OPTION_COLORS[20]; // Default GT Blue
        int minDistance = Integer.MAX_VALUE;

        for (int opt : OPTION_COLORS) {
            int or = (opt >> 16) & 0xFF;
            int og = (opt >> 8) & 0xFF;
            int ob = opt & 0xFF;
            int dr = tr - or;
            int dg = tg - og;
            int db = tb - ob;
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestColor = opt;
                if (dist == 0) break;
            }
        }
        return bestColor;
    }

    public static float findAngleForColor(int targetColor) {
        int cleanTarget = targetColor & 0x00FFFFFF;
        int bestIdx = 20; // Default to GT Blue
        int minDistance = Integer.MAX_VALUE;

        int tr = (cleanTarget >> 16) & 0xFF;
        int tg = (cleanTarget >> 8) & 0xFF;
        int tb = cleanTarget & 0xFF;

        for (int i = 0; i < OPTION_COLORS.length; i++) {
            int opt = OPTION_COLORS[i] & 0x00FFFFFF;
            if (opt == cleanTarget) {
                bestIdx = i;
                minDistance = 0;
                break;
            }
            int or = (opt >> 16) & 0xFF;
            int og = (opt >> 8) & 0xFF;
            int ob = opt & 0xFF;
            int dist = (tr - or) * (tr - or) + (tg - og) * (tg - og) + (tb - ob) * (tb - ob);
            if (dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
        }

        float pStart = (bestIdx == 0) ? 0.0f : MULTI_LED_RING_PERCENT[bestIdx - 1];
        float pEnd = MULTI_LED_RING_PERCENT[Math.min(bestIdx, MULTI_LED_RING_PERCENT.length - 1)];
        float mid = (pStart + pEnd) / 2.0f;
        return (float) (mid * 2.0 * Math.PI);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2.0f;
        mCenterY = h / 2.0f;

        float minDim = Math.min(w, h);
        float density = getResources().getDisplayMetrics().density;
        float padding = 20f * density;

        // Outer radius of donut
        float outerRadius = (minDim / 2.0f) - padding;
        // Donut thickness: approx 26% of outer diameter, matching ColorOS
        mWheelThickness = outerRadius * 0.44f;
        mWheelPaint.setStrokeWidth(mWheelThickness);

        // Center line of track
        mTrackRadius = outerRadius - (mWheelThickness / 2.0f);
        mWheelRect.set(
                mCenterX - mTrackRadius,
                mCenterY - mTrackRadius,
                mCenterX + mTrackRadius,
                mCenterY + mTrackRadius
        );

        if (mTrackRadius > 0f) {
            SweepGradient sweep = new SweepGradient(mCenterX, mCenterY, DEFAULT_COLORS, GRADIENT_POSITIONS);
            mWheelPaint.setShader(sweep);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTrackRadius <= 0f) return;

        // 1. Draw the hollow annular ring (donut) with 100% saturated gradient
        canvas.drawOval(mWheelRect, mWheelPaint);

        // 2. Draw selector thumb bead sitting strictly on the donut centerline
        float thumbX = mCenterX + (float) Math.cos(mAngle) * mTrackRadius;
        float thumbY = mCenterY + (float) Math.sin(mAngle) * mTrackRadius;
        float thumbRadius = (mWheelThickness / 2.0f) * 1.05f;

        // Inner saturated color fill
        mThumbFillPaint.setColor(mSelectedColor);
        canvas.drawCircle(thumbX, thumbY, thumbRadius, mThumbFillPaint);

        // Crisp white ring outline
        canvas.drawCircle(thumbX, thumbY, thumbRadius, mThumbStrokePaint);
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
                updateFromTouch(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                updateFromTouch(x, y);
                return true;

            case MotionEvent.ACTION_UP:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                updateFromTouch(x, y);
                if (mListener != null) {
                    mListener.onColorChangeStop(mSelectedColor);
                }
                return true;

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

    private void updateFromTouch(float touchX, float touchY) {
        float dx = touchX - mCenterX;
        float dy = touchY - mCenterY;

        float angle = (float) Math.atan2(dy, dx);
        if (angle < 0) {
            angle += (float) (2 * Math.PI);
        }

        mAngle = angle;
        int newColor = getCalibratedColorForAngle(mAngle);
        mSelectedColor = newColor;

        if (mListener != null) {
            mListener.onColorChanged(mSelectedColor, true);
        }

        invalidate();
    }
}
