package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;

import io.github.openwearos.sysui.R;

public abstract class WearButton extends ConstraintLayout implements Checkable {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(WearButton button, boolean checked);
    }

    static final int[] DISABLED = {-android.R.attr.state_enabled};
    static final int[] ENABLED_UNCHECKED = {android.R.attr.state_enabled, -android.R.attr.state_checked};
    static final int[] ENABLED_CHECKED = {android.R.attr.state_enabled, android.R.attr.state_checked};
    private static final int[] CHECKED = {android.R.attr.state_checked};

    protected ColorStateList mDefaultIconTint;
    protected ColorStateList mDefaultBackgroundTint;
    protected ImageView mIcon;

    private ColorStateList mIconTint;
    private ColorStateList mBackgroundTint;
    private boolean mChecked;
    private boolean mCheckable;
    private boolean mToggleOnClick;
    private OnCheckedChangeListener mListener;

    public WearButton(Context context) {
        this(context, null);
    }

    public WearButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.wearButtonStyle);
    }

    public WearButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int onPrimary = themeColor(context, R.attr.colorOnPrimary, 0xFF000000);
        mDefaultBackgroundTint = stateList(onPrimary, onPrimary);
    }

    static ColorStateList stateList(int checked, int unchecked) {
        return new ColorStateList(
                new int[][] {ENABLED_UNCHECKED, ENABLED_CHECKED, DISABLED},
                new int[] {unchecked, checked, unchecked});
    }

    static int themeColor(Context context, int attr, int fallback) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true)) return fallback;
        return value.resourceId != 0 ? context.getColor(value.resourceId) : value.data;
    }

    private static int colorFor(ColorStateList list, int[] state) {
        return list == null ? 0 : list.getColorForState(state, 0);
    }

    protected final void initFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.WearButton, defStyleAttr, R.style.WearButtonDefault);
        try {
            boolean stateColors = a.hasValue(R.styleable.WearButton_buttonActiveBackgroundColor)
                    || a.hasValue(R.styleable.WearButton_buttonInactiveBackgroundColor)
                    || a.hasValue(R.styleable.WearButton_buttonActiveIconTint)
                    || a.hasValue(R.styleable.WearButton_buttonInactiveIconTint);

            int icon = a.getResourceId(R.styleable.WearButton_icon, 0);
            if (icon != 0) onIconResource(icon);

            mIconTint = a.hasValueOrEmpty(R.styleable.WearButton_buttonIconTint)
                    ? a.getColorStateList(R.styleable.WearButton_buttonIconTint) : mDefaultIconTint;
            mBackgroundTint = a.hasValueOrEmpty(R.styleable.WearButton_buttonBackgroundColor)
                    ? a.getColorStateList(R.styleable.WearButton_buttonBackgroundColor) : mDefaultBackgroundTint;

            if (stateColors) {
                int activeIcon = a.getColor(R.styleable.WearButton_buttonActiveIconTint,
                        colorFor(mIconTint, ENABLED_CHECKED));
                int inactiveIcon = a.getColor(R.styleable.WearButton_buttonInactiveIconTint,
                        colorFor(mIconTint, ENABLED_UNCHECKED));
                if (activeIcon == 0 && inactiveIcon == 0) mIconTint = null;
                else mIconTint = stateList(activeIcon, inactiveIcon);

                mBackgroundTint = stateList(
                        a.getColor(R.styleable.WearButton_buttonActiveBackgroundColor,
                                colorFor(mBackgroundTint, ENABLED_CHECKED)),
                        a.getColor(R.styleable.WearButton_buttonInactiveBackgroundColor,
                                colorFor(mBackgroundTint, ENABLED_UNCHECKED)));
            }
            mIcon.setImageTintList(mIconTint);
            applyBackgroundTint(mBackgroundTint);

            int radius = (int) a.getDimension(R.styleable.WearButton_cornerRadius, 0f);
            Drawable bg = getBackground();
            if (bg instanceof LayerDrawable) {
                bg = ((LayerDrawable) bg).findDrawableByLayerId(android.R.id.background);
            }
            if (bg instanceof GradientDrawable) ((GradientDrawable) bg).setCornerRadius(radius);

            setEnabled(a.getBoolean(R.styleable.WearButton_android_enabled, true));
            setCheckable(a.getBoolean(R.styleable.WearButton_android_checkable, false));
            setChecked(a.getBoolean(R.styleable.WearButton_android_checked, false));
            mToggleOnClick = a.getBoolean(R.styleable.WearButton_toggleOnClick, false);
            setClipToOutline(true);
        } finally {
            a.recycle();
        }
    }

    protected abstract void onIconResource(int resId);

    public final void setButtonBackgroundTint(ColorStateList tint) {
        mBackgroundTint = tint;
        applyBackgroundTint(tint);
    }

    public final void setIconTint(ColorStateList tint) {
        mIconTint = tint;
        mIcon.setImageTintList(tint);
    }

    protected void applyBackgroundTint(ColorStateList tint) {
        Drawable bg = getBackground();
        if (bg != null) bg.mutate().setTintList(tint);
        refreshDrawableState();
    }

    @Override
    public void setBackground(Drawable drawable) {
        if (drawable != null) drawable.mutate().setTintMode(PorterDuff.Mode.SRC_ATOP);
        super.setBackground(drawable);
        applyBackgroundTint(mBackgroundTint);
    }

    @Override
    public final void setBackgroundColor(int color) {
        setButtonBackgroundTint(ColorStateList.valueOf(color));
    }

    public final void setCheckable(boolean checkable) {
        mCheckable = checkable;
        refreshDrawableState();
    }

    public final boolean isCheckable() {
        return mCheckable;
    }

    public final void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (!mCheckable || checked == mChecked) return;
        mChecked = checked;
        refreshDrawableState();
        if (mListener != null) mListener.onCheckedChanged(this, checked);
    }

    @Override
    public final void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        if (mCheckable && isEnabled() && mToggleOnClick) toggle();
        return super.performClick();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 2);
        if (mChecked) mergeDrawableStates(state, CHECKED);
        int background = mBackgroundTint == null ? 0 : mBackgroundTint.getColorForState(state, 0);
        boolean light = ColorUtils.calculateLuminance(background) > 0.5;
        mergeDrawableStates(state, new int[] {light ? R.attr.state_light : -R.attr.state_light});
        return state;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return (mCheckable ? CompoundButton.class : Button.class).getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setCheckable(mCheckable);
        info.setChecked(mChecked);
    }

    public final void getIconGlobalVisibleRect(android.graphics.Rect out) {
        mIcon.getGlobalVisibleRect(out);
    }

    protected final int pixels(int dimenRes) {
        return getResources().getDimensionPixelSize(dimenRes);
    }

    protected static void setVisible(View v, boolean visible) {
        v.setVisibility(visible ? VISIBLE : GONE);
    }
}