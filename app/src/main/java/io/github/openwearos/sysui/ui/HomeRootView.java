package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import io.github.openwearos.sysui.UiMode;
import io.github.openwearos.sysui.event.Bus;
import io.github.openwearos.sysui.event.Events;
import io.github.openwearos.sysui.ui.PositionLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HomeRootView extends PositionLayout implements LifecycleEventObserver {
    private static final class ModeEntry {
        final UiMode mode;
        final View view;
        final AccessibilityNodeInfo.AccessibilityAction action;
        final Runnable onAction;

        ModeEntry(UiMode mode, View view, AccessibilityNodeInfo.AccessibilityAction action,
                  Runnable onAction) {
            this.mode = mode;
            this.view = view;
            this.action = action;
            this.onAction = onAction;
        }
    }

    private final List<ModeEntry> mEntries = new CopyOnWriteArrayList<>();
    private final AccessibilityManager mA11yManager;
    private UiMode mMode = UiMode.WATCH_FACE;
    private Lifecycle.Event mLastLifecycleEvent;
    private Runnable mUnsubscribe;

    public HomeRootView(Context context) {
        this(context, null, 0);
    }

    public HomeRootView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeRootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mA11yManager = context.getSystemService(AccessibilityManager.class);
        setAccessibilityDelegate(new ModeAccessibilityDelegate());
    }

    public void addModeViewBehind(View view, UiMode mode) {
        ensureLayoutParams(view);
        addView(view, 0);
        register(view, mode, null, null);
    }

    public void addModeView(View view, UiMode mode,
                            AccessibilityNodeInfo.AccessibilityAction action, Runnable onAction) {
        ensureLayoutParams(view);
        addView(view);
        register(view, mode, action, onAction);
    }

    private static void ensureLayoutParams(View view) {
        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void register(View view, UiMode mode,
                          AccessibilityNodeInfo.AccessibilityAction action, Runnable onAction) {
        if (action != null) {
            for (ModeEntry existing : mEntries) {
                if (action.equals(existing.action)) {
                    throw new IllegalArgumentException("Duplicate UI modes registered for a11y action "
                            + action + ": " + existing.mode + " and " + mode);
                }
            }
        }
        mEntries.add(new ModeEntry(mode, view, action, onAction));
    }

    public UiMode getMode() {
        return mMode;
    }

    private void setMode(UiMode mode) {
        if (mode == mMode) return;
        mMode = mode;
        interruptA11yIfShowing();
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (event != mLastLifecycleEvent) {
            mLastLifecycleEvent = event;
            interruptA11yIfShowing();
        }
    }

    private void interruptA11yIfShowing() {
        if (getWindowVisibility() == VISIBLE
                && mLastLifecycleEvent == Lifecycle.Event.ON_RESUME
                && mA11yManager.isEnabled()) {
            mA11yManager.interrupt();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mUnsubscribe = Bus.subscribe(Events.UiModeChange.class, e -> setMode(e.mode), true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mUnsubscribe != null) mUnsubscribe.run();
        mUnsubscribe = null;
        super.onDetachedFromWindow();
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> outChildren) {
        super.addChildrenForAccessibility(outChildren);
        for (ModeEntry entry : mEntries) {
            if (entry.mode == mMode) continue;
            outChildren.removeIf(child -> isSameOrDescendant(child, entry.view));
        }
    }

    private static boolean isSameOrDescendant(View candidate, View ancestor) {
        if (candidate == ancestor) return true;
        ViewParent parent = candidate.getParent();
        while (parent != null) {
            if (parent == ancestor) return true;
            parent = parent.getParent();
        }
        return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private final class ModeAccessibilityDelegate extends View.AccessibilityDelegate {
        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (mMode != UiMode.WATCH_FACE) return;
            info.setScrollable(true);
            for (ModeEntry entry : mEntries) {
                if (entry.action != null) info.addAction(entry.action);
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (mMode == UiMode.WATCH_FACE) {
                for (ModeEntry entry : mEntries) {
                    if (entry.action != null && entry.action.getId() == action
                            && entry.onAction != null) {
                        entry.onAction.run();
                        return true;
                    }
                }
            }
            return super.performAccessibilityAction(host, action, args);
        }
    }
}