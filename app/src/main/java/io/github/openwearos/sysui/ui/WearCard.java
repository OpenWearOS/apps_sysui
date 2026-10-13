package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

public final class WearCard extends FrameLayout {
	private static final float START_ALPHA = 0.16f;
	private static final float END_ALPHA = 0.08f;
	private static final int DEFAULT_CORNER_DP = 26;

	private final GradientDrawable mBackground = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
			new int[] { 0, 0 });
	private int mCornerRadius;

	public WearCard(Context context) {
		this(context, null);
	}

	public WearCard(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WearCard(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mCornerRadius = Ui.dp(context, DEFAULT_CORNER_DP);
		setBackground(mBackground);
		setCardColors(Color.WHITE, Color.WHITE);
		setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
			}
		});
		setClipToOutline(true);
	}

	public void setCardColors(int startColor, int endColor) {
		mBackground.setColors(new int[] {
				withAlpha(startColor, START_ALPHA), withAlpha(endColor, END_ALPHA) });
	}

	public void setCardColor(int color) {
		setCardColors(color, color);
	}

	public void setCornerRadius(int radiusPx) {
		mCornerRadius = Math.max(0, radiusPx);
		invalidateOutline();
	}

	private static int withAlpha(int color, float alpha) {
		return (Math.round(alpha * 255f) << 24) | (color & 0x00FFFFFF);
	}
}
