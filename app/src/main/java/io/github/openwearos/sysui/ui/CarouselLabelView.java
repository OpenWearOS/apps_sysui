package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.ui.OutlineProviders;

public class CarouselLabelView extends FrameLayout {
    public static final Interpolator INTERPOLATOR = new PathInterpolator(0.33f, 0.0f, 1.0f, 1.0f);

    private final RelativeLayout mIconContainer;
    private final FrameLayout mNextFrame;
    private final FrameLayout mCurrentFrame;
    private final ImageView mCurrentIcon;
    private final ImageView mNextIcon;
    private final float mPaddingBetween;
    private final float mMinIconScale;

    private int mSettledPage;
    private int mCurrentPage;
    private int mScrollState;

    public CarouselLabelView(Context context) {
        this(context, null);
    }

    public CarouselLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarouselLabelView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CarouselLabelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(getContext()).inflate(R.layout.carousel_label, this);
        mIconContainer = findViewById(R.id.carousel_label_icon_container);
        mNextFrame = findViewById(R.id.carousel_label_next_frame);
        mCurrentFrame = findViewById(R.id.carousel_label_current_frame);
        for (FrameLayout frame : new FrameLayout[]{mNextFrame, mCurrentFrame}) {
            frame.setOutlineProvider(OutlineProviders.CIRCLE);
            frame.setClipToOutline(true);
        }
        mCurrentIcon = findViewById(R.id.carousel_label_current_icon);
        mNextIcon = findViewById(R.id.carousel_label_next_icon);
        mCurrentIcon.setLayerType(LAYER_TYPE_HARDWARE, null);
        mNextIcon.setLayerType(LAYER_TYPE_HARDWARE, null);

        mPaddingBetween = getResources().getDimension(R.dimen.carousel_padding_between);
        TypedValue scale = new TypedValue();
        getResources().getValue(R.dimen.minimum_icon_scale, scale, true);
        mMinIconScale = scale.getFloat();
    }

    public void setIcons(Drawable first, Drawable second) {
        if (mSettledPage == mCurrentPage) {
            applyIcons(first, second);
        } else {
            applyIcons(second, first);
        }
    }

    private void applyIcons(Drawable current, Drawable next) {
        mCurrentIcon.setImageDrawable(current);
        mNextIcon.setImageDrawable(next);
    }

    public void applyFrameTransform(View frame, float progress, float translationX) {
        float scale = mMinIconScale + (1.0f - mMinIconScale) * progress;
        frame.setScaleX(scale);
        frame.setScaleY(scale);
        frame.setAlpha(progress);
        frame.setTranslationX(translationX);
    }

    public RelativeLayout getIconContainer() {
        return mIconContainer;
    }

    public FrameLayout getNextFrame() {
        return mNextFrame;
    }

    public FrameLayout getCurrentFrame() {
        return mCurrentFrame;
    }

    public float getPaddingBetween() {
        return mPaddingBetween;
    }


    public int getSettledPage() {
        return mSettledPage;
    }

    public void setSettledPage(int page) {
        mSettledPage = page;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int page) {
        mCurrentPage = page;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public void setScrollState(int state) {
        mScrollState = state;
    }
}