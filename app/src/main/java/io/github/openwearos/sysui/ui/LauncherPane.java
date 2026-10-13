package io.github.openwearos.sysui.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import io.github.openwearos.sysui.R;

import java.util.ArrayList;
import java.util.List;

final class LauncherPane extends Pane {
	private static final String TAG = "OpenWearLauncher";

	private final List<ResolveInfo> mApps = new ArrayList<>();
	private final BaseAdapter mAdapter;

	LauncherPane(Context c) {
		super(c);
		ListView list = new ListView(c);
		list.setDivider(null);
		list.setDividerHeight(c.getResources().getDimensionPixelSize(R.dimen.launcher_row_spacing));
		list.setClipToPadding(false);
		int side = Ui.sideInset(c);
		list.setPadding(side, Ui.dp(c, 40), side, Ui.dp(c, 40));
		mAdapter = new BaseAdapter() {
			@Override
			public int getCount() {
				return mApps.size();
			}

			@Override
			public Object getItem(int position) {
				return mApps.get(position);
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return row(mApps.get(position), convertView, parent);
			}
		};
		list.setAdapter(mAdapter);
		list.setOnItemClickListener((p, v, position, id) -> launch(mApps.get(position)));
		addView(list, new LayoutParams(Ui.MP, Ui.MP));
	}

	@Override
	void onShown() {
		Context c = getContext();
		PackageManager pm = c.getPackageManager();
		Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
		mApps.clear();
		for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
			if (!c.getPackageName().equals(ri.activityInfo.packageName))
				mApps.add(ri);
		}
		mApps.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
		mAdapter.notifyDataSetChanged();
	}

	private View row(ResolveInfo ri, View convertView, ViewGroup parent) {
		Context c = getContext();
		PackageManager pm = c.getPackageManager();
		View row = convertView != null
				? convertView
				: LayoutInflater.from(c).inflate(R.layout.launcher_row, parent, false);
		((ImageView) row.findViewById(R.id.icon)).setImageDrawable(ri.loadIcon(pm));
		((TextView) row.findViewById(R.id.label)).setText(ri.loadLabel(pm));
		return row;
	}

	private void launch(ResolveInfo ri) {
		Intent i = new Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_LAUNCHER)
				.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		try {
			getContext().startActivity(i);
		} catch (RuntimeException e) {
			Log.w(TAG, "cannot launch " + ri.activityInfo.packageName, e);
		}
	}
}
