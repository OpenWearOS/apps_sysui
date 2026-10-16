package io.github.openwearos.sysui.ui;

import android.animation.TimeAnimator;
import android.view.MotionEvent;
import android.view.View;

public final class StepRepeater {
	public interface Callback {
		boolean decrement();

		boolean increment();
	}

	private static final long REPEAT_INTERVAL_MS = 60;

	private final TimeAnimator mAnimator = new TimeAnimator();
	private boolean mAllowLongPress;
	private Callback mCallback;
	private boolean mRepeatingIncrement;
	private boolean mRepeatingDecrement;
	private long mLastStepTime;

	public StepRepeater(View incrementView, View decrementView) {
		mAnimator.setTimeListener((animator, totalTime, deltaTime) -> {
			if (totalTime - mLastStepTime <= REPEAT_INTERVAL_MS) {
				return;
			}
			mLastStepTime = totalTime;
			if (mCallback == null) {
				return;
			}
			boolean stepped = mRepeatingIncrement ? mCallback.increment() : mCallback.decrement();
			if (!stepped) {
				reset();
			}
		});

		incrementView.setOnClickListener(v -> {
			if (mCallback != null) {
				mCallback.increment();
			}
		});
		incrementView.setOnLongClickListener(v -> {
			if (mAllowLongPress) {
				mRepeatingIncrement = true;
				update();
			}
			return true;
		});
		incrementView.setOnTouchListener((v, event) -> {
			if (isRelease(event)) {
				mRepeatingIncrement = false;
				update();
			}
			return false;
		});

		decrementView.setOnClickListener(v -> {
			if (mCallback != null) {
				mCallback.decrement();
			}
		});
		decrementView.setOnLongClickListener(v -> {
			if (mAllowLongPress) {
				mRepeatingDecrement = true;
				update();
			}
			return true;
		});
		decrementView.setOnTouchListener((v, event) -> {
			if (isRelease(event)) {
				mRepeatingDecrement = false;
				update();
			}
			return false;
		});
	}

	private static boolean isRelease(MotionEvent event) {
		return event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	public void setAllowLongPress(boolean allow) {
		mAllowLongPress = allow;
		reset();
	}

	public void reset() {
		mRepeatingIncrement = false;
		mRepeatingDecrement = false;
		if (mAnimator.isStarted()) {
			mAnimator.cancel();
		}
	}

	private void update() {
		if (!mAllowLongPress) {
			return;
		}
		if (!mRepeatingIncrement && !mRepeatingDecrement) {
			mAnimator.cancel();
		} else {
			mLastStepTime = 0L;
			mAnimator.start();
		}
	}
}
