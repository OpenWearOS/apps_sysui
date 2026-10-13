package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import io.github.openwearos.sysui.UiMode;
import io.github.openwearos.sysui.event.Bus;
import io.github.openwearos.sysui.event.Events;

import java.util.EnumMap;
import java.util.Map;

public final class RootView extends FrameLayout {
	private final Map<UiMode, Pane> mPanes = new EnumMap<>(UiMode.class);
	private final GestureDetector mGestures;
	private Runnable mUnsubscribe;
	private UiMode mMode;

	public RootView(Context c) {
		super(c);
		setBackgroundColor(Ui.BG);
		mPanes.put(UiMode.WATCH_FACE, new WatchFacePane(c));
		mPanes.put(UiMode.STREAM, new StreamPane(c));
		mPanes.put(UiMode.QUICK_SETTINGS, new QuickSettingsPane(c));
		mPanes.put(UiMode.LAUNCHER, new LauncherPane(c));
		for (Pane p : mPanes.values())
			addView(p, new LayoutParams(Ui.MP, Ui.MP));

		mGestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}

			@Override
			public boolean onFling(MotionEvent down, MotionEvent up, float vx, float vy) {
				if (mMode != UiMode.WATCH_FACE || down == null)
					return false;
				float dx = up.getX() - down.getX();
				float dy = up.getY() - down.getY();
				if (Math.abs(dy) <= Math.abs(dx))
					return false;
				requestMode(dy < 0 ? UiMode.STREAM : UiMode.QUICK_SETTINGS);
				return true;
			}
		});
		show(UiMode.WATCH_FACE);
	}

	public UiMode mode() {
		return mMode;
	}

	public static void requestMode(UiMode mode) {
		Bus.post(new Events.UiModeRequest(mode));
	}

	public boolean back() {
		if (mMode == UiMode.WATCH_FACE)
			return false;
		requestMode(UiMode.WATCH_FACE);
		return true;
	}

	private void show(UiMode mode) {
		if (mode == mMode)
			return;
		mMode = mode;
		for (Map.Entry<UiMode, Pane> e : mPanes.entrySet()) {
			e.getValue().setVisibility(e.getKey() == mode ? VISIBLE : GONE);
		}
		mPanes.get(mode).onShown();
		Bus.post(new Events.UiModeChange(mode));
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mUnsubscribe = Bus.onNext(Events.UiModeRequest.class, e -> show(e.mode));
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mUnsubscribe != null)
			mUnsubscribe.run();
		mUnsubscribe = null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestures.onTouchEvent(event);
		return mMode == UiMode.WATCH_FACE;
	}
}
