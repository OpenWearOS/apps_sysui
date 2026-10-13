package io.github.openwearos.sysui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import io.github.openwearos.sysui.ui.RootView;

public class SysUiActivity extends Activity {
	private RootView mRoot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setShowWhenLocked(true);
		getWindow().setDecorFitsSystemWindows(false);
		mRoot = new RootView(this);
		setContentView(mRoot);
		hideSystemBars();
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideSystemBars();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (!Intent.ACTION_MAIN.equals(intent.getAction()))
			return;
		if (mRoot.mode() == UiMode.WATCH_FACE)
			RootView.requestMode(UiMode.LAUNCHER);
		else
			RootView.requestMode(UiMode.WATCH_FACE);
	}

	@Override
	public void onBackPressed() {
		mRoot.back();
	}

	private void hideSystemBars() {
		WindowInsetsController c = getWindow().getInsetsController();
		if (c == null)
			return;
		c.hide(WindowInsets.Type.systemBars());
		c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
	}
}
