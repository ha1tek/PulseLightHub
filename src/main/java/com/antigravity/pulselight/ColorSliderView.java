package com.antigravity.pulselight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
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

    public static class ColorItem {
        public final int id;
        public final boolean isGradient;
        public final int startColor;
        public final int endColor;
        public final String name;

        public ColorItem(int id, int solidColor, String name) {
            this.id = id;
            this.isGradient = false;
            this.startColor = solidColor;
            this.endColor = solidColor;
            this.name = name;
        }

        public ColorItem(int id, int startColor, int endColor, String name) {
            this.id = id;
            this.isGradient = true;
            this.startColor = startColor;
            this.endColor = endColor;
            this.name = name;
        }
    }

    public static final int COLS = 5;
    public static final int ROWS = 3;
    private int mRows = ROWS;
    private int mDeviceModel = DeviceModelManager.MODEL_GT_5;

    // 15-color palette (3 rows x 5 cols):
    // Row 1 (0..4): 5 single colors (Белый, Неоновый Розовый, Красный, Оранжевый, Желтый)
    // Row 2 (5..9): 5 single colors (Зеленый, Изумрудный, Голубой, Синий, Фиолетовый)
    // Row 3 (10..14): 5 dual-tone gradients grouped together
    public static final ColorItem[] ITEMS = new ColorItem[]{
            // --- РЯД 1: 5 ОДНОТОННЫХ ЦВЕТОВ ---
            // 0: Белый (White) - ЗАФИКСИРОВАН
            new ColorItem(0xFFFDFFFB, 0xFFFDFFFB, "Белый"),
            // 1: Неоновый Розовый (Pure Pink)
            new ColorItem(0xFFFFA7FF, 0xFFFFA7FF, "Неоновый Розовый"),
            // 2: Красный (Racing Red) - ЗАФИКСИРОВАН
            new ColorItem(0xFFFF3B30, 0xFFFF3B30, "Красный"),
            // 3: Оранжевый (Cyber Amber) - ЗАФИКСИРОВАН
            new ColorItem(0xFFFF9500, 0xFFFF9500, "Оранжевый"),
            // 4: Желтый (Neon Yellow) - ЗАФИКСИРОВАН
            new ColorItem(0xFFFFEA00, 0xFFFFEA00, "Желтый"),

            // --- РЯД 2: 5 ОДНОТОННЫХ ЦВЕТОВ ---
            // 5: Зеленый (Matrix Green) - ЗАФИКСИРОВАН
            new ColorItem(0xFF00E676, 0xFF00E676, "Зеленый"),
            // 6: Изумрудный / Бирюзовый (Teal Green)
            new ColorItem(0xFF00FF1B, 0xFF00E5A3, "Изумрудный"),
            // 7: Голубой (Realme GT Blue / Cyan) - ЗАФИКСИРОВАН
            new ColorItem(0xFF71BBFF, 0xFF71BBFF, "Голубой"),
            // 8: Синий (Electric Blue) - ЗАФИКСИРОВАН
            new ColorItem(0xFF2979FF, 0xFF2979FF, "Синий"),
            // 9: Фиолетовый (GT Purple) - ЗАФИКСИРОВАН
            new ColorItem(0xFFA820FF, 0xFFA820FF, "Фиолетовый"),

            // --- РЯД 3: 5 ДВУХЦВЕТНЫХ ГРАДИЕНТОВ (СОБРАНЫ ВМЕСТЕ) ---
            // 10: Розовый двухцветный: Cyber Pink и Deep Blue Gradient
            new ColorItem(0xFFFFFFF0, 0xFFFF2D7A, 0xFF3D5AFE, "Розовый: градиент"),
            // 11: Оранжево-Розовый дуэт (Orange-Pink Gradient)
            new ColorItem(0xFFFFFFF1, 0xFFFF9500, 0xFFFF2D7A, "Оранжево-Розовый"),
            // 12: Сине-Желтый дуэт (Blue-Yellow Gradient)
            new ColorItem(0xFFFFFFF2, 0xFF2979FF, 0xFFFFEA00, "Сине-Желтый"),
            // 13: Аква-Мята дуэт (Cyan-Mint Gradient)
            new ColorItem(0xFFFFFFF3, 0xFF00E5FF, 0xFF00E676, "Аква-Мята"),
            // 14: Золотой Лайм дуэт (Gold-Lime Gradient)
            new ColorItem(0xFFFFFFF4, 0xFFFFD600, 0xFF76FF03, "Золотой Лайм")
    };

    public static final int[] PALETTE = new int[ITEMS.length];
    static {
        for (int i = 0; i < ITEMS.length; i++) {
            PALETTE[i] = ITEMS[i].id;
        }
    }

    public static boolean isGradient(int color) {
        for (ColorItem it : ITEMS) {
            if (it.id == color) return it.isGradient;
        }
        return false;
    }

    public static int[] getGradientForColor(int color) {
        for (ColorItem it : ITEMS) {
            if (it.id == color && it.isGradient) {
                return new int[]{ it.startColor, it.endColor };
            }
        }
        return new int[]{ color, color };
    }

    public static String getColorName(int color) {
        for (ColorItem it : ITEMS) {
            if (it.id == color) return it.name;
        }
        return String.format("#%06X", (0xFFFFFF & color));
    }

    private OnColorChangeListener mListener;

    private final Paint mSegmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mSegRect = new RectF();
    private final float[] mAnimFractions = new float[ITEMS.length];
    private ValueAnimator mAnimator = null;

    private int mSelectedIndex = 7; // Default to Голубой (index 7)
    private int mSelectedColor = ITEMS[7].id;

    public ColorSliderView(Context context) {
        super(context);
        init(context);
    }

    public ColorSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColorSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mSegmentPaint.setStyle(Paint.Style.FILL);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        float density = getResources().getDisplayMetrics().density;
        mBorderPaint.setStrokeWidth(1.2f * density);
        mBorderPaint.setColor(0x33FFFFFF);

        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(2.5f * density);
        mHighlightPaint.setColor(Color.WHITE);

        mShadowPaint.setStyle(Paint.Style.FILL);

        mSelectedIndex = 7; // Голубой
        mSelectedColor = ITEMS[mSelectedIndex].id;
        mAnimFractions[mSelectedIndex] = 1.0f;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mListener = listener;
    }

    public void setDeviceModel(int model) {
        mDeviceModel = model;
        if (model == DeviceModelManager.MODEL_GT_NEO_5) {
            mRows = 2;
            if (mSelectedIndex >= 10) {
                selectIndexAnimated(7, true); // Revert gradient to single color (Голубой)
            }
        } else {
            mRows = ROWS;
        }
        requestLayout();
        invalidate();
    }

    public int getDeviceModel() {
        return mDeviceModel;
    }

    public void setColor(int color) {
        int bestIndex = 7;
        int minDistance = Integer.MAX_VALUE;
        int tr = (color >> 16) & 0xFF;
        int tg = (color >> 8) & 0xFF;
        int tb = color & 0xFF;

        int limit = Math.min(ITEMS.length, mRows * COLS);
        for (int i = 0; i < limit; i++) {
            if (ITEMS[i].id == color) {
                bestIndex = i;
                break;
            }
            int c = ITEMS[i].startColor;
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
            mSelectedColor = ITEMS[mSelectedIndex].id;
            invalidate();
        }
    }

    public int getColor() {
        return mSelectedColor;
    }

    private void selectIndexAnimated(int newIndex, boolean fromUser) {
        if (newIndex < 0 || newIndex >= ITEMS.length) return;
        final int prevIndex = mSelectedIndex;
        mSelectedIndex = newIndex;
        mSelectedColor = ITEMS[mSelectedIndex].id;

        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        final float startPrev = mAnimFractions[prevIndex];
        final float startNew = mAnimFractions[newIndex];

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(180);
        mAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        mAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            for (int i = 0; i < ITEMS.length; i++) {
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        float padY = 4f * density;
        float gapY = 6f * density;
        float tileH = 36f * density;
        int desiredHeight = Math.round(2 * padY + mRows * tileH + (mRows - 1) * gapY);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight = desiredHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(desiredHeight, heightSize);
        }

        setMeasuredDimension(width, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int count = Math.min(ITEMS.length, mRows * COLS);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float padX = 2f * density;
        float padY = 4f * density;
        float gapX = 6f * density;
        float gapY = 6f * density;

        float availableW = w - 2 * padX - (COLS - 1) * gapX;
        float segW = availableW / COLS;

        float availableH = h - 2 * padY - (mRows - 1) * gapY;
        float segH = availableH / mRows;

        float baseCorner = 8f * density;

        // 1. Draw non-selected segments first
        for (int i = 0; i < count; i++) {
            if (i == mSelectedIndex) continue;
            float f = mAnimFractions[i];
            drawTile(canvas, i, f, padX, padY, gapX, gapY, segW, segH, baseCorner, density);
        }

        // 2. Draw selected segment on top with elevated pop and highlight
        if (mSelectedIndex < count) {
            float selectedF = mAnimFractions[mSelectedIndex];
            drawTile(canvas, mSelectedIndex, selectedF, padX, padY, gapX, gapY, segW, segH, baseCorner, density);
        }
    }

    private void drawTile(Canvas canvas, int index, float f,
                          float padX, float padY, float gapX, float gapY,
                          float segW, float segH, float baseCorner, float density) {
        int row = index / COLS;
        int col = index % COLS;

        float left = padX + col * (segW + gapX);
        float top = padY + row * (segH + gapY);
        float right = left + segW;
        float bottom = top + segH;

        float expand = 1.5f * density * f;
        mSegRect.set(left - expand, top - expand, right + expand, bottom + expand);

        // Soft elevated drop shadow under active tile
        if (f > 0.05f) {
            mShadowPaint.setColor(Color.argb((int) (80 * f), 0, 0, 0));
            mShadowPaint.setShadowLayer(6f * density * f, 0, 2f * density * f, 0x99000000);
            canvas.drawRoundRect(mSegRect, baseCorner, baseCorner, mShadowPaint);
        }

        // Body: Gradient or Solid Color
        ColorItem item = ITEMS[index];
        if (item.isGradient) {
            LinearGradient grad = new LinearGradient(
                    mSegRect.left, mSegRect.top,
                    mSegRect.right, mSegRect.bottom,
                    item.startColor, item.endColor,
                    Shader.TileMode.CLAMP
            );
            mSegmentPaint.setShader(grad);
        } else {
            mSegmentPaint.setShader(null);
            mSegmentPaint.setColor(item.startColor);
        }
        canvas.drawRoundRect(mSegRect, baseCorner, baseCorner, mSegmentPaint);
        mSegmentPaint.setShader(null);

        // Crisp white elevated highlight on active popped tile
        if (f > 0.05f) {
            mHighlightPaint.setAlpha((int) (255 * f));
            mHighlightPaint.setStrokeWidth(2.2f * density);
            mHighlightPaint.setShadowLayer(4f * density * f, 0, 1.5f * density, 0x66000000);
            canvas.drawRoundRect(mSegRect, baseCorner, baseCorner, mHighlightPaint);
        }
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
                handleTouchPosition(x, y, true);
                return true;

            case MotionEvent.ACTION_MOVE:
                handleTouchPosition(x, y, false);
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

    private void handleTouchPosition(float touchX, float touchY, boolean isDown) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float padX = 2f * density;
        float padY = 4f * density;
        float gapX = 6f * density;
        float gapY = 6f * density;

        float availableW = w - 2 * padX - (COLS - 1) * gapX;
        float segW = availableW / COLS;
        float slotW = segW + gapX;

        float availableH = h - 2 * padY - (mRows - 1) * gapY;
        float segH = availableH / mRows;
        float slotH = segH + gapY;

        int col = (int) ((touchX - padX) / slotW);
        int row = (int) ((touchY - padY) / slotH);

        col = Math.max(0, Math.min(COLS - 1, col));
        row = Math.max(0, Math.min(mRows - 1, row));

        int targetIndex = row * COLS + col;
        int limit = Math.min(ITEMS.length, mRows * COLS);
        targetIndex = Math.max(0, Math.min(limit - 1, targetIndex));

        if (targetIndex != mSelectedIndex) {
            selectIndexAnimated(targetIndex, true);
        } else if (isDown) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (mListener != null) {
                mListener.onColorChanged(mSelectedColor, true);
            }
        }
    }
}
