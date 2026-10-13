package io.github.openwearos.sysui.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.event.Events;

final class QuickSettingsPane extends Pane {
	private static final String TAG = "OpenWearQs";
	private static final int[] BRIGHTNESS_STEPS = { 20, 80, 160, 255 };

	private final QsTile mDnd;
	private final QsTile mTheater;
	private final QsTile mAirplane;
	private final QsTile mBattery;
	private final QsTile mBrightness;
	private boolean mDndOn;
	private boolean mBedtimeOn;
	private boolean mTheaterOn;
	private boolean mAirplaneOn;

	QuickSettingsPane(Context c) {
		super(c);
		mDnd = new QsTile(c, R.string.dnd, R.drawable.ic_dnd, this::toggleDnd);
		mTheater = new QsTile(c, R.string.theater, R.drawable.ic_theater, this::toggleTheater);
		mAirplane = new QsTile(c, R.string.airplane, R.drawable.ic_airplane, this::toggleAirplane);
		mBattery = new QsTile(c, R.string.battery_saver, R.drawable.ic_battery_saver, this::toggleBatterySaver);
		mBrightness = new QsTile(c, R.string.brightness, R.drawable.ic_brightness, this::cycleBrightness);
		QsTile settings = new QsTile(c, R.string.settings, R.drawable.ic_settings, this::openSettings);

		LinearLayout column = new LinearLayout(c);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setGravity(Gravity.CENTER_HORIZONTAL);
		int side = Ui.sideInset(c);
		column.setPadding(side, Ui.dp(c, 40), side, Ui.dp(c, 40));
		addRow(column, mDnd, mTheater);
		addRow(column, mAirplane, mBattery);
		addRow(column, mBrightness, settings);

		ScrollView scroll = new ScrollView(c);
		scroll.addView(column, new LayoutParams(Ui.MP, Ui.WC));
		addView(scroll, new LayoutParams(Ui.MP, Ui.MP));

		on(Events.Dnd.class, e -> {
			mDndOn = e.on;
			refresh();
		});
		on(Events.Bedtime.class, e -> {
			mBedtimeOn = e.on;
			refresh();
		});
		on(Events.Theater.class, e -> {
			mTheaterOn = e.on;
			refresh();
		});
		on(Events.Airplane.class, e -> {
			mAirplaneOn = e.on;
			refresh();
		});
		refresh();
	}

	private void addRow(LinearLayout column, QsTile a, QsTile b) {
		LinearLayout row = new LinearLayout(getContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.addView(a, cell());
		row.addView(b, cell());
		column.addView(row, new LinearLayout.LayoutParams(Ui.MP, Ui.WC));
	}

	private LinearLayout.LayoutParams cell() {
		int height = getResources().getDimensionPixelSize(R.dimen.qs_tile_height);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
		int m = Ui.dp(getContext(), 4);
		lp.setMargins(m, m, m, m);
		return lp;
	}

	@Override
	void onShown() {
		refresh();
	}

	private String onOff(boolean on) {
		return getContext().getString(on ? R.string.state_on : R.string.state_off);
	}

	private void refresh() {
		Context c = getContext();
		mDnd.set(mDndOn || mBedtimeOn, mBedtimeOn ? c.getString(R.string.bedtime) : onOff(mDndOn));
		mTheater.set(mTheaterOn, onOff(mTheaterOn));
		mAirplane.set(mAirplaneOn, onOff(mAirplaneOn));
		mBattery.set(batterySaverOn(), onOff(batterySaverOn()));
		mBrightness.set(false, c.getString(R.string.percent, brightness() * 100 / 255));
	}

	private ContentResolver cr() {
		return getContext().getContentResolver();
	}

	private boolean batterySaverOn() {
		return Settings.Global.getInt(cr(), "low_power", 0) == 1;
	}

	private int brightness() {
		return Settings.System.getInt(cr(), Settings.System.SCREEN_BRIGHTNESS, 128);
	}

	private void putGlobal(String name, int value) {
		try {
			Settings.Global.putInt(cr(), name, value);
		} catch (RuntimeException e) {
			Log.w(TAG, "cannot write " + name, e);
		}
	}

	private void toggleDnd() {
		if (mBedtimeOn)
			putGlobal("bedtime_mode", 0);
		else
			putGlobal("zen_mode", mDndOn ? 0 : 1);
	}

	private void toggleTheater() {
		putGlobal("theater_mode_on", mTheaterOn ? 0 : 1);
	}

	private void toggleAirplane() {
		try {
			// getContext().getSystemService(ConnectivityManager.class).setAirplaneMode(!mAirplaneOn);
		} catch (RuntimeException e) {
			Log.w(TAG, "cannot change airplane mode", e);
		}
	}

	private void toggleBatterySaver() {
		putGlobal("low_power", batterySaverOn() ? 0 : 1);
		refresh();
	}

	private void cycleBrightness() {
		int current = brightness();
		int next = BRIGHTNESS_STEPS[0];
		for (int step : BRIGHTNESS_STEPS) {
			if (step > current) {
				next = step;
				break;
			}
		}
		try {
			Settings.System.putInt(cr(), Settings.System.SCREEN_BRIGHTNESS_MODE,
					Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			Settings.System.putInt(cr(), Settings.System.SCREEN_BRIGHTNESS, next);
		} catch (RuntimeException e) {
			Log.w(TAG, "cannot change brightness", e);
		}
		refresh();
	}

	private void openSettings() {
		try {
			getContext().startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} catch (RuntimeException e) {
			Log.w(TAG, "no settings app", e);
		}
	}
}
