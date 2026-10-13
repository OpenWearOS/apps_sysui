package io.github.openwearos.sysui.ui;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class BackgroundGlowDrawable extends Drawable {
	private static final float[] STOPS = { 0f, 0.6f, 1f };
	private static final float[] STOP_ALPHA = { 0.6f, 0.2f, 0f };

	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private int mColor;
	private int mAlpha = 255;
	private ColorFilter mFilter;
	private ColorStateList mTint;
	private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN;

	public BackgroundGlowDrawable() {
		this(Ui.ACCENT);
	}

	public BackgroundGlowDrawable(int color) {
		mColor = color;
	}

	public void setColor(int color) {
		if (color == mColor)
			return;
		mColor = color;
		rebuildShader();
		invalidateSelf();
	}

	public int getColor() {
		return mColor;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		rebuildShader();
	}

	private void rebuildShader() {
		Rect b = getBounds();
		float radius = Math.min(b.width(), b.height()) / 2f;
		if (radius <= 0f) {
			mPaint.setShader(null);
			return;
		}
		int[] colors = new int[STOPS.length];
		for (int i = 0; i < colors.length; i++)
			colors[i] = scaleAlpha(mColor, STOP_ALPHA[i]);
		mPaint.setShader(new RadialGradient(b.exactCenterX(), b.exactCenterY(), radius,
				colors, STOPS, Shader.TileMode.CLAMP));
	}

	private static int scaleAlpha(int color, float factor) {
		return (Math.round(Color.alpha(color) * factor) << 24) | (color & 0x00FFFFFF);
	}

	@Override
	public void draw(Canvas canvas) {
		if (mPaint.getShader() == null)
			return;
		Rect b = getBounds();
		canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), Math.min(b.width(), b.height()) / 2f, mPaint);
	}

	@Override
	public void setAlpha(int alpha) {
		mAlpha = alpha;
		mPaint.setAlpha(alpha);
		invalidateSelf();
	}

	@Override
	public int getAlpha() {
		return mAlpha;
	}

	@Override
	public void setColorFilter(ColorFilter filter) {
		mFilter = filter;
		applyFilter();
	}

	@Override
	public ColorFilter getColorFilter() {
		return mPaint.getColorFilter();
	}

	@Override
	public void setTintList(ColorStateList tint) {
		mTint = tint;
		applyFilter();
	}

	@Override
	public void setTintMode(PorterDuff.Mode mode) {
		mTintMode = mode;
		applyFilter();
	}

	@Override
	public boolean isStateful() {
		return mTint != null && mTint.isStateful();
	}

	@Override
	protected boolean onStateChange(int[] state) {
		if (mTint == null)
			return false;
		applyFilter();
		return true;
	}

	private void applyFilter() {
		if (mTint != null) {
			int color = mTint.getColorForState(getState(), mTint.getDefaultColor());
			mPaint.setColorFilter(new PorterDuffColorFilter(color, mTintMode));
		} else {
			mPaint.setColorFilter(mFilter);
		}
		invalidateSelf();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}
