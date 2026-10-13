package io.github.openwearos.sysui;

import android.app.Application;

import io.github.openwearos.sysui.state.StateTracker;

public class SysUiApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		new StateTracker(this).start();
	}
}
