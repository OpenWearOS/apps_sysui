package io.github.openwearos.sysui.ui;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;

public final class ProgressSpinnerDrawable extends Drawable implements Animatable {
	public enum Direction {
		CLOCKWISE, COUNTER_CLOCKWISE
	}

	private static final int LEVEL_RANGE = 10000;
	private static final float RADIANS_TO_DEGREES = 57.29578f;
	private static final float TOP = -90f;
	private static final float TINY_SWEEP = 0.01f;
	private static final int TRACK_ALPHA = 26;
	private static final int FALLBACK_TRACK = 0x1AFFFFFF;
	private static final long SPIN_PERIOD_MS = 1000L;

	private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Rect mSquare = new Rect();
	private final RectF mArc = new RectF();

	private ColorStateList mProgressColor = ColorStateList.valueOf(Color.WHITE);
	private ColorStateList mTrackColor;
	private int mProgressNow = Color.WHITE;
	private int mTrackNow = FALLBACK_TRACK;
	private int mAlpha = 255;

	private float mTrackWidth;
	private float mTrackStart = 0f;
	private float mTrackEnd = 360f;
	private float mStartAngle;
	private float mRotation;
	private boolean mShowEmpty;
	private Direction mDirection = Direction.CLOCKWISE;
	private int mGravity = Gravity.CENTER;

	private ValueAnimator mSpin;
	private boolean mResumeSpinWhenVisible;

	public ProgressSpinnerDrawable() {
		mTrackWidth = Resources.getSystem().getDisplayMetrics().density * 3f;
		mProgressPaint.setStyle(Paint.Style.STROKE);
		mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
		mTrackPaint.setStyle(Paint.Style.STROKE);
		mTrackPaint.setStrokeWidth(mTrackWidth);
		updateColors(getState());
	}

	public void setProgress(float fraction) {
		setLevel(Math.round(Math.max(0f, Math.min(1f, fraction)) * LEVEL_RANGE));
	}

	public void setSweepAngle(float degrees) {
		float max = maxSweep();
		setLevel(max <= 0f ? 0 : Math.round(Math.min(Math.abs(degrees), max) / max * LEVEL_RANGE));
	}

	public float getSweepAngle() {
		return Math.min(Math.abs(getLevel()), LEVEL_RANGE) * maxSweep() / LEVEL_RANGE;
	}

	public void setStartAngle(float degrees) {
		float max = maxSweep();
		float v = max > 0f ? degrees % max : 0f;
		if (v != mStartAngle) {
			mStartAngle = v;
			invalidateSelf();
		}
	}

	public float getStartAngle() {
		return mStartAngle;
	}

	public void setRotation(float degrees) {
		float v = degrees % 360f;
		if (v != mRotation) {
			mRotation = v;
			invalidateSelf();
		}
	}

	public float getRotation() {
		return mRotation;
	}

	public void setTrackStartAngle(float degrees) {
		if (degrees != mTrackStart) {
			mTrackStart = degrees;
			invalidateSelf();
		}
	}

	public void setTrackEndAngle(float degrees) {
		if (degrees != mTrackEnd) {
			mTrackEnd = degrees;
			invalidateSelf();
		}
	}

	public void setTrackWidth(float px) {
		float v = Math.max(0f, px);
		if (v == mTrackWidth)
			return;
		mTrackWidth = v;
		mTrackPaint.setStrokeWidth(v);
		updateArc();
		invalidateSelf();
	}

	public void setDirection(Direction direction) {
		if (direction != mDirection) {
			mDirection = direction;
			invalidateSelf();
		}
	}

	public void setGravity(int gravity) {
		if (gravity != mGravity) {
			mGravity = gravity;
			onBoundsChange(getBounds());
			invalidateSelf();
		}
	}

	public void showEmptySweepAngle(boolean show) {
		if (show != mShowEmpty) {
			mShowEmpty = show;
			invalidateSelf();
		}
	}

	public void setProgressColor(ColorStateList color) {
		mProgressColor = color;
		if (updateColors(getState()))
			invalidateSelf();
	}

	public void setProgressColor(int color) {
		setProgressColor(ColorStateList.valueOf(color));
	}

	public void setTrackColor(ColorStateList color) {
		mTrackColor = color;
		if (updateColors(getState()))
			invalidateSelf();
	}

	public void setTrackColor(int color) {
		setTrackColor(ColorStateList.valueOf(color));
	}

	private float maxSweep() {
		return Math.abs(mTrackEnd - mTrackStart);
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		int side = Math.min(bounds.width(), bounds.height());
		Gravity.apply(mGravity, side, side, bounds, mSquare);
		updateArc();
	}

	private void updateArc() {
		float inset = mTrackWidth / 2f;
		mArc.set(mSquare.left + inset, mSquare.top + inset, mSquare.right - inset, mSquare.bottom - inset);
	}

	@Override
	public void draw(Canvas canvas) {
		float max = maxSweep();
		float radius = mArc.width() / 2f;
		if (!isVisible() || max <= 0f || radius <= 0f || mTrackWidth <= 0f)
			return;

		boolean ring = max >= 360f;
		int saved = canvas.save();
		canvas.rotate(mRotation, mArc.centerX(), mArc.centerY());

		mTrackPaint.setStrokeCap(ring ? Paint.Cap.BUTT : Paint.Cap.ROUND);
		mTrackPaint.setColor(withAlpha(mTrackNow));
		canvas.drawArc(mArc, Math.min(mTrackStart, mTrackEnd) + TOP, max, false, mTrackPaint);

		drawProgress(canvas, max, radius, ring);
		canvas.restoreToCount(saved);
	}

	private void drawProgress(Canvas canvas, float max, float radius, boolean ring) {
		float sweep = getSweepAngle();

		float arcLength = (float) Math.toRadians(sweep) * radius;
		float width = mShowEmpty ? mTrackWidth : mTrackWidth * Math.min(1f, arcLength / mTrackWidth);
		if (width <= 0f)
			return;
		mProgressPaint.setStrokeWidth(width);
		mProgressPaint.setColor(withAlpha(mProgressNow));

		float capDegrees = mTrackWidth / radius * RADIANS_TO_DEGREES;
		float closing = ring ? 1f - sweep / max : 0f;
		float drawn = sweep - capDegrees * closing;
		float offset = capDegrees * closing / 2f;
		if (drawn <= 0f) {
			drawn = TINY_SWEEP;
			offset = sweep / 2f;
		}

		boolean clockwise = mDirection == Direction.CLOCKWISE;
		float origin = clockwise
				? Math.min(mTrackStart, mTrackEnd) + mStartAngle
				: Math.max(mTrackStart, mTrackEnd) - mStartAngle;
		float from = clockwise ? origin + offset : origin - offset - drawn;
		canvas.drawArc(mArc, from + TOP, drawn, false, mProgressPaint);
	}

	private int withAlpha(int color) {
		return (Color.alpha(color) * mAlpha / 255 << 24) | (color & 0x00FFFFFF);
	}

	@Override
	protected boolean onLevelChange(int level) {
		invalidateSelf();
		return true;
	}

	@Override
	public boolean isStateful() {
		return mProgressColor.isStateful() || (mTrackColor != null && mTrackColor.isStateful());
	}

	@Override
	protected boolean onStateChange(int[] state) {
		return updateColors(state);
	}

	private boolean updateColors(int[] state) {
		int progress = mProgressColor.getColorForState(state, Color.WHITE);
		ColorStateList trackSource = mTrackColor != null
				? mTrackColor
				: mProgressColor.withAlpha(TRACK_ALPHA);
		int track = trackSource.getColorForState(state, FALLBACK_TRACK);
		boolean changed = progress != mProgressNow || track != mTrackNow;
		mProgressNow = progress;
		mTrackNow = track;
		return changed;
	}

	@Override
	public void setAlpha(int alpha) {
		if (alpha != mAlpha) {
			mAlpha = alpha;
			invalidateSelf();
		}
	}

	@Override
	public int getAlpha() {
		return mAlpha;
	}

	@Override
	public void setColorFilter(ColorFilter filter) {
		mProgressPaint.setColorFilter(filter);
		mTrackPaint.setColorFilter(filter);
		invalidateSelf();
	}

	@Override
	public ColorFilter getColorFilter() {
		return mProgressPaint.getColorFilter();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void start() {
		mResumeSpinWhenVisible = true;
		if (mSpin == null) {
			mSpin = ValueAnimator.ofFloat(0f, 360f);
			mSpin.setDuration(SPIN_PERIOD_MS);
			mSpin.setInterpolator(new LinearInterpolator());
			mSpin.setRepeatCount(ValueAnimator.INFINITE);
			mSpin.addUpdateListener(a -> setRotation((Float) a.getAnimatedValue()));
		}
		if (isVisible() && !mSpin.isRunning())
			mSpin.start();
	}

	@Override
	public void stop() {
		mResumeSpinWhenVisible = false;
		if (mSpin != null)
			mSpin.cancel();
	}

	@Override
	public boolean isRunning() {
		return mSpin != null && mSpin.isRunning();
	}

	@Override
	public boolean setVisible(boolean visible, boolean restart) {
		boolean changed = super.setVisible(visible, restart);
		if (!visible) {
			if (mSpin != null)
				mSpin.cancel();
		} else if (mResumeSpinWhenVisible) {
			start();
		}
		return changed;
	}
}
