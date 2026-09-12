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

    // Natural, harmonious color gradient matching GT 5 HAL hardware profiles:
    // Clockwise from 3 o'clock:
    // 3 o'clock: White transition mark
    // 4 o'clock: Orange
    // 5:30 o'clock: Yellow
    // 7:30 o'clock: Green
    // 9:00 o'clock: Cyan / Light Blue
    // 10:30 o'clock: True Blue
    // 11:00 o'clock: Royal Ultramarine (ColorOS #73BBFF Always-On)
    // 11:30 o'clock: Electric Indigo (ColorOS #72BBFF)
    // 12:00 o'clock: Deep Velvet Violet (ColorOS #71BBFF)
    // 1:00 o'clock: Magenta / Pink (ColorOS #FFA8FF)
    // 2:15 o'clock: Red
    // 3:00 o'clock: White
    public static final int[] DEFAULT_COLORS = new int[]{
            0xFFFDFFFB, // 0: White at 3 o'clock (0°)
            0xFFFDFFFB, // 1: White margin (~11°)
            0xFFFF8800, // 2: Orange (~36°)
            0xFFFFEA00, // 3: Yellow (~75°)
            0xFF00E676, // 4: Green (~137°)
            0xFF00E5FF, // 5: Cyan / Light Blue (~184°)
            0xFF2979FF, // 6: True Blue (~230°)
            0xFF191CDD, // 7: Royal Ultramarine — ColorOS #73BBFF Always-On (~255°)
            0xFF5C27F5, // 8: Electric Indigo — ColorOS #72BBFF (~275°)
            0xFFA820FF, // 9: Deep Velvet Violet — ColorOS #71BBFF (~295°)
            0xFFFF2D7A, // 10: Magenta / Pink — ColorOS #FFA8FF (~322°)
            0xFFFF3B30, // 11: Red (~342°)
            0xFFFDFFFB, // 12: White margin (~351°)
            0xFFFDFFFB  // 13: White (360°)
    };

    public static final float[] GRADIENT_POSITIONS = new float[]{
            0.000f, // White (0°)
            0.030f, // White margin (~11°)
            0.100f, // Orange (~36°)
            0.210f, // Yellow (~75°)
            0.380f, // Green (~137°)
            0.510f, // Cyan / Light Blue (~184°)
            0.640f, // True Blue (~230°)
            0.708f, // Royal Ultramarine (~255°)
            0.764f, // Electric Indigo (~275°)
            0.820f, // Deep Velvet Violet (~295°)
            0.895f, // Magenta / Pink (~322°)
            0.950f, // Red (~342°)
            0.975f, // White margin (~351°)
            1.000f  // White (360°)
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
        mSelectedColor = color;
        mAngle = findAngleForColor(mSelectedColor);
        invalidate();
    }

    public int getColor() {
        return mSelectedColor;
    }

    /**
     * Linearly interpolates the exact color at any angle (0 .. 2*PI).
     * Guaranteed to match the visual pixel color drawn on the sweep gradient.
     */
    public static int getColorForAngle(float angleRad) {
        float f = (float) (angleRad / (2.0 * Math.PI));
        while (f < 0.0f) f += 1.0f;
        while (f >= 1.0f) f -= 1.0f;

        for (int i = 0; i < GRADIENT_POSITIONS.length - 1; i++) {
            float p0 = GRADIENT_POSITIONS[i];
            float p1 = GRADIENT_POSITIONS[i + 1];
            if (f >= p0 && f <= p1) {
                float t = (f - p0) / (p1 - p0);
                return interpolateColor(DEFAULT_COLORS[i], DEFAULT_COLORS[i + 1], t);
            }
        }
        return DEFAULT_COLORS[0];
    }

    public static int interpolateColor(int c0, int c1, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a0 = (c0 >> 24) & 0xFF;
        int r0 = (c0 >> 16) & 0xFF;
        int g0 = (c0 >> 8) & 0xFF;
        int b0 = c0 & 0xFF;

        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a = (int) (a0 + (a1 - a0) * t);
        int r = (int) (r0 + (r1 - r0) * t);
        int g = (int) (g0 + (g1 - g0) * t);
        int b = (int) (b0 + (b1 - b0) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int toNearestHardwareColor(int targetColor) {
        return targetColor;
    }

    public static float findAngleForColor(int targetColor) {
        int tr = (targetColor >> 16) & 0xFF;
        int tg = (targetColor >> 8) & 0xFF;
        int tb = targetColor & 0xFF;

        int bestDeg = 0;
        int minDistance = Integer.MAX_VALUE;

        for (int deg = 0; deg < 360; deg++) {
            float rad = (float) Math.toRadians(deg);
            int col = getColorForAngle(rad);
            int cr = (col >> 16) & 0xFF;
            int cg = (col >> 8) & 0xFF;
            int cb = col & 0xFF;

            int dr = tr - cr;
            int dg = tg - cg;
            int db = tb - cb;
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestDeg = deg;
                if (dist == 0) break;
            }
        }
        return (float) Math.toRadians(bestDeg);
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
        if (!isEnabled()) {
            return false;
        }
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
        mSelectedColor = getColorForAngle(mAngle);

        if (mListener != null) {
            mListener.onColorChanged(mSelectedColor, true);
        }

        invalidate();
    }
}
