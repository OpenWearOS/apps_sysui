package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.github.openwearos.sysui.R;

public class WearCircularButton extends WearButton {
	public enum Size {
		XSMALL(R.dimen.wear_circular_button_diameter_xsmall, R.dimen.wear_circular_button_icon_size_xsmall),
		SMALL(R.dimen.wear_circular_button_diameter_small, R.dimen.wear_circular_button_icon_size_small),
		STANDARD(R.dimen.wear_circular_button_diameter_standard, R.dimen.wear_circular_button_icon_size_standard),
		LARGE(R.dimen.wear_circular_button_diameter_large, R.dimen.wear_circular_button_icon_size_large);

		final int diameterRes;
		final int iconRes;

		Size(int diameterRes, int iconRes) {
			this.diameterRes = diameterRes;
			this.iconRes = iconRes;
		}
	}

	private int mDiameterRes = Size.STANDARD.diameterRes;
	private int mIconSizeRes = Size.STANDARD.iconRes;

	public WearCircularButton(Context context) {
		this(context, null);
	}

	public WearCircularButton(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.wearCircularButtonStyle);
	}

	public WearCircularButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mDefaultIconTint = stateList(
				themeColor(context, R.attr.colorPrimary, 0xFFFFFFFF),
				themeColor(context, R.attr.colorOnPrimary, 0xFF000000));
		inflateContent();
		initFromAttributes(attrs, defStyleAttr);

		TypedArray a = context.obtainStyledAttributes(
				attrs, R.styleable.WearCircularButton, defStyleAttr, R.style.WearCircularButtonDefault);
		try {
			int ordinal = a.getInt(R.styleable.WearCircularButton_size, Size.STANDARD.ordinal());
			Size[] sizes = Size.values();
			setSize(sizes[Math.max(0, Math.min(sizes.length - 1, ordinal))]);
		} finally {
			a.recycle();
		}
	}

	public WearCircularButton(Context context, Size size, Drawable icon, Runnable onTap) {
		this(context);
		setSize(size);
		setIcon(icon);
		setOnClickListener(v -> onTap.run());
	}

	protected void inflateContent() {
		LayoutInflater.from(getContext()).inflate(R.layout.wear_circular_button_layout, this, true);
		mIcon = findViewById(R.id.icon);
	}

	public final void setSize(Size size) {
		setSizeResources(size.diameterRes, size.iconRes);
	}

	public final void setSizeResources(int diameterRes, int iconSizeRes) {
		mDiameterRes = diameterRes;
		mIconSizeRes = iconSizeRes;
		onIconSizeChanged();
	}

	protected void onIconSizeChanged() {
		int px = pixels(mIconSizeRes);
		ViewGroup.LayoutParams lp = mIcon.getLayoutParams();
		lp.width = px;
		lp.height = px;
		mIcon.setLayoutParams(lp);
		requestLayout();
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		int diameter = pixels(mDiameterRes);
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

	@Override
	protected void onIconResource(int resId) {
		if (resId != 0)
			mIcon.setImageResource(resId);
	}

	public void setIcon(Drawable icon) {
		mIcon.setImageDrawable(icon);
	}

	public void setIconResource(int resId) {
		mIcon.setImageResource(resId);
	}

	public final Drawable getIcon() {
		return mIcon.getDrawable();
	}

	public final ImageView getIconView() {
		return mIcon;
	}

	public final void setButtonColor(int color) {
		setButtonBackgroundTint(ColorStateList.valueOf(color));
	}

	public final void setIconColor(int color) {
		setIconTint(ColorStateList.valueOf(color));
	}

	public final Size getSize() {
		for (Size s : Size.values()) {
			if (s.diameterRes == mDiameterRes)
				return s;
		}
		return null;
	}
}
