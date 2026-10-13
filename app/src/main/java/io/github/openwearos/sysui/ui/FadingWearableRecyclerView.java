package io.github.openwearos.sysui.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FadingWearableRecyclerView extends RecyclerView {
	private static final float EDGE_FRACTION = 0.22f;
	private static final float MIN_SCALE = 0.8f;
	private static final int ENTER_DURATION_MS = 225;
	private static final int ENTER_DELAY_MS = 75;
	private static final int ROTARY_SCROLL_TICK = 18;

	private boolean mEdgeFade = true;
	private boolean mEffectsApplied;
	private boolean mRotaryScroll = true;
	private boolean mRotaryActive;
	private boolean mHaptics = true;
	private boolean mCenterEdgeItems;
	private boolean mEnterOnAttach;
	private int mLastFirstPosition = NO_POSITION;

	private final ViewTreeObserver.OnPreDrawListener mPreDraw = () -> {
		updateEdgePadding();
		return true;
	};

	public FadingWearableRecyclerView(Context context) {
		this(context, null);
	}

	public FadingWearableRecyclerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FadingWearableRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setLayoutManager(new LinearLayoutManager(context));
		setItemAnimator(null);
		setOverScrollMode(OVER_SCROLL_NEVER);
	}

	public void setEdgeFadeEnabled(boolean enabled) {
		if (enabled == mEdgeFade)
			return;
		mEdgeFade = enabled;
		if (enabled)
			applyEdgeEffects();
		else
			resetChildren();
	}

	public void setRotaryScrollEnabled(boolean enabled) {
		mRotaryScroll = enabled;
	}

	public void setRotaryHapticsEnabled(boolean enabled) {
		mHaptics = enabled;
	}

	public void setCenterEdgeItems(boolean center) {
		if (center == mCenterEdgeItems)
			return;
		mCenterEdgeItems = center;
		setClipToPadding(!center);
		requestLayout();
	}

	public void setSideInsetFraction(float fraction) {
		int inset = Math.round(getResources().getDisplayMetrics().widthPixels
				* Math.max(0f, Math.min(0.5f, fraction)));
		setPaddingRelative(inset, getPaddingTop(), inset, getPaddingBottom());
	}

	public void setPlayEnterAnimationOnAttach(boolean play) {
		mEnterOnAttach = play;
	}

	public void playEnterAnimation() {
		int distance = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
		setTranslationY(distance);
		setAlpha(0f);
		ObjectAnimator move = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, distance, 0f);
		move.setDuration(ENTER_DURATION_MS);
		move.setInterpolator(new PathInterpolator(0.2f, 0.2f, 0f, 1f));
		ObjectAnimator fade = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f);
		fade.setDuration(ENTER_DURATION_MS);
		fade.setInterpolator(new PathInterpolator(0.33f, 0f, 0.67f, 1f));
		AnimatorSet set = new AnimatorSet();
		set.playTogether(move, fade);
		set.setStartDelay(ENTER_DELAY_MS);
		set.start();
	}

	private void applyEdgeEffects() {
		int height = getHeight();
		if (!mEdgeFade || height <= 0)
			return;
		float zone = height * EDGE_FRACTION;
		mEffectsApplied = true;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			float t = Math.min(child.getBottom(), height - child.getTop()) / zone;
			t = Math.max(0f, Math.min(1f, t));
			float scale = MIN_SCALE + (1f - MIN_SCALE) * t;
			child.setAlpha(t);
			child.setScaleX(scale);
			child.setScaleY(scale);
		}
	}

	private void resetChildren() {
		for (int i = 0; i < getChildCount(); i++)
			resetChild(getChildAt(i));
	}

	private static void resetChild(View child) {
		child.setAlpha(1f);
		child.setScaleX(1f);
		child.setScaleY(1f);
	}

	@Override
	public void onScrolled(int dx, int dy) {
		super.onScrolled(dx, dy);
		applyEdgeEffects();
		tickWhenItemChanges();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		applyEdgeEffects();
	}

	@Override
	public void onChildAttachedToWindow(View child) {
		super.onChildAttachedToWindow(child);
		if (!mEdgeFade && mEffectsApplied)
			resetChild(child);
	}

	private void updateEdgePadding() {
		Adapter<?> adapter = getAdapter();
		if (!mCenterEdgeItems || adapter == null || getChildCount() == 0 || getHeight() <= 0)
			return;
		View first = getChildAt(0);
		View last = getChildAt(getChildCount() - 1);
		int top = getPaddingTop();
		int bottom = getPaddingBottom();
		if (getChildAdapterPosition(first) == 0) {
			top = Math.max(0, (getHeight() - first.getHeight()) / 2);
		}
		if (getChildAdapterPosition(last) == adapter.getItemCount() - 1) {
			bottom = Math.max(0, (getHeight() - last.getHeight()) / 2);
		}
		if (top != getPaddingTop() || bottom != getPaddingBottom()) {
			setPadding(getPaddingLeft(), top, getPaddingRight(), bottom);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnPreDrawListener(mPreDraw);
		if (mEnterOnAttach)
			playEnterAnimation();
	}

	@Override
	protected void onDetachedFromWindow() {
		getViewTreeObserver().removeOnPreDrawListener(mPreDraw);
		super.onDetachedFromWindow();
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mRotaryScroll
				&& event.getAction() == MotionEvent.ACTION_SCROLL
				&& event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
			float pixels = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
					* ViewConfiguration.get(getContext()).getScaledVerticalScrollFactor();
			mRotaryActive = true;
			scrollBy(0, Math.round(pixels));
			return true;
		}
		return super.onGenericMotionEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
			mRotaryActive = false;
		return super.onTouchEvent(event);
	}

	private void tickWhenItemChanges() {
		if (!mHaptics || !mRotaryActive || getChildCount() == 0)
			return;
		int first = getChildAdapterPosition(getChildAt(0));
		if (first == mLastFirstPosition)
			return;
		boolean wasKnown = mLastFirstPosition != NO_POSITION;
		mLastFirstPosition = first;
		if (wasKnown) {
			performHapticFeedback(Build.VERSION.SDK_INT >= 34
					? ROTARY_SCROLL_TICK
					: HapticFeedbackConstants.CLOCK_TICK);
		}
	}
}
