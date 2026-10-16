package io.github.openwearos.sysui.state;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;

import io.github.openwearos.sysui.event.Bus;
import io.github.openwearos.sysui.event.Events;

public final class StateTracker {
    private static final String ZEN_MODE = "zen_mode";
    private static final String BEDTIME_MODE = "bedtime_mode";
    private static final String THEATER_MODE = "theater_mode_on";

    private final Context mContext;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private boolean mStarted;

    public StateTracker(Context context) {
        mContext = context.getApplicationContext();
    }

    public void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        trackBattery();
        trackSettings();
        trackDisplay();
    }

    private void trackBattery() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                publishBattery(intent);
            }
        };
        Intent current = mContext.registerReceiver(receiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        if (current != null) {
            publishBattery(current);
        }
    }

    private static void publishBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        int percent = level < 0 || scale <= 0 ? 0 : Math.round(level * 100f / scale);
        Bus.post(new Events.Battery(percent, charging));
    }

    private void trackSettings() {
        ContentResolver cr = mContext.getContentResolver();
        ContentObserver observer = new ContentObserver(mMain) {
            @Override
            public void onChange(boolean selfChange) {
                publishSettings();
            }
        };
        for (String name : new String[] { ZEN_MODE, BEDTIME_MODE, THEATER_MODE, Settings.Global.AIRPLANE_MODE_ON }) {
            Uri uri = Settings.Global.getUriFor(name);
            cr.registerContentObserver(uri, false, observer);
        }
        publishSettings();
    }

    private void publishSettings() {
        ContentResolver cr = mContext.getContentResolver();
        Bus.post(new Events.Dnd(Settings.Global.getInt(cr, ZEN_MODE, 0) != 0));
        Bus.post(new Events.Bedtime(Settings.Global.getInt(cr, BEDTIME_MODE, 0) != 0));
        Bus.post(new Events.Theater(Settings.Global.getInt(cr, THEATER_MODE, 0) != 0));
        Bus.post(new Events.Airplane(Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) != 0));
    }

    private void trackDisplay() {
        DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        dm.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId == Display.DEFAULT_DISPLAY) {
                    publishAmbient(dm);
                }
            }
        }, mMain);
        publishAmbient(dm);
    }

    private static void publishAmbient(DisplayManager dm) {
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
        int state = display == null ? Display.STATE_ON : display.getState();
        Bus.post(new Events.Ambient(state == Display.STATE_DOZE || state == Display.STATE_DOZE_SUSPEND));
    }
}