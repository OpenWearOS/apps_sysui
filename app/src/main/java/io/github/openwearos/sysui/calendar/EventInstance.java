package io.github.openwearos.sysui.calendar;

import android.content.ContentUris;
import android.content.Intent;
import android.provider.CalendarContract;

public final class EventInstance {
    public final long id;
    public final long eventId;
    public final String title;
    public final long beginMillis;
    public final long endMillis;
    public final boolean allDay;
    public final String location;

    public EventInstance(long id, long eventId, String title, long beginMillis, long endMillis,
                         boolean allDay, String location) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.beginMillis = beginMillis;
        this.endMillis = endMillis;
        this.allDay = allDay;
        this.location = location;
    }

    public Intent toViewIntent() {
        return new Intent(Intent.ACTION_VIEW)
                .setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
