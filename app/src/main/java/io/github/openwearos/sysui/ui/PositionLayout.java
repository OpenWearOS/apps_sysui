package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class PositionLayout extends ViewGroup {
	public interface TouchInterceptor {
		boolean onInterceptTouch(MotionEvent event);
	}

	private TouchInterceptor mInterceptor;

	public PositionLayout(Context context) {
		super(context);
	}

	public PositionLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PositionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setTouchInterceptor(TouchInterceptor interceptor) {
		mInterceptor = interceptor;
	}

	public void setPosition(View child, float x, float y) {
		if (child.getParent() != this) {
			throw new IllegalArgumentException("View is not a child of this PositionLayout");
		}
		LayoutParams lp = (LayoutParams) child.getLayoutParams();
		lp.x = x;
		lp.y = y;
		child.offsetLeftAndRight(Math.round(x) - child.getLeft());
		child.offsetTopAndBottom(Math.round(y) - child.getTop());
		invalidate();
	}

	@Override
	protected void measureChild(View child, int widthSpec, int heightSpec) {
		LayoutParams lp = (LayoutParams) child.getLayoutParams();
		child.measure(
				getChildMeasureSpec(widthSpec, lp.leftMargin + lp.rightMargin, lp.width),
				getChildMeasureSpec(heightSpec, lp.topMargin + lp.bottomMargin, lp.height));
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		int width = 0;
		int height = 0;
		int state = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == GONE)
				continue;
			measureChild(child, widthSpec, heightSpec);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			width = Math.max(width, Math.round(lp.x) + child.getMeasuredWidth() + lp.rightMargin);
			height = Math.max(height, Math.round(lp.y) + child.getMeasuredHeight() + lp.bottomMargin);
			state = combineMeasuredStates(state, child.getMeasuredState());
		}
		setMeasuredDimension(
				resolveSizeAndState(Math.max(width, getSuggestedMinimumWidth()), widthSpec, state),
				resolveSizeAndState(Math.max(height, getSuggestedMinimumHeight()), heightSpec,
						state << MEASURED_HEIGHT_STATE_SHIFT));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == GONE)
				continue;
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			int left = Math.round(lp.x);
			int top = Math.round(lp.y);
			child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return mInterceptor != null ? mInterceptor.onInterceptTouch(event) : super.onInterceptTouchEvent(event);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		if (p instanceof ViewGroup.MarginLayoutParams) {
			return new LayoutParams((ViewGroup.MarginLayoutParams) p);
		}
		return new LayoutParams(p);
	}

	public static class LayoutParams extends ViewGroup.MarginLayoutParams {
		public float x;
		public float y;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(int width, int height, float x, float y) {
			super(width, height);
			this.x = x;
			this.y = y;
		}

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
			copyPosition(source);
		}

		public LayoutParams(ViewGroup.MarginLayoutParams source) {
			super(source);
			copyPosition(source);
		}

		private void copyPosition(ViewGroup.LayoutParams source) {
			if (source instanceof LayoutParams) {
				x = ((LayoutParams) source).x;
				y = ((LayoutParams) source).y;
			}
		}
	}
}
