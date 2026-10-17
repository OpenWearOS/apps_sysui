package io.github.openwearos.sysui.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.SwipeDismissFrameLayout;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.ui.AllItemsView;
import io.github.openwearos.sysui.ui.WearCircularButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;

public class ManagementView extends FrameLayout {
    private static final String TAG = "ManagementView";
    private static final int NONE = -1;

    public interface Presenter {
        int NO_POSITION = RecyclerView.NO_POSITION;

        void onUserInteractionStart();

        void onUserInteractionEnd();

        void onCarouselDragStarted();

        void onFocusSettled();

        void onItemFocused(int position);

        void onDismissRequested();

        void onAllItemsFooterClicked();

        void onAllItemSelected(int allItemsPosition);

        void onPickerHidden();

        int getItemCount();

        boolean hasTrailingAddPlaceholder();

        boolean isItemPosition(int position);

        @Nullable
        View getAllItemsPreview(int position);
    }

    public static final class ItemMetrics {
        public final int previewWidth;
        public final int previewHeight;
        public final int borderWidth;
        public final boolean round;
        public final boolean chin;

        ItemMetrics(int previewWidth, int previewHeight, int borderWidth, boolean round, boolean chin) {
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
            this.borderWidth = borderWidth;
            this.round = round;
            this.chin = chin;
        }
    }

    private Presenter mPresenter;
    private LongSupplier mClock = System::currentTimeMillis;

    private View mMainContainer;
    private RecyclerView mList;
    private LinearLayoutManager mLayoutManager;
    private CarouselSnapHelper mSnapHelper;
    private AllItemsView mAllItems;
    private SwipeDismissFrameLayout mSwipeLayout;
    private ViewGroup mSettingsArea;
    private TextView mSettingsChip;
    private WearCircularButton mLeftButton;
    private WearCircularButton mRightButton;
    private CarouselDeletionLayerListDrawable mDeletionDrawable;

    private ItemMetrics mMetrics;
    private int mItemPitch;
    private Interpolator mFastOutSlowIn;
    private Interpolator mLinearOutSlowIn;

    private Transition mSettingsTransition;
    private Animator mHideAnimator;
    private ViewPropertyAnimator mFadeAnimator;
    private View mErrorView;

    private boolean mSettingsTransitionRunning;
    private boolean mEntering;
    private boolean mAddInProgress;
    private boolean mAtAddPlaceholder;
    private boolean mFadeInOnEntry;
    private boolean mAnimateEntry = true;
    private int mPendingAllItemsPosition = NONE;

    public ManagementView(Context context) {
        this(context, null);
    }

    public ManagementView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ManagementView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPresenter(Presenter presenter) {
        mPresenter = presenter;
    }

    public void setClock(LongSupplier clock) {
        mClock = clock != null ? clock : System::currentTimeMillis;
    }

    protected LongSupplier getClock() {
        return mClock;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        getItemMetrics();

        mSettingsArea = requireViewById(R.id.managementui_settings_area);
        ViewGroup.MarginLayoutParams areaParams =
                (ViewGroup.MarginLayoutParams) mSettingsArea.getLayoutParams();
        areaParams.topMargin = -res.getDimensionPixelSize(R.dimen.managementui_rearrange_circular_settings);
        mSettingsArea.setLayoutParams(areaParams);

        mSettingsChip = requireViewById(R.id.managementui_item_settings);
        mLeftButton = requireViewById(R.id.managementui_rearrange_left);
        mRightButton = requireViewById(R.id.managementui_rearrange_right);
        setSettingsChipVisible(true, false);

        int padding = res.getDimensionPixelSize(R.dimen.managementui_items_padding);
        mItemPitch = mMetrics.previewWidth + 2 * padding + 2 * mMetrics.borderWidth;

        mMainContainer = requireViewById(R.id.managementui_main_container);
        mList = requireViewById(R.id.managementui_list);
        mList.setHasFixedSize(true);
        mFastOutSlowIn = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowIn = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in);

        DisplayMetrics dm = res.getDisplayMetrics();
        mDeletionDrawable = new CarouselDeletionLayerListDrawable(
                res.getDrawable(R.drawable.ic_delete, null),
                res.getDrawable(R.drawable.ic_delete_background, null),
                dm.widthPixels, dm.heightPixels);
        requireViewById(R.id.managementui_delete_icon).setBackground(mDeletionDrawable);

        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mList.setLayoutManager(mLayoutManager);
        int sidePadding = (dm.widthPixels - mItemPitch) / 2;
        mList.setPadding(sidePadding, 0, sidePadding, 0);
        mList.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            onFocusSettled();
            fadeSideItems();
        });

        mSwipeLayout = requireViewById(R.id.managementui_swipe_layout);
        mSwipeLayout.setSwipeable(true);
        mSwipeLayout.setVisibility(GONE);
        mSwipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                mSwipeLayout.setVisibility(GONE);
                mAllItems.setVisibility(GONE);
                mMainContainer.setVisibility(VISIBLE);
                mPendingAllItemsPosition = NONE;
                mAddInProgress = false;
                if (mPresenter != null) mPresenter.onFocusSettled();
            }
//
//            @Override
//            public void onSwipeStarted() {
//                mMainContainer.setVisibility(GONE);
//            }
//
//            @Override
//            public void onSwipeCanceled() {
//                mMainContainer.setVisibility(VISIBLE);
//            }
        });

        mAllItems = requireViewById(R.id.managementui_all_items);
        mAllItems.setVisibility(INVISIBLE);
        mAllItems.setListener(new AllItemsView.Listener() {
            @Override
            public void onFooterClicked() {
                if (mPresenter == null) return;
                mPresenter.onCarouselDragStarted();
                mPresenter.onAllItemsFooterClicked();
            }

            @Override
            public void onItemSelected(int position) {
                if (mPendingAllItemsPosition == NONE && !mAddInProgress) {
                    mAddInProgress = true;
                    mPendingAllItemsPosition = position;
                    mMainContainer.setVisibility(VISIBLE);
                    mAllItems.setListScrollingEnabled(false);
                    if (mPresenter != null) {
                        mPresenter.onFocusSettled();
                        mPresenter.onAllItemSelected(position);
                    }
                    return;
                }
                Log.d(TAG, "Add pending, ignoring new selection, pendingIndex="
                        + mPendingAllItemsPosition + " inProgress=" + mAddInProgress);
            }

            @Override
            public void onUserInteractionEnd() {
                if (mPresenter != null) mPresenter.onUserInteractionEnd();
            }

            @Override
            public void onUserInteractionStart() {
                if (mPresenter != null) mPresenter.onUserInteractionStart();
            }

            @Override
            public void onCloseAnimationEnd() {
                mAddInProgress = false;
                mPendingAllItemsPosition = NONE;
            }
        });
        mAllItems.setCloseCompanionView(mSwipeLayout);

        mSettingsArea.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, or, ob) -> updateSettingsTouchTargets());
    }

    public ItemMetrics getItemMetrics() {
        if (mMetrics != null) return mMetrics;

        Configuration config = getResources().getConfiguration();
        boolean round = config.isScreenRound();
        boolean landscape = config.screenHeightDp < config.screenWidthDp;
        float percent = getResources().getFraction(R.fraction.managementui_items_size, 100, 1);
        if (landscape && config.screenWidthDp > 0 && config.screenHeightDp > 0) {
            percent = percent * config.screenHeightDp / config.screenWidthDp;
        }
        Rect frame = new Rect();
        getWindowVisibleDisplayFrame(frame);
        if (frame.isEmpty()) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            frame.set(0, 0, dm.widthPixels, dm.heightPixels);
        }
        // Width is forced even so the preview centers exactly.
        int halfWidth = Math.round(percent / 100f * frame.width()) / 2;
        int height = Math.round(percent / 100f * frame.height());
        int border = getResources().getDimensionPixelSize(R.dimen.managementui_items_border_width);
        mMetrics = new ItemMetrics(halfWidth * 2, height, border, round, landscape);
        return mMetrics;
    }

    public RecyclerView getList() {
        return mList;
    }

    public AllItemsView getAllItemsView() {
        return mAllItems;
    }

    public SwipeDismissFrameLayout getSwipeLayout() {
        return mSwipeLayout;
    }

    public TextView getSettingsChip() {
        return mSettingsChip;
    }

    public WearCircularButton getLeftButton() {
        return mLeftButton;
    }

    public WearCircularButton getRightButton() {
        return mRightButton;
    }

    public boolean isAllItemsShown() {
        return mSwipeLayout.getVisibility() == VISIBLE;
    }

    public boolean isRtl() {
        return mMainContainer.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    public boolean shouldAnimateEntry() {
        return mAnimateEntry;
    }

    public void setEntryOptions(boolean fadeInOnEntry, boolean animateEntry) {
        mFadeInOnEntry = fadeInOnEntry;
        mAnimateEntry = animateEntry;
    }

    public void setDeleteProgress(float progress) {
        mDeletionDrawable.setDragProgress(progress);
    }

    public void setDeleteFadeOutProgress(float progress) {
        mDeletionDrawable.setFadeOutProgress(progress);
    }

    public void resetDeleteProgress() {
        mDeletionDrawable.reset();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!mEntering && mPresenter != null) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) mPresenter.onUserInteractionStart();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mPresenter.onUserInteractionEnd();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEntering || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }
    private void updateSettingsTouchTargets() {
        Resources res = getResources();
        int slop = (int) (res.getDimension(R.dimen.managementui_rearrange_button_tap_target_size)
                - (int) res.getDimension(R.dimen.managementui_rearrange_button_size));
        int half = slop / 2;

        Rect chip = new Rect();
        mSettingsChip.getHitRect(chip);
        chip.top = 0;
        chip.bottom = mSettingsArea.getBottom();

        Rect left = new Rect();
        mLeftButton.getHitRect(left);
        left.top -= half;
        left.bottom += half;
        left.left -= slop;
        left.right = chip.left;

        Rect right = new Rect();
        mRightButton.getHitRect(right);
        right.top -= half;
        right.bottom += half;
        right.right += slop;
        right.left = chip.right;

        List<TouchDelegate> delegates = new ArrayList<>();
        delegates.add(new TouchDelegate(chip, mSettingsChip));
        delegates.add(new TouchDelegate(left, mLeftButton));
        delegates.add(new TouchDelegate(right, mRightButton));
        mSettingsArea.setTouchDelegate(new MultiTouchDelegate(mSettingsArea, delegates));
    }

    private static final class MultiTouchDelegate extends TouchDelegate {
        private final List<TouchDelegate> mDelegates;
        private TouchDelegate mActive;

        MultiTouchDelegate(View host, List<TouchDelegate> delegates) {
            super(new Rect(), host);
            mDelegates = delegates;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mActive = null;
                for (TouchDelegate delegate : mDelegates) {
                    if (delegate.onTouchEvent(event)) {
                        mActive = delegate;
                        return true;
                    }
                }
                return false;
            }
            return mActive != null && mActive.onTouchEvent(event);
        }
    }

    private void beginSettingsTransition() {
        if (mSettingsTransitionRunning) TransitionManager.endTransitions(mSettingsArea);
        if (mSettingsTransition == null) {
            ChangeBounds bounds = new ChangeBounds();
            bounds.setDuration(375L);
            Fade fade = new Fade(Fade.IN);
            fade.setDuration(450L);
            Slide slide = new Slide();
            slide.setDuration(375L);
            TransitionSet set = new TransitionSet();
            set.setOrdering(TransitionSet.ORDERING_TOGETHER);
            set.addTransition(bounds);
            set.addTransition(fade);
            set.addTransition(slide);
            set.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition t) {
                    mSettingsTransitionRunning = true;
                }

                @Override
                public void onTransitionEnd(Transition t) {
                    mSettingsTransitionRunning = false;
                }

                @Override
                public void onTransitionCancel(Transition t) {
                }

                @Override
                public void onTransitionPause(Transition t) {
                }

                @Override
                public void onTransitionResume(Transition t) {
                }
            });
            mSettingsTransition = set;
        }
        TransitionManager.beginDelayedTransition(mSettingsArea, mSettingsTransition);
    }

    private static boolean visibilityWouldChange(View view, boolean visible) {
        Boolean tagged = (Boolean) view.getTag(R.id.managementui_is_visible);
        boolean current = tagged != null ? tagged : view.getVisibility() == VISIBLE;
        return visible != current;
    }

    private static void applyVisibility(View view, boolean visible) {
        view.setVisibility(visible ? VISIBLE : INVISIBLE);
        view.setTag(R.id.managementui_is_visible, visible);
    }

    public void setSettingsChipVisible(boolean visible, boolean skipArrowLayout) {
        mSettingsChip.setEnabled(visible);
        Resources res = getResources();
        boolean toggle = visibilityWouldChange(mSettingsChip, visible);
        int bottomMargin = res.getDimensionPixelSize(visible
                ? R.dimen.managementui_rearrange_circular_settings
                : R.dimen.managementui_rearrange_circular);
        ViewGroup.MarginLayoutParams leftParams = (ViewGroup.MarginLayoutParams) mLeftButton.getLayoutParams();
        ViewGroup.MarginLayoutParams rightParams = (ViewGroup.MarginLayoutParams) mRightButton.getLayoutParams();

        if (!toggle) {
            if (skipArrowLayout || leftParams.bottomMargin == bottomMargin) return;
        }
        beginSettingsTransition();
        if (toggle) applyVisibility(mSettingsChip, visible);
        if (skipArrowLayout) return;

        int sideMargin = res.getDimensionPixelSize(visible
                ? R.dimen.managementui_rearrange_padding_settings
                : R.dimen.managementui_rearrange_padding);
        if (!visible) {
            sideMargin -= res.getDimensionPixelSize(R.dimen.managementui_rearrange_chip_width) / 2;
        }
        leftParams.bottomMargin = bottomMargin;
        leftParams.setMarginEnd(sideMargin);
        mLeftButton.setLayoutParams(leftParams);
        rightParams.bottomMargin = bottomMargin;
        rightParams.setMarginStart(sideMargin);
        mRightButton.setLayoutParams(rightParams);
    }

    public void setLeftButtonVisible(boolean visible) {
        mLeftButton.setEnabled(visible);
        if (visibilityWouldChange(mLeftButton, visible)) {
            beginSettingsTransition();
            applyVisibility(mLeftButton, visible);
        }
    }

    public void setRightButtonVisible(boolean visible) {
        mRightButton.setEnabled(visible);
        if (visibilityWouldChange(mRightButton, visible)) {
            beginSettingsTransition();
            applyVisibility(mRightButton, visible);
        }
    }

    public void setSettingsChipIcon(@Nullable Drawable icon) {
        mSettingsChip.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    public void setSettingsIcon(@Nullable Drawable icon) {
        setSettingsChipIcon(icon);
    }

    public void enterEnabledState() {
        int arrowColor = ContextCompat.getColor(getContext(), R.color.wear_circular_button_background);
        for (WearCircularButton arrow : new WearCircularButton[]{mLeftButton, mRightButton}) {
            arrow.setButtonBackgroundTint(ColorStateList.valueOf(arrowColor));
            arrow.setEnabled(true);
            setIconAlpha(arrow, 255);
        }
        mSettingsChip.setEnabled(true);
        mSettingsChip.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.attr.colorPrimary)));
    }

    public void enterLoadingState() {
        int loading = getResources().getColor(R.color.managementui_loading_icon_color, null);
        setLeftButtonVisible(true);
        mLeftButton.setBackgroundColor(loading);
        mLeftButton.setEnabled(false);
        setIconAlpha(mLeftButton, 0);
        setRightButtonVisible(true);
        mRightButton.setBackgroundColor(loading);
        mRightButton.setEnabled(false);
        setIconAlpha(mRightButton, 0);
        setSettingsChipVisible(true, false);
        mSettingsChip.setEnabled(false);
        mSettingsChip.setBackgroundTintList(ColorStateList.valueOf(loading));
    }

    protected static void setIconAlpha(WearCircularButton button, int alpha) {
        Drawable icon = button.getIcon();
        if (icon != null) icon.setAlpha(alpha);
    }

    protected int themeColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        getContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    @Nullable
    private RecyclerView.ViewHolder findFocusedHolder() {
        View centered = mList.findChildViewUnder(mList.getWidth() / 2f, mList.getHeight() / 2f);
        if (centered == null) {
            Log.w(TAG, "getFocusedViewHolder: couldn't find a centred view");
            return null;
        }
        return mList.getChildViewHolder(centered);
    }

    private static void announceFocus(RecyclerView.ViewHolder holder) {
        View preview = holder.itemView.findViewById(R.id.managementui_preview_container);
        if (preview != null) {
            preview.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            return;
        }
        View add = holder.itemView.findViewById(R.id.managementui_add_more_favorites_button);
        if (add != null) add.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
    }

    private void onFocusSettled() {
        RecyclerView.ViewHolder holder = findFocusedHolder();
        if (holder == null || mPresenter == null) return;
        announceFocus(holder);
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        if (position == mPresenter.getItemCount() - 1 && mPresenter.hasTrailingAddPlaceholder()) {
            mAtAddPlaceholder = true;
            mPresenter.onItemFocused(Presenter.NO_POSITION);
        } else {
            mAtAddPlaceholder = false;
            mPresenter.onItemFocused(position);
        }
        mPresenter.onFocusSettled();
    }

    private void fadeSideItems() {
        if (mPresenter == null) return;
        int center = mList.getWidth() / 2;
        for (int i = 0; i < mList.getChildCount(); i++) {
            View child = mList.getChildAt(i);
            if (!mPresenter.isItemPosition(mList.getChildAdapterPosition(child))) continue;
            float distance = Math.abs(center - (child.getLeft() + child.getRight()) / 2f);
            child.setAlpha(1f - 0.5f * Math.min(1f, distance / child.getWidth()));
        }
    }

    public void scrollToItem(int position) {
        int count = mPresenter == null ? 0 : mPresenter.getItemCount();
        if (count > 0) {
            if (position < 0 || position >= count) {
                throw new IllegalArgumentException("Bad position: " + position);
            }
        } else {
            position = 0;
        }
        mLayoutManager.scrollToPosition(position);
        mList.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                mList.removeOnLayoutChangeListener(this);
                int center = mList.getWidth() / 2;
                View centered = mList.findChildViewUnder(center, mList.getHeight() / 2f);
                if (centered != null) {
                    int offset = (centered.getLeft() + centered.getRight()) / 2 - center;
                    if (offset != 0) mList.scrollBy(offset, 0);
                }
                RecyclerView.ViewHolder holder = findFocusedHolder();
                if (holder != null) announceFocus(holder);
            }
        });
    }

    public void runForFocusedItem(IntConsumer action) {
        RecyclerView.ViewHolder holder = findFocusedHolder();
        if (holder == null) return;
        int position = holder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) action.accept(position);
    }

    public void startCarouselTracking() {
        stopCarouselTracking();
        mSnapHelper = new CarouselSnapHelper(mList.getResources().getDimensionPixelOffset(
                R.dimen.managementui_auto_transition_touch_slop));
        mSnapHelper.attachToRecyclerView(mList);
        mList.addOnScrollListener(mScrollListener);
        onFocusSettled();
    }

    private void stopCarouselTracking() {
        mList.removeOnScrollListener(mScrollListener);
        if (mSnapHelper != null) {
            mSnapHelper.attachToRecyclerView(null);
            mSnapHelper = null;
        }
    }

    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (mSnapHelper != null) mSnapHelper.onScrollStateChanged(newState);
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                if (mPresenter != null) mPresenter.onCarouselDragStarted();
            } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                onFocusSettled();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mSnapHelper != null) mSnapHelper.onScrolled(dx);
            fadeSideItems();
        }
    };

    private static final class CarouselSnapHelper extends LinearSnapHelper {
        private final int mSlop;
        private RecyclerView mRecycler;
        private int mScrollDelta;
        private boolean mFlung;
        private int mDragStartPosition = NONE;

        CarouselSnapHelper(int slop) {
            mSlop = slop;
        }

        @Override
        public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
            super.attachToRecyclerView(recyclerView);
            mRecycler = recyclerView != null
                    && recyclerView.getLayoutManager() instanceof LinearLayoutManager ? recyclerView : null;
        }

        @Override
        public boolean onFling(int velocityX, int velocityY) {
            mFlung = true;
            return super.onFling(velocityX, velocityY);
        }

        void onScrolled(int dx) {
            mScrollDelta += dx;
        }

        void onScrollStateChanged(int state) {
            if (state != RecyclerView.SCROLL_STATE_DRAGGING || mRecycler == null) return;
            mFlung = false;
            mScrollDelta = 0;
            RecyclerView.LayoutManager lm = mRecycler.getLayoutManager();
            View start = super.findSnapView(lm);
            mDragStartPosition = start != null ? lm.getPosition(start) : NONE;
        }

        @Nullable
        @Override
        public View findSnapView(RecyclerView.LayoutManager lm) {
            View nearest = super.findSnapView(lm);
            View result = nearest;
            if (nearest != null && !mFlung && Math.abs(mScrollDelta) > mSlop
                    && lm.getPosition(nearest) == mDragStartPosition && mRecycler != null) {
                result = nearestInDirection(lm, mScrollDelta);
            }
            mScrollDelta = 0;
            return result;
        }

        @Nullable
        private View nearestInDirection(RecyclerView.LayoutManager lm, int direction) {
            int center = mRecycler.getMeasuredWidth() / 2;
            View best = null;
            int bestOffset = direction > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            for (int i = 0; i < lm.getChildCount(); i++) {
                View child = lm.getChildAt(i);
                if (child == null) break;
                int offset = (child.getLeft() + child.getRight()) / 2 - center;
                if ((direction > 0 && offset >= 0 && offset < bestOffset)
                        || (direction < 0 && offset <= 0 && offset > bestOffset)) {
                    best = child;
                    bestOffset = offset;
                }
            }
            return best;
        }
    }

    public Animator createEnterAnimator() {
        float screenHeight = getResources().getDisplayMetrics().heightPixels;
        AnimatorSet set = new AnimatorSet();
        set.setStartDelay(150L);
        set.setDuration(300L);
        set.setInterpolator(mFastOutSlowIn);
        set.playTogether(
                ObjectAnimator.ofFloat(mSettingsArea, TRANSLATION_Y, screenHeight * 0.15f, 0f),
                ObjectAnimator.ofFloat(mSettingsArea, ALPHA, 0f, 1f));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mSettingsArea.setAlpha(0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mSettingsArea.setTranslationY(0f);
                mSettingsArea.setAlpha(1f);
            }
        });
        return set;
    }

    public void playEntryZoom() {
        float startScale = (float) getResources().getDisplayMetrics().widthPixels / mMetrics.previewWidth;
        mMainContainer.setScaleX(startScale);
        mMainContainer.setScaleY(startScale);
        mMainContainer.setPivotX(mList.getX() + mList.getWidth() / 2f);
        mMainContainer.setPivotY(mList.getY() + mList.getHeight() / 2f
                - getResources().getDimension(R.dimen.managementui_animation_offsety));

        AnimatorSet zoom = new AnimatorSet();
        zoom.playTogether(
                ObjectAnimator.ofFloat(mMainContainer, SCALE_X, 1f),
                ObjectAnimator.ofFloat(mMainContainer, SCALE_Y, 1f));
        zoom.setInterpolator(mFastOutSlowIn);
        zoom.setDuration(266L);
        zoom.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mEntering = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mEntering = false;
            }
        });

        if (!mFadeInOnEntry) {
            zoom.start();
        } else {
            setAlpha(0f);
            mFadeAnimator = animate().setDuration(300L).alpha(1f).withEndAction(zoom::start);
        }
    }

    public void startExitFade() {
        mFadeAnimator = animate()
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mEntering = true;
                    }
                })
                .setStartDelay(150L)
                .alpha(0f)
                .withEndAction(() -> {
                    if (mPresenter != null) mPresenter.onDismissRequested();
                });
    }

    protected final void hide() {
        Log.d(TAG, "Hiding picker view");
//        if (mAllItems.isClosing() || !(isAllItemsShown() || mAtAddPlaceholder)) {
//            setVisibility(GONE);
//        } else {
//            if (mHideAnimator != null) mHideAnimator.cancel();
//            mHideAnimator = createHideAnimator();
//            mHideAnimator.start();
//        }
        stopCarouselTracking();
        if (mPresenter != null) mPresenter.onPickerHidden();
        mDeletionDrawable.reset();
    }

    public void onHideRequested() {
        hide();
    }

    private Animator createHideAnimator() {
        float away = ResourcesCompat.getFloat(getResources(), R.dimen.sysui_common_activity_away_scale);
        Interpolator fastOutLinearIn = AnimationUtils.loadInterpolator(
                getContext(), android.R.interpolator.fast_out_linear_in);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, SCALE_X, 1f, away);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, SCALE_Y, 1f, away);
        scaleX.setDuration(350L);
        scaleY.setDuration(350L);
        scaleX.setInterpolator(mFastOutSlowIn);
        scaleY.setInterpolator(mFastOutSlowIn);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f);
        alpha.setDuration(300L);
        alpha.setInterpolator(fastOutLinearIn);
        AnimatorSet exit = new AnimatorSet();
        exit.playTogether(scaleX, scaleY, alpha);
        exit.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                resetAfterExit();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                resetAfterExit();
            }
        });

        Drawable dim = getContext().getDrawable(R.color.managementui_black_dim).mutate();
        ObjectAnimator dimAnimator = ObjectAnimator.ofInt(dim, "alpha", 0, 255);
        dimAnimator.setDuration(350L);
        dimAnimator.setInterpolator(fastOutLinearIn);
        dimAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setForeground(dim);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                setForeground(null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setForeground(null);
            }
        });

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        float maxRadius = Math.max(cx, cy);
        Animator reveal = ViewAnimationUtils.createCircularReveal(this, cx, cy, maxRadius, maxRadius * away);
        reveal.setDuration(700L);
        reveal.setInterpolator(fastOutLinearIn);

        AnimatorSet all = new AnimatorSet();
        all.playTogether(exit, dimAnimator, reveal);
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSwipeLayout.setVisibility(GONE);
                mMainContainer.setVisibility(VISIBLE);
                mAllItems.setListScrollingEnabled(true);
            }
        });
        return all;
    }

    private void resetAfterExit() {
        setVisibility(GONE);
        setScaleX(1f);
        setScaleY(1f);
        setAlpha(1f);
    }

    public boolean returnFromAllItems() {
        if (!isAllItemsShown()) return false;

        RecyclerView.ViewHolder holder = findFocusedHolder();
        View target = holder == null ? null
                : holder.itemView.findViewById(R.id.managementui_preview_image_border);
        View source = mPendingAllItemsPosition == NONE || mPresenter == null ? null
                : mPresenter.getAllItemsPreview(mPendingAllItemsPosition);

        if (target == null || source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            mAllItems.close();
        } else {
            playReturnTransition(source, target, holder);
            mPendingAllItemsPosition = NONE;
        }
        mMainContainer.setVisibility(VISIBLE);
        mAllItems.setListScrollingEnabled(true);
        return true;
    }

    private void playReturnTransition(View source, View target, RecyclerView.ViewHolder holder) {
        ViewGroupOverlay overlay = getOverlay();
        int[] me = new int[2];
        int[] from = new int[2];
        int[] to = new int[2];
        getLocationOnScreen(me);
        source.getLocationOnScreen(from);
        target.getLocationOnScreen(to);

        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        source.draw(new Canvas(snapshot));
        BitmapDrawable drawable = new BitmapDrawable(getResources(), snapshot);
        drawable.setBounds(0, 0, width, height);

        View ghost = new View(getContext());
        ghost.setBackground(drawable);
        int left = from[0] - me[0];
        int top = from[1] - me[1];
        ghost.layout(left, top, left + width, top + height);
        ghost.setPivotX(0f);
        ghost.setPivotY(0f);
        overlay.add(ghost);
        source.setAlpha(0f);

        AnimatorSet fly = new AnimatorSet();
        fly.setDuration(300L);
        fly.setInterpolator(mFastOutSlowIn);
        fly.playTogether(
                ObjectAnimator.ofFloat(ghost, ALPHA, 1f),
                ObjectAnimator.ofFloat(ghost, SCALE_X, target.getWidth() / (float) width),
                ObjectAnimator.ofFloat(ghost, SCALE_Y, target.getHeight() / (float) height),
                ObjectAnimator.ofFloat(ghost, X, to[0] - me[0]),
                ObjectAnimator.ofFloat(ghost, Y, to[1] - me[1]));

        ObjectAnimator fade = ObjectAnimator.ofFloat(mAllItems, ALPHA, 0f);
        fade.setDuration(300L);
        fade.setInterpolator(mAllItems.getEnterInterpolator());

        AnimatorSet all = new AnimatorSet();
        all.playTogether(fly, fade);
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(ghost);
                source.setAlpha(1f);
                mAllItems.setAlpha(1f);
                mAllItems.setVisibility(GONE);
                mSwipeLayout.setVisibility(GONE);
                mMainContainer.setVisibility(VISIBLE);
                mAddInProgress = false;
                restorePreviewAlpha(holder);
            }
        });
        all.start();
        createEnterAnimator().start();
    }

    private static void restorePreviewAlpha(RecyclerView.ViewHolder holder) {
        View preview = holder.itemView.findViewById(R.id.managementui_preview);
        if (preview != null) preview.setAlpha(1f);
        View name = holder.itemView.findViewById(R.id.managementui_item_name);
        if (name != null) name.setAlpha(1f);
    }

    public void showError(OnTouchListener onTouch) {
        if (mErrorView != null) {
            removeView(mErrorView);
            Log.w(TAG, "Found dangling error screen, cleaning up.");
        }
        mErrorView = LayoutInflater.from(getContext()).inflate(R.layout.management_ui_error_chip, this, false);
        addView(mErrorView);
        mErrorView.setOnTouchListener(onTouch);
    }

    public void clearError() {
        if (mErrorView != null) {
            removeView(mErrorView);
            mErrorView = null;
        }
    }

    public void showLoading(boolean showText) {
    }

    public void clearLoading() {
    }
}