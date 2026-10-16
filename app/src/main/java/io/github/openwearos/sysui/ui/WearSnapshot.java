package io.github.openwearos.sysui.ui;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewOverlay;

public final class WearSnapshot {
	private static final Rect sTmpRect = new Rect();

	private final BitmapDrawable mDrawable;
	private final View mView;

	public WearSnapshot(BitmapDrawable drawable, View view) {
		mView = view;
		mDrawable = drawable;
	}

	public BitmapDrawable getDrawable() {
		return mDrawable;
	}

	public ViewOverlay getOverlay() {
		return ((View) mView.getParent()).getOverlay();
	}

	public void remove() {
		getOverlay().remove(mDrawable);
	}

	public int getAlpha() {
		return mDrawable.getAlpha();
	}

	public void setAlpha(int alpha) {
		mDrawable.setAlpha(alpha);
		Rect rect = sTmpRect;
		mView.getHitRect(rect);
		int top = rect.top;
		int left = rect.left;
		int right = rect.right;
		mDrawable.copyBounds(rect);
		if (mView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
			left = right - rect.width();
		}
		rect.offsetTo(left, top);
		mDrawable.setBounds(rect);
	}
}
