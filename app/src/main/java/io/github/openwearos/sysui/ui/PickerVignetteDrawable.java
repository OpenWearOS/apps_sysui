package io.github.openwearos.sysui.ui;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import androidx.core.graphics.ColorUtils;

import io.github.openwearos.sysui.R;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class PickerVignetteDrawable extends DrawableWrapper {
	private static final int DEFAULT_VIGNETTE_COLOR = 0xFF000000;
	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	private final Rect mEndGradientRect = new Rect();
	private final Rect mStartGradientRect = new Rect();
	private final int[] mGradientColors = new int[2];
	private final GradientDrawable mGradientDrawable = new GradientDrawable();
	private int mEndOfClearArea = -1;
	private int mStartOfClearArea = -1;
	private boolean mIsVertical = true;
	private int mVignetteAlpha;
	private ColorStateList mVignetteColor;

	public PickerVignetteDrawable() {
		super(null);
		mGradientDrawable.setColors(mGradientColors);
		setDrawable(mGradientDrawable);
	}

	public static PickerVignetteDrawable create(int orientation) {
		PickerVignetteDrawable drawable = new PickerVignetteDrawable();
		drawable.mIsVertical = orientation == VERTICAL;
		drawable.setVignetteColor(ColorStateList.valueOf(DEFAULT_VIGNETTE_COLOR));
		return drawable;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void updateEndGradientColor() {
		mGradientColors[1] = ColorUtils.setAlphaComponent(mGradientColors[0], mVignetteAlpha);
	}

	private void applyColors() {
		mGradientDrawable.mutate();
		mGradientDrawable.setColors(mGradientColors);
	}

	private boolean handleColorChange(int[] state) {
		int color = getVignetteColor().getColorForState(state, DEFAULT_VIGNETTE_COLOR);
		int current = mGradientColors[0];
		if (current != 0 && current == color) {
			return false;
		}
		mGradientColors[0] = color;
		updateEndGradientColor();
		applyColors();
		return true;
	}

	private void ensureClearArea(int fallback) {
		if (mStartOfClearArea < 0) {
			mStartOfClearArea = fallback;
		}
		if (mEndOfClearArea < 0) {
			mEndOfClearArea = fallback;
		}
	}

	private void updateGradientRects(Rect bounds) {
		if (bounds.isEmpty()) {
			return;
		}
		int left = bounds.left;
		int top = bounds.top;
		int right = bounds.right;
		int bottom = bounds.bottom;
		if (mIsVertical) {
			ensureClearArea((top + bottom) / 2);
			mStartGradientRect.set(left, top, right, clamp(mStartOfClearArea, top, bottom));
			mEndGradientRect.set(left, clamp(mEndOfClearArea, top, bottom), right, bottom);
		} else {
			ensureClearArea((left + right) / 2);
			mStartGradientRect.set(left, top, clamp(mStartOfClearArea, left, right), bottom);
			mEndGradientRect.set(clamp(mEndOfClearArea, left, right), top, right, bottom);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (mStartGradientRect.isEmpty() && mEndGradientRect.isEmpty()) {
			return;
		}
		if (!mStartGradientRect.isEmpty()) {
			mGradientDrawable.setOrientation(mIsVertical
					? GradientDrawable.Orientation.TOP_BOTTOM
					: GradientDrawable.Orientation.LEFT_RIGHT);
			mGradientDrawable.setBounds(mStartGradientRect);
			mGradientDrawable.draw(canvas);
		}
		if (!mEndGradientRect.isEmpty()) {
			mGradientDrawable.setOrientation(mIsVertical
					? GradientDrawable.Orientation.BOTTOM_TOP
					: GradientDrawable.Orientation.RIGHT_LEFT);
			mGradientDrawable.setBounds(mEndGradientRect);
			mGradientDrawable.draw(canvas);
		}
	}

	Rect getStartGradientRect() {
		return mStartGradientRect;
	}

	Rect getEndGradientRect() {
		return mEndGradientRect;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	public int getOrientation() {
		return mIsVertical ? VERTICAL : HORIZONTAL;
	}

	public int getVignetteAlpha() {
		return mVignetteAlpha;
	}

	public void setVignetteAlpha(int alpha) {
		mVignetteAlpha = alpha;
		if (mGradientColors[0] != 0) {
			updateEndGradientColor();
		}
		applyColors();
		invalidateSelf();
	}

	public ColorStateList getVignetteColor() {
		if (mVignetteColor == null) {
			mVignetteColor = ColorStateList.valueOf(DEFAULT_VIGNETTE_COLOR);
		}
		return mVignetteColor;
	}

	public void setVignetteColor(ColorStateList color) {
		mVignetteColor = color;
		handleColorChange(getState());
		invalidateSelf();
	}

	public void setClearArea(int start, int end) {
		if (mStartOfClearArea == start && mEndOfClearArea == end) {
			return;
		}
		mStartOfClearArea = start;
		mEndOfClearArea = end;
		updateGradientRects(getBounds());
		invalidateSelf();
	}

	@Override
	public void inflate(Resources resources, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme)
			throws XmlPullParserException, IOException {
		super.inflate(resources, parser, attrs, theme);
		TypedArray a = theme != null
				? theme.obtainStyledAttributes(attrs, R.styleable.PickerVignetteDrawable, 0, 0)
				: resources.obtainAttributes(attrs, R.styleable.PickerVignetteDrawable);
		mIsVertical = a.getInt(R.styleable.PickerVignetteDrawable_android_orientation, VERTICAL) == VERTICAL;
		ColorStateList color = a.getColorStateList(R.styleable.PickerVignetteDrawable_vignetteColor);
		if (color != null) {
			setVignetteColor(color);
		}
		a.recycle();
	}

	@Override
	public void invalidateDrawable(Drawable who) {
		if (who == mGradientDrawable) {
			return;
		}
		Drawable.Callback callback = getCallback();
		if (callback != null) {
			callback.invalidateDrawable(who);
		}
	}

	@Override
	public boolean isStateful() {
		return getVignetteColor().isStateful();
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		updateGradientRects(bounds);
	}

	@Override
	protected boolean onStateChange(int[] state) {
		return super.onStateChange(state) || handleColorChange(state);
	}
}
