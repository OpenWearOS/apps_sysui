package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.event.Events;

import java.util.ArrayList;
import java.util.List;

final class WatchFacePane extends Pane {
	private final TextClock mTime;
	private final TextClock mDate;
	private final TextView mStatus;
	private final BatteryArc mArc;
	private boolean mAmbient;
	private boolean mDnd;
	private boolean mBedtime;
	private boolean mTheater;
	private boolean mAirplane;
	private int mNotifications;

	WatchFacePane(Context c) {
		super(c);
		mArc = new BatteryArc(c);
		addView(mArc, new LayoutParams(Ui.MP, Ui.MP));

		LinearLayout column = new LinearLayout(c);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setGravity(Gravity.CENTER);
		mTime = Ui.clock(c, "h:mm", "HH:mm", 56, Ui.TEXT);
		mDate = Ui.clock(c, "EEE, MMM d", "EEE, MMM d", 16, Ui.TEXT2);
		mStatus = Ui.text(c, "", 13, Ui.ACCENT);
		column.addView(mTime);
		column.addView(mDate);
		column.addView(mStatus);
		addView(column, new LayoutParams(Ui.MP, Ui.MP));

		on(Events.Battery.class, e -> mArc.setBattery(e.percent, e.charging));
		on(Events.Ambient.class, e -> {
			mAmbient = e.ambient;
			render();
		});
		on(Events.Dnd.class, e -> {
			mDnd = e.on;
			render();
		});
		on(Events.Bedtime.class, e -> {
			mBedtime = e.on;
			render();
		});
		on(Events.Theater.class, e -> {
			mTheater = e.on;
			render();
		});
		on(Events.Airplane.class, e -> {
			mAirplane = e.on;
			render();
		});
		on(Events.NotificationCount.class, e -> {
			mNotifications = e.count;
			render();
		});
		render();
	}

	private void render() {
		Context c = getContext();
		mTime.setTextColor(mAmbient ? Ui.TEXT2 : Ui.TEXT);
		mDate.setVisibility(mAmbient ? GONE : VISIBLE);
		mStatus.setVisibility(mAmbient ? GONE : VISIBLE);
		mArc.setHidden(mAmbient);

		List<String> parts = new ArrayList<>();
		if (mNotifications > 0)
			parts.add(c.getResources().getQuantityString(
					R.plurals.notification_count, mNotifications, mNotifications));
		if (mBedtime)
			parts.add(c.getString(R.string.bedtime));
		else if (mDnd)
			parts.add(c.getString(R.string.dnd));
		if (mTheater)
			parts.add(c.getString(R.string.theater));
		if (mAirplane)
			parts.add(c.getString(R.string.airplane));
		mStatus.setText(String.join(c.getString(R.string.status_separator), parts));
	}
}
