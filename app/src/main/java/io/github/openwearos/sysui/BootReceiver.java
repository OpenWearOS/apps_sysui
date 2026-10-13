package io.github.openwearos.sysui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import io.github.openwearos.sysui.notif.StreamListenerService;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "OpenWearBoot";
    private static final String LISTENERS = "enabled_notification_listeners";

    @Override
    public void onReceive(Context context, Intent intent) {
        grantListenerAccess(context);
    }

    static void grantListenerAccess(Context context) {
        String me = new ComponentName(context, StreamListenerService.class).flattenToString();
        ContentResolver cr = context.getContentResolver();
        String current = Settings.Secure.getString(cr, LISTENERS);
        if (!TextUtils.isEmpty(current)) {
            for (String entry : current.split(":")) {
                if (entry.equals(me)) return;
            }
        }
        String next = TextUtils.isEmpty(current) ? me : current + ":" + me;
        try {
            Settings.Secure.putString(cr, LISTENERS, next);
        } catch (SecurityException e) {
            Log.w(TAG, "cannot grant notification access (not privileged?)", e);
        }
    }
}
