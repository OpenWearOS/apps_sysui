package io.github.openwearos.sysui.notif;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import io.github.openwearos.sysui.event.Bus;
import io.github.openwearos.sysui.event.Events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StreamListenerService extends NotificationListenerService {
    private static volatile StreamListenerService sInstance;
    private static volatile List<StatusBarNotification> sItems = Collections.emptyList();

    public static List<StatusBarNotification> items() {
        return sItems;
    }

    public static void dismiss(String key) {
        StreamListenerService service = sInstance;
        if (service != null) {
            service.cancelNotification(key);
        }
    }

    public static void dismissAll() {
        StreamListenerService service = sInstance;
        if (service != null) {
            service.cancelAllNotifications();
        }
    }

    @Override
    public void onListenerConnected() {
        sInstance = this;
        refresh();
    }

    @Override
    public void onListenerDisconnected() {
        sInstance = null;
        sItems = Collections.emptyList();
        publish();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        refresh();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        refresh();
    }

    private void refresh() {
        List<StatusBarNotification> shown = new ArrayList<>();
        StatusBarNotification[] active = getActiveNotifications();
        if (active != null) {
            for (StatusBarNotification sbn : active) {
                int flags = sbn.getNotification().flags;
                if ((flags & (Notification.FLAG_GROUP_SUMMARY | Notification.FLAG_ONGOING_EVENT)) == 0) {
                    shown.add(sbn);
                }
            }
        }
        shown.sort((a, b) -> Long.compare(b.getPostTime(), a.getPostTime()));
        sItems = Collections.unmodifiableList(shown);
        publish();
    }

    private static void publish() {
        Bus.post(new Events.NotificationCount(sItems.size()));
        Bus.post(new Events.StreamChanged());
    }
}