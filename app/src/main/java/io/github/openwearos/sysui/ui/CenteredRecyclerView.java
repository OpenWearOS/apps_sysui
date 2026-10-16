package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class CenteredRecyclerView extends RecyclerView {
    public CenteredRecyclerView(Context context) {
        this(context, null);
    }

    public CenteredRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenteredRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipToPadding(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        if (getLayoutManager() == null) {
            setLayoutManager(new LinearLayoutManager(context));
        }
        new LinearSnapHelper().attachToRecyclerView(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        centerEdgeItems();
    }

    private void centerEdgeItems() {
        if (getChildCount() == 0 || getHeight() <= 0) {
            return;
        }
        View child = getChildAt(getChildCount() / 2);
        int pad = Math.max(0, (getHeight() - child.getHeight()) / 2);
        if (pad != getPaddingTop() || pad != getPaddingBottom()) {
            setPadding(getPaddingLeft(), pad, getPaddingRight(), pad);
        }
    }
}