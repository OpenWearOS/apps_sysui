package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class WearCircularButton extends FrameLayout {
	public enum Size {
		XSMALL(32, 16),
		SMALL(40, 20),
		STANDARD(52, 24),
		LARGE(64, 28);

		final int diameterDp;
		final int iconDp;

		Size(int diameterDp, int iconDp) {
			this.diameterDp = diameterDp;
			this.iconDp = iconDp;
		}
	}

	private static final int DEFAULT_FILL = Ui.ACCENT;
	private static final int DEFAULT_ICON = 0xFF00201D;
	private static final float DISABLED_ALPHA = 0.38f;

	private final GradientDrawable mFill = new GradientDrawable();
	private final ImageView mIcon;
	private Size mSize = Size.STANDARD;

	public WearCircularButton(Context context) {
		this(context, null);
	}

	public WearCircularButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WearCircularButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mFill.setShape(GradientDrawable.OVAL);
		mFill.setColor(DEFAULT_FILL);
		GradientDrawable mask = new GradientDrawable();
		mask.setShape(GradientDrawable.OVAL);
		mask.setColor(Color.BLACK);
		setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), mFill, mask));

		mIcon = new ImageView(context);
		mIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mIcon.setImageTintList(ColorStateList.valueOf(DEFAULT_ICON));
		mIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
		addView(mIcon, new LayoutParams(0, 0, Gravity.CENTER));
		applyIconSize();

		setClickable(true);
		setFocusable(true);
	}

	public WearCircularButton(Context context, Size size, Drawable icon, Runnable onTap) {
		this(context);
		setSize(size);
		setIcon(icon);
		setOnClickListener(v -> onTap.run());
	}

	public void setSize(Size size) {
		if (size == mSize)
			return;
		mSize = size;
		applyIconSize();
		requestLayout();
	}

	public Size getSize() {
		return mSize;
	}

	public void setIcon(Drawable icon) {
		mIcon.setImageDrawable(icon);
	}

	public void setIconResource(int resId) {
		mIcon.setImageResource(resId);
	}

	public Drawable getIcon() {
		return mIcon.getDrawable();
	}

	public void setButtonColor(int color) {
		mFill.setColor(color);
	}

	public void setIconColor(int color) {
		mIcon.setImageTintList(ColorStateList.valueOf(color));
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setAlpha(enabled ? 1f : DISABLED_ALPHA);
	}

	private void applyIconSize() {
		int px = Ui.dp(getContext(), mSize.iconDp);
		LayoutParams lp = (LayoutParams) mIcon.getLayoutParams();
		lp.width = px;
		lp.height = px;
		mIcon.setLayoutParams(lp);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		int diameter = Ui.dp(getContext(), mSize.diameterDp);
		int side = Math.min(fit(diameter, widthSpec), fit(diameter, heightSpec));
		int exact = MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY);
		super.onMeasure(exact, exact);
	}

	private static int fit(int wanted, int spec) {
		int size = View.MeasureSpec.getSize(spec);
		switch (View.MeasureSpec.getMode(spec)) {
			case View.MeasureSpec.AT_MOST:
				return Math.min(size, wanted);
			case View.MeasureSpec.EXACTLY:
				return size;
			default:
				return wanted;
		}
	}
}
