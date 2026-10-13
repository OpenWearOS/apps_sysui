package io.github.openwearos.sysui.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.UiMode;
import io.github.openwearos.sysui.event.Bus;
import io.github.openwearos.sysui.event.Events;
import io.github.openwearos.sysui.notif.StreamListenerService;

final class StreamPane extends Pane {
	private static final String TAG = "OpenWearStream";
	private static final int MAX_ACTIONS = 3;

	private final LinearLayout mList;

	StreamPane(Context c) {
		super(c);
		mList = new LinearLayout(c);
		mList.setOrientation(LinearLayout.VERTICAL);
		int side = Ui.sideInset(c);
		mList.setPadding(side, Ui.dp(c, 40), side, Ui.dp(c, 40));
		ScrollView scroll = new ScrollView(c);
		scroll.addView(mList, new LayoutParams(Ui.MP, Ui.WC));
		addView(scroll, new LayoutParams(Ui.MP, Ui.MP));
		on(Events.StreamChanged.class, e -> rebuild());
	}

	@Override
	void onShown() {
		rebuild();
	}

	private void rebuild() {
		Context c = getContext();
		mList.removeAllViews();
		mList.addView(Ui.text(c, c.getString(R.string.notifications), 16, Ui.TEXT2),
				new LinearLayout.LayoutParams(Ui.MP, Ui.WC));
		java.util.List<StatusBarNotification> items = StreamListenerService.items();
		if (items.isEmpty()) {
			TextView empty = Ui.text(c, c.getString(R.string.no_notifications), 14, Ui.TEXT2);
			empty.setPadding(0, Ui.dp(c, 24), 0, 0);
			mList.addView(empty, new LinearLayout.LayoutParams(Ui.MP, Ui.WC));
			return;
		}
		for (StatusBarNotification sbn : items)
			mList.addView(card(sbn));
		mList.addView(clearAll());
	}

	private TextView clearAll() {
		Context c = getContext();
		TextView clear = Ui.text(c, c.getString(R.string.clear_all), 14, Ui.ACCENT);
		clear.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clear_all, 0, 0, 0);
		clear.setCompoundDrawableTintList(ColorStateList.valueOf(Ui.ACCENT));
		clear.setCompoundDrawablePadding(Ui.dp(c, 6));
		clear.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 12));
		clear.setOnClickListener(v -> StreamListenerService.dismissAll());
		clear.setLayoutParams(new LinearLayout.LayoutParams(Ui.MP, Ui.WC));
		return clear;
	}

	private LinearLayout card(StatusBarNotification sbn) {
		Notification n = sbn.getNotification();
		Bundle extras = n.extras;
		CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
		CharSequence body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
		if (TextUtils.isEmpty(body))
			body = extras.getCharSequence(Notification.EXTRA_TEXT);

		LinearLayout card = (LinearLayout) LayoutInflater.from(getContext())
				.inflate(R.layout.notification_card, mList, false);
		bind(card.findViewById(R.id.app_name), appLabel(sbn.getPackageName()));
		bind(card.findViewById(R.id.title), title);
		bind(card.findViewById(R.id.body), body);
		addActions(card.findViewById(R.id.actions), sbn);

		card.setOnClickListener(v -> open(sbn));
		card.setOnLongClickListener(v -> {
			StreamListenerService.dismiss(sbn.getKey());
			return true;
		});
		return card;
	}

	private static void bind(TextView view, CharSequence text) {
		view.setText(text);
		view.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
	}

	private void addActions(LinearLayout holder, StatusBarNotification sbn) {
		Notification.Action[] actions = sbn.getNotification().actions;
		if (actions == null)
			return;
		int shown = 0;
		for (Notification.Action action : actions) {
			if (shown == MAX_ACTIONS)
				break;
			if (action.actionIntent == null || TextUtils.isEmpty(action.title))
				continue;
			shown++;
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MP, Ui.WC);
			lp.topMargin = getResources().getDimensionPixelSize(R.dimen.chip_spacing);
			holder.addView(new WearChipButton(getContext(), action.title, () -> run(sbn, action)), lp);
		}
	}

	private void run(StatusBarNotification sbn, Notification.Action action) {
		RemoteInput[] inputs = action.getRemoteInputs();
		try {
			if (inputs != null && inputs.length > 0) {
				Intent ask = new Intent(RemoteInputActivity.ACTION)
						.setPackage(getContext().getPackageName())
						.putExtra(RemoteInputActivity.EXTRA_REMOTE_INPUTS, inputs)
						.putExtra(RemoteInputActivity.EXTRA_PENDING_INTENT, action.actionIntent)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getContext().startActivity(ask);
			} else {
				action.actionIntent.send();
			}
		} catch (PendingIntent.CanceledException | RuntimeException e) {
			Log.w(TAG, "cannot run action " + action.title, e);
		}
	}

	private String appLabel(String pkg) {
		PackageManager pm = getContext().getPackageManager();
		try {
			return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
		} catch (PackageManager.NameNotFoundException e) {
			return pkg;
		}
	}

	private void open(StatusBarNotification sbn) {
		PendingIntent pi = sbn.getNotification().contentIntent;
		if (pi == null)
			return;
		try {
			pi.send();
			if ((sbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) {
				StreamListenerService.dismiss(sbn.getKey());
			}
		} catch (PendingIntent.CanceledException e) {
			Log.w(TAG, "notification intent was cancelled", e);
		}
		Bus.post(new Events.UiModeRequest(UiMode.WATCH_FACE));
	}
}
