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

public class ThemePaletteView extends View {

    public interface OnColorSelectedListener {
        void onColorSelected(int color, String name);
    }

    private ThemeManager.ColorOption[] mOptions = new ThemeManager.ColorOption[0];
    private int mSelectedColor = 0;
    private int mSelectedIndex = -1;
    private float[] mAnimScales = new float[0];
    private OnColorSelectedListener mListener;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mItemRect = new RectF();

    public ThemePaletteView(Context context) {
        super(context);
        init();
    }

    public ThemePaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemePaletteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(0xFF2C3242);
        mStrokePaint.setStrokeWidth(dp(1.5f));

        mIndicatorPaint.setStyle(Paint.Style.STROKE);
        mIndicatorPaint.setStrokeWidth(dp(2.5f));
    }

    public void setOptions(ThemeManager.ColorOption[] options, int selectedColor) {
        mOptions = options != null ? options : new ThemeManager.ColorOption[0];
        mSelectedColor = selectedColor;
        mAnimScales = new float[mOptions.length];
        mSelectedIndex = -1;
        for (int i = 0; i < mOptions.length; i++) {
            if (mOptions[i].color == selectedColor) {
                mSelectedIndex = i;
                mAnimScales[i] = 1.0f;
            } else {
                mAnimScales[i] = 0.0f;
            }
        }
        invalidate();
    }

    public void setSelectedColor(int color) {
        mSelectedColor = color;
        for (int i = 0; i < mOptions.length; i++) {
            if (mOptions[i].color == color) {
                animateSelection(i);
                break;
            }
        }
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        mListener = listener;
    }

    private void animateSelection(int newIndex) {
        if (newIndex == mSelectedIndex && mSelectedIndex >= 0) return;
        final int oldIndex = mSelectedIndex;
        mSelectedIndex = newIndex;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(180);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            if (oldIndex >= 0 && oldIndex < mAnimScales.length) {
                mAnimScales[oldIndex] = 1f - v;
            }
            if (newIndex >= 0 && newIndex < mAnimScales.length) {
                mAnimScales[newIndex] = v;
            }
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = (int) dp(52);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int finalHeight = desiredHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(desiredHeight, heightSize);
        }
        setMeasuredDimension(width, finalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOptions.length == 0) return;

        int count = mOptions.length;
        float w = getWidth();
        float h = getHeight();
        float padding = dp(4);
        float availableWidth = w - padding * 2;
        float itemWidth = availableWidth / count;
        float itemHeight = h - padding * 2;
        float radius = Math.min(itemWidth, itemHeight) * 0.42f;

        for (int i = 0; i < count; i++) {
            float cx = padding + itemWidth * i + itemWidth / 2f;
            float cy = h / 2f;

            mPaint.setColor(mOptions[i].color);
            mPaint.setStyle(Paint.Style.FILL);

            // Draw base color circle
            canvas.drawCircle(cx, cy, radius, mPaint);
            canvas.drawCircle(cx, cy, radius, mStrokePaint);

            // Selection indicator ring
            float scale = (i < mAnimScales.length) ? mAnimScales[i] : 0f;
            if (scale > 0.01f || i == mSelectedIndex) {
                float ringRadius = radius + dp(3.5f) * Math.max(scale, (i == mSelectedIndex ? 1f : 0f));
                int optCol = mOptions[i].color;
                boolean isWhiteish = Color.red(optCol) > 230 && Color.green(optCol) > 230 && Color.blue(optCol) > 230;
                mIndicatorPaint.setColor(isWhiteish ? 0xFF00F0FF : 0xFFFFFFFF);
                mIndicatorPaint.setAlpha((int) (240 * Math.max(scale, (i == mSelectedIndex ? 1f : 0f))));
                canvas.drawCircle(cx, cy, ringRadius, mIndicatorPaint);
            }
        }
    }

    private float mDownX, mDownY;
    private boolean mIsDownInView = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mDownX = event.getX();
            mDownY = event.getY();
            mIsDownInView = true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dy = Math.abs(event.getY() - mDownY);
            if (dy > dp(10)) {
                mIsDownInView = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            if (mIsDownInView) {
                float x = event.getX();
                int count = mOptions.length;
                if (count > 0) {
                    float padding = dp(4);
                    float itemWidth = (getWidth() - padding * 2) / count;
                    int clickedIndex = (int) ((x - padding) / itemWidth);
                    if (clickedIndex >= 0 && clickedIndex < count) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        mSelectedColor = mOptions[clickedIndex].color;
                        animateSelection(clickedIndex);
                        if (mListener != null) {
                            mListener.onColorSelected(mSelectedColor, mOptions[clickedIndex].name);
                        }
                    }
                }
            }
            mIsDownInView = false;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mIsDownInView = false;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
