package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.ui.WearTimeText;
import io.github.openwearos.sysui.ui.ManagementView;

public class WatchFacePickerView extends ManagementView {
    private static final String TAG = "WFP2View";

    private View mPreviewOverlay;
    private View mLoadingView;
    private View mErrorView;
    private boolean mLoading;

    public WatchFacePickerView(Context context) {
        super(context);
    }

    public WatchFacePickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WatchFacePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPreviewOverlay(View overlay) {
        mPreviewOverlay = overlay;
    }

    public void removePreviewOverlay() {
        if (mPreviewOverlay == null) return;
        removeView(mPreviewOverlay);
        mPreviewOverlay = null;
    }

    @Override
    public void showLoading(boolean showText) {
        if (mLoadingView != null) {
            removeView(mLoadingView);
            Log.w(TAG, "Found dangling latency loading view, cleaning up...");
        }
        mLoadingView = LayoutInflater.from(getContext())
                .inflate(R.layout.wfp2_loading_watchface_screen, this, false);
        addView(mLoadingView);

        WearTimeText clock = mLoadingView.findViewById(R.id.watchface_picker_clock_text);
        TextView caption = mLoadingView.findViewById(R.id.watchface_picker_loading_text);
        clock.setClock(getClock());
        caption.setVisibility(showText ? VISIBLE : GONE);
    }

    @Override
    public void clearLoading() {
        if (mLoadingView != null) {
            removeView(mLoadingView);
            mLoadingView = null;
        }
    }

    @Override
    public void onHideRequested() {
        if (mPreviewOverlay != null) {
            mPreviewOverlay.setVisibility(INVISIBLE);
            mPreviewOverlay.setAlpha(0f);
        }
        super.onHideRequested();
    }

    @Override
    public void clearError() {
        if (mErrorView != null) {
            removeView(mErrorView);
            mErrorView = null;
        }
    }

    @Override
    public void showError(OnTouchListener onTouch) {
        if (mErrorView != null) {
            removeView(mErrorView);
            Log.w(TAG, "Found dangling error screen, cleaning up...");
        }
        mErrorView = LayoutInflater.from(getContext())
                .inflate(R.layout.wfp2_error_chip_view, this, false);
        addView(mErrorView);
        mErrorView.setOnTouchListener(onTouch);
    }

    @Override
    public void setSettingsIcon(Drawable icon) {
        super.setSettingsIcon(mLoading ? null : icon);
    }

    @Override
    public void enterEnabledState() {
        mLoading = false;
        super.enterEnabledState();
        getSettingsChip().setEnabled(true);
        setSettingsChipIcon(getContext().getDrawable(R.drawable.pencil_icon));
        getSettingsChip().setBackgroundTintList(ColorStateList.valueOf(themeColor(R.attr.colorPrimary)));
    }

    @Override
    public void enterLoadingState() {
        mLoading = true;
        super.enterLoadingState();
        setSettingsChipIcon(null);
    }
}