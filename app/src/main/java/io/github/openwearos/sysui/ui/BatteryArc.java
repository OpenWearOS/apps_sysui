package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class BatteryArc extends View {
    private static final int LOW_PERCENT = 15;
    private static final int LOW_COLOR = 0xFFE57373;
    private static final int TRACK_COLOR = 0x33FFFFFF;
    private static final float TOP = -90f;

    private final Paint mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private int mPercent = 100;
    private boolean mCharging;

    BatteryArc(Context context) {
        super(context);
        float stroke = Ui.dp(context, 3);
        for (Paint p : new Paint[] { mTrack, mArc }) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        mTrack.setColor(TRACK_COLOR);
        updateColor();
    }

    void setBattery(int percent, boolean charging) {
        mPercent = Math.max(0, Math.min(100, percent));
        mCharging = charging;
        updateColor();
        invalidate();
    }

    void setHidden(boolean hidden) {
        setVisibility(hidden ? INVISIBLE : VISIBLE);
    }

    private void updateColor() {
        if (mCharging) {
            mArc.setColor(Ui.ACCENT);
        } else if (mPercent <= LOW_PERCENT) {
            mArc.setColor(LOW_COLOR);
        } else {
            mArc.setColor(Ui.TEXT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = mArc.getStrokeWidth() * 2f;
        mBounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(mBounds, 0f, 360f, false, mTrack);
        canvas.drawArc(mBounds, TOP, 360f * mPercent / 100f, false, mArc);
    }
}