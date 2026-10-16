package io.github.openwearos.sysui.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.CurvedTextView;

import java.util.Objects;

import io.github.openwearos.sysui.R;

public final class AllItemsView extends FrameLayout {

    public interface Listener {
        void onFooterClicked();

        void onItemSelected(int position);

        void onUserInteractionEnd();

        void onUserInteractionStart();

        void onCloseAnimationEnd();
    }

    public interface Resettable {
        void reset();
    }

    public static final class AdapterInfo {
        public final Context context;
        public final int barePreviewWidth;
        public final int barePreviewHeight;
        public final int borderedPreviewWidth;
        public final int borderedPreviewHeight;
        public final Drawable screenShapeDrawable;
        public final Drawable screenShapePlaceholder;
        public final AllItemsView owner;

        AdapterInfo(Context context, int bareW, int bareH, int borderedW, int borderedH,
                    Drawable shape, Drawable placeholder, AllItemsView owner) {
            this.context = Objects.requireNonNull(context);
            this.barePreviewWidth = bareW;
            this.barePreviewHeight = bareH;
            this.borderedPreviewWidth = borderedW;
            this.borderedPreviewHeight = borderedH;
            this.screenShapeDrawable = shape;
            this.screenShapePlaceholder = placeholder;
            this.owner = owner;
        }
    }

    public static class AllItemsRecyclerView extends FadingWearableRecyclerView {
        Listener mListener;

        public AllItemsRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            Listener l = mListener;
            if (l == null) return super.onGenericMotionEvent(event);
            l.onUserInteractionStart();
            boolean handled = super.onGenericMotionEvent(event);
            l.onUserInteractionEnd();
            return handled;
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
            Listener l = mListener;
            if (l == null || event.getEventType() != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                return super.onRequestSendAccessibilityEvent(child, event);
            }
            l.onUserInteractionStart();
            boolean result = super.onRequestSendAccessibilityEvent(child, event);
            l.onUserInteractionEnd();
            return result;
        }
    }

    private static final class LockableGridLayoutManager extends GridLayoutManager {
        boolean scrollingEnabled = true;

        LockableGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public boolean canScrollVertically() {
            return scrollingEnabled && super.canScrollVertically();
        }
    }

    private static final class SideGapDecoration extends RecyclerView.ItemDecoration {
        private final int mItemWidth;
        private final int mColumns;

        SideGapDecoration(int itemWidth, int columns) {
            mItemWidth = itemWidth;
            mColumns = columns;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int column = parent.getChildAdapterPosition(view) % mColumns;
            int free = parent.getWidth() - mColumns * mItemWidth;
            int gap = Math.max(0, free) / (mColumns + 1);
            int half = gap / 2;
            outRect.left = column == 0 ? gap : half;
            outRect.right = column == mColumns - 1 ? gap : half;
            outRect.top = 0;
            outRect.bottom = 0;
        }
    }

    private static final class ScreenShape {
        final int width;
        final int height;
        final boolean round;
        int cornerRadius;

        private Path mPath;
        private Rect mPathBounds;
        private int mPathRadius = -1;

        ScreenShape(int width, int height, boolean round, int cornerRadius) {
            this.width = width;
            this.height = height;
            this.round = round;
            this.cornerRadius = cornerRadius;
        }

        boolean isFlatTire() {
            return height < width;
        }

        Path pathFor(Rect rect) {
            if (mPath != null && rect.equals(mPathBounds) && mPathRadius == cornerRadius) return mPath;
            Path path = new Path();
            if (!round) {
                addRectangular(path, rect);
            } else if (isFlatTire()) {
                addFlatTire(path, rect);
            } else {
                path.addCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, Path.Direction.CW);
            }
            mPath = path;
            mPathBounds = new Rect(rect);
            mPathRadius = cornerRadius;
            return path;
        }

        private void addRectangular(Path path, Rect rect) {
            float screenAspect = (float) width / height;
            float rectAspect = rect.height() == 0 ? 0f : (float) rect.width() / rect.height();
            Rect fit = rect;
            if (screenAspect > rectAspect) {
                int inset = (int) (rect.height() - rect.width() / screenAspect) / 2;
                fit = new Rect(rect.left, rect.top + inset, rect.right, rect.bottom - inset);
            } else if (screenAspect < rectAspect) {
                int inset = (int) (rect.width() - rect.height() * screenAspect) / 2;
                fit = new Rect(rect.left + inset, rect.top, rect.right - inset, rect.bottom);
            }
            if (cornerRadius == 0) {
                path.addRect(fit.left, fit.top, fit.right, fit.bottom, Path.Direction.CW);
            } else {
                float r = cornerRadius * fit.width() / (float) width;
                path.addRoundRect(fit.left, fit.top, fit.right, fit.bottom, r, r, Path.Direction.CW);
            }
        }

        private void addFlatTire(Path path, Rect rect) {
            float cut = (float) height / width;
            float offset = cut - 0.5f;
            float halfChord = (float) Math.sqrt(0.25f - offset * offset);
            float startAngle = (float) Math.toDegrees(Math.acos(offset / 0.5f));
            float y = rect.top + rect.height() * cut;
            path.moveTo(rect.left + rect.width() * (halfChord + 0.5f), y);
            path.lineTo(rect.left + rect.width() * (0.5f - halfChord), y);
            path.arcTo(rect.left, rect.top, rect.right, rect.bottom,
                    startAngle + 90f, 360f - 2f * startAngle, false);
            path.close();
        }
    }

    private static final class ScreenShapeDrawable extends Drawable {
        private final ScreenShape mShape;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScreenShapeDrawable(ScreenShape shape, int color) {
            mShape = shape;
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(color);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawPath(mShape.pathFor(getBounds()), mPaint);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }
    }

    private static final Interpolator CLOSE_INTERPOLATOR = new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    private final ScreenShape mScreenShape;
    private final int mBackgroundColor;

    private AdapterInfo mAdapterInfo;
    private Interpolator mEnterInterpolator;
    private Listener mListener;
    private RecyclerView.Adapter<?> mAdapter;
    private View mCloseCompanion;
    private View mContent;
    private AllItemsRecyclerView mList;
    private ButtonWithPercentPadding mFooterButton;
    private LockableGridLayoutManager mLayoutManager;
    private int mColumns;
    private boolean mClosing;

    public AllItemsView(Context context) {
        this(context, null);
    }

    public AllItemsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllItemsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AllItemsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mBackgroundColor = context.getColor(R.color.new_default_customizable_background_dark);
        mScreenShape = new ScreenShape(
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                getResources().getConfiguration().isScreenRound(),
                0);
    }

    public void setScreenCornerRadius(int radiusPx) {
        mScreenShape.cornerRadius = Math.max(0, radiusPx);
        invalidate();
    }

    public AdapterInfo getAdapterInfo() {
        if (mAdapterInfo != null) return mAdapterInfo;

        mEnterInterpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.linear_out_slow_in);
        int w = getResources().getDimensionPixelSize(R.dimen.managementui_all_preview_width);
        int h = getResources().getDimensionPixelSize(R.dimen.managementui_all_preview_height);
        int border = getResources().getDimensionPixelSize(R.dimen.managementui_all_preview_border_width);
        int borderColor = getResources().getColor(R.color.managementui_all_border_color, null);

        mAdapterInfo = new AdapterInfo(getContext(), w, h, w + 2 * border, h + 2 * border,
                new ScreenShapeDrawable(mScreenShape, borderColor),
                new ScreenShapeDrawable(mScreenShape, borderColor),
                this);
        return mAdapterInfo;
    }

    public Interpolator getEnterInterpolator() {
        getAdapterInfo();
        return mEnterInterpolator;
    }

    public Listener getListener() {
        return mListener;
    }

    public AllItemsRecyclerView getList() {
        return mList;
    }

    public View getContent() {
        return mContent;
    }

    public ButtonWithPercentPadding getFooterButton() {
        return mFooterButton;
    }

    public void setCloseCompanionView(View view) {
        mCloseCompanion = view;
    }

    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    public void setListener(Listener listener) {
        mListener = listener;
        mFooterButton.setOnClickListener(v -> {
            Listener l = mListener;
            if (mFooterButton.getVisibility() == VISIBLE && l != null) l.onFooterClicked();
        });
        mList.mListener = listener;
    }

    public void setListScrollingEnabled(boolean enabled) {
        mLayoutManager.scrollingEnabled = enabled;
    }

    public void setFooter(String text, Drawable icon) {
        mFooterButton.setText(text);
        if (icon != null && icon.getBounds().isEmpty()) {
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        }
        mFooterButton.setCompoundDrawables(null, null, null, icon);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        AdapterInfo info = getAdapterInfo();
        setBackgroundColor(mBackgroundColor);

        mColumns = getResources().getInteger(R.integer.managementui_all_faces_column_count);
        mContent = findViewById(R.id.managementui_all_items_content);
        mList = findViewById(R.id.managementui_all_list);
        mFooterButton = findViewById(R.id.managementui_all_footer_button);

        mLayoutManager = new LockableGridLayoutManager(getContext(), mColumns);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        mList.setLayoutManager(mLayoutManager);
        mList.addItemDecoration(new SideGapDecoration(info.borderedPreviewWidth, mColumns));

        if (mScreenShape.round) {
            // Reserve room above the list for the arc the title follows.
            CurvedTextView title = findViewById(R.id.managementui_all_title);
            float sweep = getResources().getFloat(R.dimen.managementui_all_list_title_max_sweep_degree);
            double radius = mScreenShape.height / 2.0;
            int sagitta = (int) (radius - Math.cos(Math.toRadians(sweep / 2f)) * radius);
            int inset = sagitta + title.getPaddingBottom() + title.getPaddingTop();
            ((HeaderFooterRecyclerViewLayout) mContent).setHeaderInset(inset);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != VISIBLE) {
            if (mAdapter instanceof Resettable) ((Resettable) mAdapter).reset();
            return;
        }
        if (mList != null) mList.requestFocus();
    }

    public void close() {
        if (mClosing) return;
        mClosing = true;

        final View content = mContent;
        final float w = getWidth();
        final float h = getHeight();

        double startRadius = w / 2f;
        if (!mScreenShape.round) startRadius = Math.hypot(startRadius, startRadius);

        Animator reveal = ViewAnimationUtils.createCircularReveal(
                this, getWidth() / 2, getHeight() / 2, (float) startRadius, 0f);
        reveal.setInterpolator(CLOSE_INTERPOLATOR);
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(reveal,
                ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f),
                ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f),
                ObjectAnimator.ofFloat(content, View.TRANSLATION_X, 0f),
                ObjectAnimator.ofFloat(content, View.TRANSLATION_Y, 0f));
        set.setInterpolator(CLOSE_INTERPOLATOR);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setTranslationX(0f);
                setTranslationY(0f);
                content.setVisibility(GONE);
                content.setTranslationX(0f);
                content.setTranslationY(0f);
            }
        });
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mClosing = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mClosing = false;
                if (mCloseCompanion != null) {
                    mCloseCompanion.setVisibility(GONE);
                    if (mListener != null) mListener.onCloseAnimationEnd();
                }
            }
        });
        set.start();
    }
}