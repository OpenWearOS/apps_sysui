package io.github.openwearos.sysui.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class SliderProgressDrawable extends Drawable {
	final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	final Paint mSeparatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	float mSeparatorCount;

	private final RectF mFillRect = new RectF();
	private float mFillAmount;

	public SliderProgressDrawable() {
		mSeparatorPaint.setStrokeWidth(1.0f);
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		Rect bounds = getBounds();
		float width = bounds.width();
		float height = bounds.height();
		if (width == 0.0f || height == 0.0f) {
			canvas.restore();
			return;
		}
		if (getLayoutDirection() == android.view.View.LAYOUT_DIRECTION_RTL) {
			canvas.scale(-1.0f, 1.0f, width / 2.0f, height / 2.0f);
		}
		canvas.drawPaint(mBackgroundPaint);
		mFillRect.right = mFillAmount * width;
		mFillRect.bottom = height;
		canvas.drawRect(mFillRect, mFillPaint);
		if (mSeparatorCount > 0.0f) {
			float step = width / mSeparatorCount;
			float x = step;
			for (int i = 0; i < mSeparatorCount; i++) {
				canvas.drawLine(x, 0.0f, x, height, mSeparatorPaint);
				x += step;
			}
		}
		canvas.restore();
	}

	public float getFillAmount() {
		return mFillAmount;
	}

	public void setFillAmount(float amount) {
		mFillAmount = Math.max(0.0f, Math.min(1.0f, amount));
		invalidateSelf();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
	}
}
