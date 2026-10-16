package io.github.openwearos.sysui.ui;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.core.graphics.ColorUtils;

import io.github.openwearos.sysui.R;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class WearPageIndicatorDrawable extends Drawable {
	private static final float MAX_PAGE_POS_TO_CENTER_DISTANCE = 0.5f;
	private static final int MAX_VISIBLE_INDICATORS = 6;
	private static final float OVERFLOW_FADEOUT_LENGTH = 6.0f;
	private static final float ROUND_DOT_SPACING_DEGREES = 6.6f;

	private final Paint mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final State mState = new State();
	private CanvasTransformer mTransformer;
	private int mDotRadius;
	private float mSelectedAlpha;
	private float mUnselectedAlpha;

	public void setPageCount(int count) {
		mState.pageCount = count;
		mState.allVisible = count <= MAX_VISIBLE_INDICATORS;
		mState.minCenter = 2.5f;
		mState.maxCenter = (count - 1) - 2.5f;
		invalidateSelf();
	}

	public void setPagePosition(float position) {
		mState.pagePosition = position;
		invalidateSelf();
	}

	private boolean needsMirroring() {
		return isAutoMirrored() && getLayoutDirection() == android.view.View.LAYOUT_DIRECTION_RTL;
	}

	private float dotRadius(int index) {
		State s = mState;
		if (s.allVisible) {
			return mDotRadius;
		}
		float edgeFade = Math.abs(index - s.center) - 3.0f;
		float edgeScale = clamp(1.0f - (edgeFade + edgeFade), 0.0f, 1.0f);
		float overflowScale = clamp(1.0f - ((Math.abs(index - s.pagePosition) - 1.0f) / OVERFLOW_FADEOUT_LENGTH),
				0.0f, 1.0f);
		return mDotRadius * edgeScale * overflowScale;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private void updateWindow() {
		State s = mState;
		float position = clamp(s.pagePosition, 0.0f, s.pageCount - 1);
		s.pagePosition = position;
		if (s.allVisible) {
			s.center = (s.pageCount - 1) / 2.0f;
		} else {
			float center = s.center;
			float lower = center - 0.5f;
			float upper = center + MAX_PAGE_POS_TO_CENTER_DISTANCE;
			if (position < lower) {
				center = position + MAX_PAGE_POS_TO_CENTER_DISTANCE;
				s.direction = State.DIRECTION_BACKWARD;
			} else if (position > upper) {
				center = position - 0.5f;
				s.direction = State.DIRECTION_FORWARD;
			} else if (center != s.snapTarget) {
				if (s.direction == State.DIRECTION_FORWARD) {
					center = Math.max(position - 0.5f, s.lowSnap);
				} else if (s.direction == State.DIRECTION_BACKWARD) {
					center = Math.min(position + MAX_PAGE_POS_TO_CENTER_DISTANCE, s.highSnap);
				}
			}
			s.center = clamp(center, s.minCenter, s.maxCenter);
		}
		float floor = (float) Math.floor(s.center);
		float low = (s.center - floor > MAX_PAGE_POS_TO_CENTER_DISTANCE) ? floor + MAX_PAGE_POS_TO_CENTER_DISTANCE
				: floor - 0.5f;
		s.lowSnap = low;
		s.highSnap = low + 1.0f;
		s.snapTarget = Math.abs(s.center - low) < Math.abs(s.center - s.highSnap) ? s.lowSnap : s.highSnap;
		if (s.allVisible) {
			s.firstVisible = 0;
			s.lastVisible = s.pageCount - 1;
		} else {
			s.firstVisible = (int) clamp((float) Math.floor(s.center - 3.0f), 0.0f, s.pageCount - 1);
			s.lastVisible = (int) clamp((float) Math.ceil(s.center + 3.0f), 0.0f, s.pageCount - 1);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (mTransformer == null || mState.pageCount <= 1) {
			return;
		}
		updateWindow();
		int first = mState.firstVisible;
		int last = mState.lastVisible;
		float center = mState.center;
		float position = needsMirroring() ? last - mState.pagePosition : mState.pagePosition;

		canvas.save();
		mTransformer.start(getBounds(), canvas, first, center);
		for (int i = first; i <= last; i++) {
			float selection = 1.0f - Math.min(1.0f, Math.abs(position - i));
			float alpha = mUnselectedAlpha + ((mSelectedAlpha - mUnselectedAlpha) * selection);
			mIndicatorPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, (int) alpha));
			canvas.drawCircle(0.0f, 0.0f, dotRadius(i), mIndicatorPaint);
			mTransformer.advance(getBounds(), canvas);
		}
		canvas.restore();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public boolean isAutoMirrored() {
		return true;
	}

	@Override
	public void inflate(Resources resources, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme)
			throws XmlPullParserException, IOException {
		super.inflate(resources, parser, attrs, theme);
		TypedArray a = theme != null
				? theme.obtainStyledAttributes(attrs, R.styleable.WearPageIndicatorDrawable, 0, 0)
				: resources.obtainAttributes(attrs, R.styleable.WearPageIndicatorDrawable);
		mDotRadius = a.getDimensionPixelSize(R.styleable.WearPageIndicatorDrawable_radius, 0);
		mSelectedAlpha = readAlpha(a, R.styleable.WearPageIndicatorDrawable_selectedAlpha);
		mUnselectedAlpha = readAlpha(a, R.styleable.WearPageIndicatorDrawable_unselectedAlpha);
		float margin = a.getDimensionPixelSize(R.styleable.WearPageIndicatorDrawable_margin, 0);
		if (resources.getConfiguration().isScreenRound()) {
			mTransformer = new RoundTransformer(mDotRadius, margin);
		} else {
			mTransformer = new RectangularTransformer(mDotRadius,
					resources.getDimensionPixelOffset(R.dimen.wear_page_indicator_rectangular_dot_distance), margin);
		}
		a.recycle();
	}

	private static int readAlpha(TypedArray a, int index) {
		TypedValue value = new TypedValue();
		if (!a.getValue(index, value)) {
			return 0;
		}
		return value.type == TypedValue.TYPE_FLOAT ? (int) value.getFloat() : value.data;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
	}

	private static final class State {
		static final int DIRECTION_FORWARD = 1;
		static final int DIRECTION_BACKWARD = 2;

		boolean allVisible;
		int pageCount;
		int firstVisible;
		int lastVisible;
		float pagePosition;
		float center;
		float maxCenter;
		float minCenter;
		float snapTarget;
		float lowSnap;
		float highSnap;
		int direction = DIRECTION_BACKWARD;
	}

	private interface CanvasTransformer {
		void start(Rect bounds, Canvas canvas, int firstIndex, float center);

		void advance(Rect bounds, Canvas canvas);
	}

	private static final class RectangularTransformer implements CanvasTransformer {
		private final float mDotSpacing;
		private final float mDotRadius;
		private final float mBottomMargin;

		RectangularTransformer(float dotRadius, float dotSpacing, float bottomMargin) {
			mDotSpacing = dotSpacing;
			mDotRadius = dotRadius;
			mBottomMargin = bottomMargin;
		}

		@Override
		public void start(Rect bounds, Canvas canvas, int firstIndex, float center) {
			canvas.translate((bounds.width() / 2.0f) - ((center - firstIndex) * mDotSpacing),
					(bounds.height() - mBottomMargin) - (mDotRadius / 2.0f));
		}

		@Override
		public void advance(Rect bounds, Canvas canvas) {
			canvas.translate(mDotSpacing, 0.0f);
		}
	}

	private static final class RoundTransformer implements CanvasTransformer {
		private final float mDotRadius;
		private final float mMargin;

		RoundTransformer(float dotRadius, float margin) {
			mDotRadius = dotRadius;
			mMargin = margin;
		}

		private float orbitRadius(Rect bounds) {
			return (Math.min(bounds.width() / 2, bounds.height() / 2) - mMargin) - (mDotRadius / 2.0f);
		}

		@Override
		public void start(Rect bounds, Canvas canvas, int firstIndex, float center) {
			float orbit = orbitRadius(bounds);
			canvas.translate(bounds.width() / 2.0f, bounds.height() / 2.0f);
			canvas.rotate(-((firstIndex - center) * ROUND_DOT_SPACING_DEGREES));
			canvas.translate(0.0f, orbit);
		}

		@Override
		public void advance(Rect bounds, Canvas canvas) {
			float orbit = orbitRadius(bounds);
			canvas.translate(0.0f, -orbit);
			canvas.rotate(-ROUND_DOT_SPACING_DEGREES);
			canvas.translate(0.0f, orbit);
		}
	}
}
