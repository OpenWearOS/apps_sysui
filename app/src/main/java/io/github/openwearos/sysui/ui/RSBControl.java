package io.github.openwearos.sysui.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import io.github.openwearos.sysui.R;

public class RSBControl extends View {
	public static final int FADE_DURATION_MS = ViewConfiguration.getScrollBarFadeDuration();
	private static final float MIN_FADED_SCALE = 0.65f;
	private static final long VALUE_ANIMATION_MS = 450L;
	private static final TimeInterpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);
	private static final int TYPE_SLIDER = 0;
	private static final int TYPE_SELECTOR = 1;

	private int mMax = 10;
	private int mValue = 0;
	private float mDisplayedValue = 0.0f;

	private final AnimatorSet mFadeSet = new AnimatorSet();
	private final ValueAnimator mValueAnimator = new ValueAnimator();
	private final ObjectAnimator mScaleAnimator = new ObjectAnimator();
	private final ObjectAnimator mAlphaAnimator = new ObjectAnimator();
	private final Animator.AnimatorListener mHideListener = new AnimatorListenerAdapter() {
		@Override
		public void onAnimationEnd(Animator animation) {
			scheduleHide();
		}
	};
	private Renderer mRenderer;

	public RSBControl(Context context) {
		this(context, null);
	}

	public RSBControl(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.rsbControlStyle);
	}

	public RSBControl(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RSBControl, defStyleAttr, 0);
		try {
			int type = a.getInt(R.styleable.RSBControl_rsbControlType, TYPE_SLIDER);
			mRenderer = type == TYPE_SELECTOR ? new SelectorRenderer(context) : new SliderRenderer(context);
			mRenderer.sweepDegrees = a.getFloat(R.styleable.RSBControl_sweepDegrees, 51.0f);
			int color = a.getColor(R.styleable.RSBControl_color, context.getColor(R.color.rsb_default_color));
			mMax = a.getInt(R.styleable.RSBControl_maxValue, 10);
			mRenderer.setColor(color);
			mRenderer.setMax(mMax);
		} finally {
			a.recycle();
		}

		mValueAnimator.setInterpolator(INTERPOLATOR);
		mValueAnimator.addUpdateListener(animation -> {
			mDisplayedValue = (Float) animation.getAnimatedValue();
			invalidate();
		});
		mScaleAnimator.setProperty(View.SCALE_Y);
		mScaleAnimator.setTarget(this);
		mAlphaAnimator.setProperty(View.ALPHA);
		mAlphaAnimator.setTarget(this);
		mFadeSet.setInterpolator(INTERPOLATOR);
		setAlpha(0.0f);
	}

	public void show() {
		if (mFadeSet.isRunning()) {
			mFadeSet.cancel();
		}
		float remaining = 1.0f - getAlpha();
		mAlphaAnimator.setFloatValues(getAlpha(), 1.0f);
		mScaleAnimator.setFloatValues(Math.max(MIN_FADED_SCALE, getScaleY()), 1.0f);
		mFadeSet.addListener(mHideListener);
		mFadeSet.setStartDelay(0L);
		mFadeSet.setDuration((long) (remaining * FADE_DURATION_MS));
		mFadeSet.playTogether(mAlphaAnimator, mScaleAnimator);
		mFadeSet.start();
	}

	private void scheduleHide() {
		if (mFadeSet.isRunning()) {
			mFadeSet.cancel();
		}
		float duration = getAlpha() * FADE_DURATION_MS;
		long delay = ViewConfiguration.getScrollDefaultDelay();
		mAlphaAnimator.setFloatValues(getAlpha(), 0.0f);
		mScaleAnimator.setFloatValues(getScaleY(), MIN_FADED_SCALE);
		mFadeSet.removeListener(mHideListener);
		mFadeSet.setStartDelay(delay * 4);
		mFadeSet.setDuration((long) duration);
		mFadeSet.playTogether(mAlphaAnimator, mScaleAnimator);
		mFadeSet.start();
	}

	public void setMaxValue(int max) {
		if (mMax != max) {
			mMax = max;
			mRenderer.setMax(max);
		}
	}

	public void setValue(int value, boolean animate) {
		int clamped = Math.max(0, Math.min(mMax, value));
		if (clamped == mValue) {
			return;
		}
		mValue = clamped;
		if (animate) {
			show();
			mValueAnimator.setFloatValues(mDisplayedValue, mValue);
			mValueAnimator.setDuration(VALUE_ANIMATION_MS);
			mValueAnimator.start();
		} else {
			mDisplayedValue = clamped;
		}
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mRenderer != null) {
			mRenderer.draw(canvas, mDisplayedValue);
		}
	}

	private abstract static class Renderer {
		final float inset;
		final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final RectF arcBounds = new RectF();
		int color;
		float sweepDegrees = 51.0f;
		int max = 10;

		Renderer(Context context) {
			color = context.getColor(R.color.rsb_default_color);
			int trackColor = context.getColor(R.color.rsb_track_color);
			float stroke = context.getResources().getDimension(R.dimen.rsb_stroke);
			valuePaint.setStrokeWidth(stroke);
			trackPaint.setStrokeWidth(stroke);
			inset = context.getResources().getDimension(R.dimen.rsb_outside_padding) + (stroke / 2.0f);
			trackPaint.setStrokeCap(Paint.Cap.ROUND);
			trackPaint.setStyle(Paint.Style.STROKE);
			valuePaint.setStrokeCap(Paint.Cap.ROUND);
			valuePaint.setStyle(Paint.Style.STROKE);
			valuePaint.setColor(color);
			trackPaint.setColor(trackColor);
		}

		abstract float startAngle(float value);

		abstract float sweepAngle(float value);

		abstract void onMaxChanged();

		final void setColor(int newColor) {
			if (color != newColor) {
				color = newColor;
				valuePaint.setColor(newColor);
			}
		}

		final void setMax(int newMax) {
			max = newMax;
			onMaxChanged();
		}

		final void draw(Canvas canvas, float value) {
			float width = canvas.getWidth();
			float height = canvas.getHeight();
			arcBounds.set(inset, inset, width - inset, height - inset);
			canvas.drawArc(arcBounds, -(sweepDegrees / 2.0f), sweepDegrees, false, trackPaint);
			canvas.drawArc(arcBounds, startAngle(value), -sweepAngle(value), false, valuePaint);
		}
	}

	private static final class SliderRenderer extends Renderer {
		private float mDegreesPerStep = 0.0f;

		SliderRenderer(Context context) {
			super(context);
		}

		@Override
		float startAngle(float value) {
			return sweepDegrees / 2.0f;
		}

		@Override
		float sweepAngle(float value) {
			return value * mDegreesPerStep;
		}

		@Override
		void onMaxChanged() {
			mDegreesPerStep = sweepDegrees / max;
		}
	}

	private static final class SelectorRenderer extends Renderer {
		private float mThumbDegrees = 0.0f;

		SelectorRenderer(Context context) {
			super(context);
		}

		@Override
		float startAngle(float value) {
			return (sweepDegrees / 2.0f) - (((sweepDegrees - mThumbDegrees) / max) * value);
		}

		@Override
		float sweepAngle(float value) {
			return mThumbDegrees;
		}

		@Override
		void onMaxChanged() {
			mThumbDegrees = Math.max(sweepDegrees / (max + 1), sweepDegrees * 0.1f);
		}
	}
}
