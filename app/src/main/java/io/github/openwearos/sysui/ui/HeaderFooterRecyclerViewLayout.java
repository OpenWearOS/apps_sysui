package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

public class HeaderFooterRecyclerViewLayout extends FrameLayout {
    private static final float ROUND_EDGE_FRACTION = 0.15f;
    private static final int NO_INSET = Integer.MAX_VALUE;

    private final boolean mRound;

    private View mHeader;
    private View mFooter;
    private RecyclerView mList;

    private int mListIndex;
    private int mFooterDrawIndex;

    private int mListBasePaddingTop;
    private int mListBasePaddingBottom;

    private int mHeaderInset = NO_INSET;

    public HeaderFooterRecyclerViewLayout(Context context) {
        this(context, null);
    }

    public HeaderFooterRecyclerViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderFooterRecyclerViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HeaderFooterRecyclerViewLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setChildrenDrawingOrderEnabled(true);
        mRound = getResources().getConfiguration().isScreenRound();
    }

    public void setHeaderInset(int insetPx) {
        if (insetPx == mHeaderInset) return;
        mHeaderInset = insetPx;
        requestLayout();
    }

    private boolean hasHeaderInset() {
        return mHeaderInset != NO_INSET;
    }

    private static void failBadLayout() {
        throw new IllegalStateException(
                "Layout must be of format:\n(Optional) Header view\nRecyclerView\n(Optional) Footer view");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        switch (getChildCount()) {
            case 1:
                mListIndex = 0;
                break;
            case 2:
                if (getChildAt(0) instanceof RecyclerView) {
                    mListIndex = 0;
                    mFooterDrawIndex = 1;
                    mFooter = getChildAt(1);
                } else {
                    mListIndex = 1;
                    mHeader = getChildAt(0);
                }
                break;
            case 3:
                mListIndex = 1;
                mFooterDrawIndex = 2;
                mHeader = getChildAt(0);
                mFooter = getChildAt(2);
                break;
            default:
                failBadLayout();
        }
        if (!(getChildAt(mListIndex) instanceof RecyclerView)) failBadLayout();

        mList = (RecyclerView) getChildAt(mListIndex);
        mListBasePaddingTop = mList.getPaddingTop();
        mListBasePaddingBottom = mList.getPaddingBottom();
        mList.setClipToPadding(false);
        mList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateHeaderFooterPositions();
            }
        });
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        if (drawingPosition == childCount - 1) return mListIndex;
        if (drawingPosition == 0 && mHeader != null) return 0;
        return mFooterDrawIndex;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int top;
        if (mHeader != null && mHeader.getVisibility() == VISIBLE) {
            measureChild(mHeader, widthMeasureSpec, heightMeasureSpec);
            top = hasHeaderInset() ? mHeaderInset : mHeader.getMeasuredHeight();
        } else {
            top = mRound ? roundEdge(heightMeasureSpec) : 0;
        }

        int bottom;
        if (mFooter != null && mFooter.getVisibility() == VISIBLE) {
            measureChild(mFooter, widthMeasureSpec, heightMeasureSpec);
            bottom = mFooter.getMeasuredHeight();
        } else {
            bottom = mRound ? roundEdge(heightMeasureSpec) : 0;
        }

        mList.setPadding(mList.getPaddingLeft(), top + mListBasePaddingTop,
                mList.getPaddingRight(), bottom + mListBasePaddingBottom);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private static int roundEdge(int heightMeasureSpec) {
        return (int) (MeasureSpec.getSize(heightMeasureSpec) * ROUND_EDGE_FRACTION);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateHeaderFooterPositions();
    }

    /** Slides the header and footer to stay attached to the first and last list items. */
    private void updateHeaderFooterPositions() {
        RecyclerView.Adapter<?> adapter = mList.getAdapter();
        RecyclerView.LayoutManager lm = mList.getLayoutManager();
        int count = adapter == null ? 0 : adapter.getItemCount();

        if (count <= 0 || lm == null) {
            if (mHeader != null) mHeader.setTranslationY(0f);
            if (mFooter != null) mFooter.setTranslationY(mHeader != null ? mHeader.getHeight() : 0f);
            return;
        }

        if (mFooter != null) {
            View last = lm.findViewByPosition(count - 1);
            mFooter.setTranslationY(last != null ? last.getBottom() : mList.getHeight());
        }
        if (mHeader != null) {
            View first = lm.findViewByPosition(0);
            int firstTop = first != null ? first.getTop() : 0;
            mHeader.setTranslationY(firstTop - (hasHeaderInset() ? mHeaderInset : mHeader.getBottom()));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mHeader != null && event.getY() < mHeader.getBottom() + mHeader.getTranslationY()) {
            forwardTouch(mHeader, event);
        }
        if (mFooter != null && event.getY() >= mFooter.getTop() + mFooter.getTranslationY()) {
            forwardTouch(mFooter, event);
        }
        return super.onInterceptTouchEvent(event);
    }

    private static void forwardTouch(View target, MotionEvent event) {
        MotionEvent copy = MotionEvent.obtain(event);
        copy.offsetLocation(-target.getX(), -target.getY());
        target.dispatchTouchEvent(copy);
        copy.recycle();
    }
}