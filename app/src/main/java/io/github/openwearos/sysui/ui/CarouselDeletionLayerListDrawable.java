package io.github.openwearos.sysui.ui;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public final class CarouselDeletionLayerListDrawable extends LayerDrawable {
    private enum State {IDLE, DRAGGING, FADING_OUT}

    private final Drawable mIcon;
    private final Drawable mBackdrop;
    private final int mScreenWidth;
    private final int mScreenHeight;
    private State mState = State.IDLE;
    private float mDragProgress;
    private float mFadeProgress;

    public CarouselDeletionLayerListDrawable(Drawable icon, Drawable backdrop,
                                             int screenWidth, int screenHeight) {
        super(new Drawable[]{icon, backdrop});
        mIcon = icon;
        mBackdrop = backdrop;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
    }

    public void reset() {
        if (mState == State.IDLE) return;
        mState = State.IDLE;
        mDragProgress = 0f;
        mFadeProgress = 0f;
        invalidateSelf();
    }

    public void setDragProgress(float progress) {
        if (mState == State.FADING_OUT) return;
        if (mState == State.DRAGGING && mDragProgress == progress) return;
        mState = State.DRAGGING;
        mDragProgress = progress;
        layoutForDrag(getBounds());
    }

    public void setFadeOutProgress(float progress) {
        if (mState == State.IDLE) return;
        if (mState == State.FADING_OUT && mFadeProgress == progress) return;
        mState = State.FADING_OUT;
        mFadeProgress = progress;
        layoutForFadeOut(getBounds());
    }

    public float getFadeOutProgress() {
        return mFadeProgress;
    }

    @Override
    public void draw(Canvas canvas) {
        if (getBounds().isEmpty() || mState == State.IDLE) return;
        mBackdrop.draw(canvas);
        mIcon.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (mState == State.DRAGGING) layoutForDrag(bounds);
        else if (mState == State.FADING_OUT) layoutForFadeOut(bounds);
    }

    private int iconSize() {
        return Math.max(mIcon.getIntrinsicWidth(), mIcon.getIntrinsicHeight());
    }

    private void layoutForDrag(Rect bounds) {
        float p = mDragProgress;
        int iconMax = iconSize();
        int backdropMax = Math.min(mScreenWidth, mScreenHeight) / 2;
        float halfHeight = mScreenHeight / 2f;
        float restOffset = ((int) ((bounds.height() - iconMax / 2f) - halfHeight)) * 0.75f;
        int centerY = (int) (halfHeight + restOffset * (1f - p));
        place(bounds, centerY, (int) ((p * 0.5f + 0.5f) * iconMax),
                (int) ((p * 0.75f + 0.25f) * backdropMax));
        mIcon.setAlpha(toAlpha(Math.min(1f, p * 1.5f)));
        mBackdrop.setAlpha(toAlpha(p));
        invalidateSelf();
    }

    private void layoutForFadeOut(Rect bounds) {
        float p = mFadeProgress;
        int backdropMax = Math.min(mScreenWidth, mScreenHeight) / 2;
        place(bounds, mScreenHeight / 2, iconSize(), (int) ((p + 1f) * backdropMax));
        float iconFade = Math.max(0f, Math.min(1f, (p - 0.25f) * 2f));
        mIcon.setAlpha(toAlpha(1f - iconFade));
        mBackdrop.setAlpha(toAlpha(1f - p));
        invalidateSelf();
    }

    private void place(Rect bounds, int centerY, int iconSize, int backdropSize) {
        int iconLeft = (bounds.width() - iconSize) / 2;
        int iconTop = (int) ((centerY - iconSize / 2) * 0.975f);
        mIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        int backdropLeft = (bounds.width() - backdropSize) / 2;
        int backdropTop = (int) ((centerY - backdropSize / 2) * 0.975f);
        mBackdrop.setBounds(backdropLeft, backdropTop,
                backdropLeft + backdropSize, backdropTop + backdropSize);
    }

    private static int toAlpha(float fraction) {
        return (int) (fraction * 255f);
    }
}
