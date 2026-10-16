package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import io.github.openwearos.sysui.R;

public class WearFullscreenSlider extends ConstraintLayout {
	private final WearCircularButton mIncrement;
	private final WearCircularButton mDecrement;
	private final TextView mText;
	private final StepRepeater mRepeater;
	private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = () -> post(this::updateTouchDelegate);
	private Rect mIncrementRegion = new Rect();
	private Rect mDecrementRegion = new Rect();

	public WearFullscreenSlider(Context context) {
		this(context, null);
	}

	public WearFullscreenSlider(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.fullscreenSliderStyle);
	}

	public WearFullscreenSlider(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, R.style.Widget_FullscreenSlider_Default);
	}

	public WearFullscreenSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		LayoutInflater.from(context).inflate(R.layout.wear_fullscreen_slider, this);
		mIncrement = findViewById(R.id.wear_fullscreen_slider_increment);
		mDecrement = findViewById(R.id.wear_fullscreen_slider_decrement);
		mText = findViewById(R.id.wear_fullscreen_slider_text);
		mRepeater = new StepRepeater(mIncrement, mDecrement);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearFullscreenSliderView, defStyleAttr,
				defStyleRes);
		mRepeater.setAllowLongPress(a.getBoolean(R.styleable.WearFullscreenSliderView_allowLongPress, false));
		mIncrement.setIcon(a.getDrawable(R.styleable.WearFullscreenSliderView_incrementDrawable));
		mDecrement.setIcon(a.getDrawable(R.styleable.WearFullscreenSliderView_decrementDrawable));
		mDecrement.setContentDescription(a.getString(R.styleable.WearFullscreenSliderView_decrementContentDescription));
		mIncrement.setContentDescription(a.getString(R.styleable.WearFullscreenSliderView_incrementContentDescription));
		ColorStateList tint = a.getColorStateList(R.styleable.WearFullscreenSliderView_buttonIconTint);
		if (tint != null) {
			mIncrement.setIconTint(tint);
			mDecrement.setIconTint(tint);
		}
		a.recycle();
	}

	public void setStepCallback(StepRepeater.Callback callback) {
		mRepeater.setCallback(callback);
	}

	public TextView getTextView() {
		return mText;
	}

	public WearCircularButton getIncrementButton() {
		return mIncrement;
	}

	public WearCircularButton getDecrementButton() {
		return mDecrement;
	}

	public Rect getDecrementRegion() {
		return new Rect(0, mText.getBottom(), getMeasuredWidth(), getMeasuredHeight());
	}

	public Rect getIncrementRegion() {
		return new Rect(0, 0, getMeasuredWidth(), mText.getTop());
	}

	private void updateTouchDelegate() {
		Rect increment = getIncrementRegion();
		Rect decrement = getDecrementRegion();
		if (mIncrementRegion.equals(increment) && mDecrementRegion.equals(decrement)) {
			return;
		}
		setTouchDelegate(new SplitTouchDelegate(this, increment, decrement));
		mIncrementRegion = increment;
		mDecrementRegion = decrement;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
	}

	@Override
	protected void onDetachedFromWindow() {
		getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
		super.onDetachedFromWindow();
	}

	private static final class SplitTouchDelegate extends TouchDelegate {
		private final TouchDelegate mIncrement;
		private final TouchDelegate mDecrement;

		SplitTouchDelegate(WearFullscreenSlider slider, Rect incrementRegion, Rect decrementRegion) {
			super(new Rect(), slider);
			mIncrement = new TouchDelegate(incrementRegion, slider.mIncrement);
			mDecrement = new TouchDelegate(decrementRegion, slider.mDecrement);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();
			if (mIncrement.onTouchEvent(event)) {
				return true;
			}
			event.setLocation(x, y);
			return mDecrement.onTouchEvent(event);
		}

		@Override
		public boolean onTouchExplorationHoverEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();
			if (mIncrement.onTouchExplorationHoverEvent(event)) {
				return true;
			}
			event.setLocation(x, y);
			return mDecrement.onTouchExplorationHoverEvent(event);
		}
	}
}
