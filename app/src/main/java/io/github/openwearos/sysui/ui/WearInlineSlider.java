package io.github.openwearos.sysui.ui;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.github.openwearos.sysui.R;

public final class WearInlineSlider extends LinearLayout {
	private static final int MAX_SEPARATED_STEPS = 8;

	private float mStep;
	private float mMin;
	private float mMax;
	private float mValue;
	private WearInlineSliderPreference mPreference;
	private boolean mShowSeparators;
	private boolean mShowMinimum;

	private final ImageView mIncrementView;
	private final ImageView mDecrementView;
	private final ImageView mProgressView;
	private final SliderProgressDrawable mProgress = new SliderProgressDrawable();
	private final StepRepeater mRepeater;
	private final ObjectAnimator mFillAnimator;
	private final Runnable mUpdateRunnable = this::update;

	public WearInlineSlider(Context context) {
		this(context, null);
	}

	public WearInlineSlider(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.inlineSliderStyle);
	}

	public WearInlineSlider(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, R.style.Widget_InlineSlider_Default);
	}

	public WearInlineSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setOrientation(HORIZONTAL);
		setClipToOutline(true);
		Context ctx = getContext();
		LayoutInflater.from(ctx).inflate(R.layout.wear_inline_slider, this, true);
		mIncrementView = findViewById(R.id.inline_slider_increment);
		mDecrementView = findViewById(R.id.inline_slider_decrement);
		mProgressView = findViewById(R.id.inline_slider_progress);

		final int cornerRadius = ctx.getResources().getDimensionPixelSize(R.dimen.inline_slider_progress_corner_radius);
		mProgressView.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
			}
		});
		mProgressView.setClipToOutline(true);
		mProgressView.setImageDrawable(mProgress);

		mRepeater = new StepRepeater(mIncrementView, mDecrementView);
		mRepeater.setCallback(new StepRepeater.Callback() {
			@Override
			public boolean decrement() {
				if (mValue == mMin) {
					return false;
				}
				setValueInternal(clamp(mValue - mStep, mMin, mMax), false);
				return true;
			}

			@Override
			public boolean increment() {
				if (mValue == mMax) {
					return false;
				}
				setValueInternal(clamp(mValue + mStep, mMin, mMax), false);
				return true;
			}
		});

		mFillAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(ctx, R.animator.wear_slider_value_transition);
		mFillAnimator.setTarget(mProgress);

		TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.WearInlineSliderView, defStyleAttr, defStyleRes);
		mRepeater.setAllowLongPress(a.getBoolean(R.styleable.WearInlineSliderView_allowLongPress, false));
		setMinValue(a.getFloat(R.styleable.WearInlineSliderView_android_valueFrom, 0.0f));
		setMaxValue(a.getFloat(R.styleable.WearInlineSliderView_android_valueTo, 1.0f));
		setStep(a.getFloat(R.styleable.WearInlineSliderView_android_stepSize, 0.0f));
		setValue(a.getFloat(R.styleable.WearInlineSliderView_android_value, 0.0f));
		mProgress.mFillPaint.setColor(a.getColor(R.styleable.WearInlineSliderView_filledBarColor, 0));
		mProgress.mSeparatorPaint.setColor(a.getColor(R.styleable.WearInlineSliderView_separatorColor, 0));
		setShowIncrementSeparators(a.getBoolean(R.styleable.WearInlineSliderView_showIncrementSeparators, false));
		setShowMinimumIncrement(a.getBoolean(R.styleable.WearInlineSliderView_showMinimumIncrement, false));
		mIncrementView.setImageDrawable(a.getDrawable(R.styleable.WearInlineSliderView_incrementDrawable));
		mDecrementView.setImageDrawable(a.getDrawable(R.styleable.WearInlineSliderView_decrementDrawable));
		mProgress.mBackgroundPaint.setColor(a.getColor(R.styleable.WearInlineSliderView_baseBarColor, 0));
		setDecrementDescription(a.getString(R.styleable.WearInlineSliderView_decrementContentDescription));
		setIncrementDescription(a.getString(R.styleable.WearInlineSliderView_incrementContentDescription));
		setProgressDescription(a.getString(R.styleable.WearInlineSliderView_progressContentDescription));
		a.recycle();
		update();
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private void scheduleUpdate() {
		removeCallbacks(mUpdateRunnable);
		post(mUpdateRunnable);
	}

	private void update() {
		mMax = Math.max(mMin, mMax);
		if (mStep <= 0.0f || (mMax - mMin) % mStep > 0.0f) {
			mStep = mMax - mMin;
		}
		int separators = 0;
		if (mShowSeparators) {
			float range = mMax - mMin;
			float steps = range / mStep;
			if (steps <= MAX_SEPARATED_STEPS && range % mStep == 0.0f) {
				separators = (int) steps;
				if (mShowMinimum) {
					separators++;
				}
			}
		}
		setValueInternal(mValue, true);
		mProgress.mSeparatorCount = separators;
		mProgress.invalidateSelf();
		mRepeater.reset();
	}

	private void setValueInternal(float requested, boolean force) {
		float previous = mValue;
		if (mStep > 0.0f) {
			int steps = Math.round((requested - mMin) / mStep);
			mValue = clamp((steps * mStep) + mMin, mMin, mMax);
		} else {
			mValue = clamp(requested, mMin, mMax);
		}
		boolean changed = mValue != previous;

		WearInlineSliderPreference pref = mPreference;
		if (pref != null && changed && pref.getValue() != mValue) {
			if (pref.callChangeListener(mValue)) {
				pref.setValue(mValue, false);
			} else {
				setValue(pref.getValue());
			}
			pref.notifyUserChanged();
		}

		if (force || changed) {
			float current = mProgress.getFillAmount();
			float base = mShowMinimum ? mMin - mStep : mMin;
			float range = mMax - base;
			float target = range != 0.0f ? (mValue - base) / range : 0.0f;
			if (current != target) {
				if (isLaidOut()) {
					mFillAnimator.cancel();
					mFillAnimator.setFloatValues(mProgress.getFillAmount(), target);
					mFillAnimator.start();
				} else {
					mProgress.setFillAmount(target);
				}
			}
		}
		invalidate();
	}

	void setPreference(WearInlineSliderPreference preference) {
		mPreference = preference;
	}

	public float getValue() {
		return mValue;
	}

	public void setValue(float value) {
		if (mValue != value) {
			setValueInternal(value, false);
		}
	}

	public void setMinValue(float min) {
		if (mMin != min) {
			mMin = min;
			scheduleUpdate();
		}
	}

	public void setMaxValue(float max) {
		if (mMax != max) {
			mMax = max;
			scheduleUpdate();
		}
	}

	public void setStep(float step) {
		if (mStep != step) {
			mStep = step;
			scheduleUpdate();
		}
	}

	public void setShowIncrementSeparators(boolean show) {
		if (mShowSeparators != show) {
			mShowSeparators = show;
			scheduleUpdate();
		}
	}

	public void setShowMinimumIncrement(boolean show) {
		if (mShowMinimum != show) {
			mShowMinimum = show;
			scheduleUpdate();
		}
	}

	public void setDecrementDescription(CharSequence description) {
		mDecrementView.setContentDescription(description);
	}

	public void setIncrementDescription(CharSequence description) {
		mIncrementView.setContentDescription(description);
	}

	public void setProgressDescription(CharSequence description) {
		mProgressView.setContentDescription(description);
	}
}
