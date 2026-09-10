package com.antigravity.pulselight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ColorSwatchView extends View {

    private int mColor = GlyphColorManager.DEFAULT_BLUE;
    private boolean mIsSelected = false;

    private Paint mFillPaint;
    private Paint mRingPaint;
    private Paint mInnerRingPaint;

    public ColorSwatchView(Context context) {
        super(context);
        init();
    }

    public ColorSwatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorSwatchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);

        mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(2.5f * getResources().getDisplayMetrics().density);
        mRingPaint.setColor(Color.WHITE);

        mInnerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerRingPaint.setStyle(Paint.Style.STROKE);
        mInnerRingPaint.setStrokeWidth(2.0f * getResources().getDisplayMetrics().density);
        mInnerRingPaint.setColor(Color.parseColor("#0B0C0E"));
    }

    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    public int getColor() {
        return mColor;
    }

    public void setSelectedState(boolean selected) {
        mIsSelected = selected;
        invalidate();
    }

    public boolean isSelectedState() {
        return mIsSelected;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 4f;

        if (mIsSelected) {
            // Draw outer white selection ring
            canvas.drawCircle(cx, cy, radius, mRingPaint);
            // Gap ring
            canvas.drawCircle(cx, cy, radius - 3f, mInnerRingPaint);
            // Core color circle
            mFillPaint.setColor(mColor);
            canvas.drawCircle(cx, cy, radius - 6f, mFillPaint);
        } else {
            mFillPaint.setColor(mColor);
            canvas.drawCircle(cx, cy, radius * 0.82f, mFillPaint);
        }
    }
}
