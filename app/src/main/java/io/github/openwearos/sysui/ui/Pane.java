package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.widget.FrameLayout;

import io.github.openwearos.sysui.event.Bus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract class Pane extends FrameLayout {
    private interface Binder {
        Runnable bind();
    }

    private final List<Binder> mBinders = new ArrayList<>();
    private final List<Runnable> mUnsubscribers = new ArrayList<>();

    Pane(Context context) {
        super(context);
    }

    void onShown() {
    }

    protected <T> void on(Class<T> type, Consumer<T> handler) {
        Binder binder = () -> Bus.subscribe(type, handler, true);
        mBinders.add(binder);
        if (isAttachedToWindow()) {
            mUnsubscribers.add(binder.bind());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (Binder binder : mBinders) {
            mUnsubscribers.add(binder.bind());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        for (Runnable unsubscribe : mUnsubscribers) {
            unsubscribe.run();
        }
        mUnsubscribers.clear();
        super.onDetachedFromWindow();
    }
}