package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;

import io.github.openwearos.sysui.R;

final class QsTile extends androidx.appcompat.widget.AppCompatTextView {
	private final String mLabel;

	QsTile(Context context, int labelRes, int iconRes, Runnable onTap) {
		super(context);
		mLabel = context.getString(labelRes);
		setGravity(Gravity.CENTER);
		setTextSize(13);
		setCompoundDrawablesWithIntrinsicBounds(0, iconRes, 0, 0);
		setCompoundDrawablePadding(Ui.dp(context, 2));
		setOnClickListener(v -> onTap.run());
		set(false, "");
	}

	void set(boolean on, String detail) {
		setText(detail.isEmpty() ? mLabel : mLabel + "\n" + detail);
		int color = getContext().getColor(on ? R.color.on_accent : R.color.text);
		setTextColor(color);
		setCompoundDrawableTintList(ColorStateList.valueOf(color));
		setBackgroundResource(on ? R.drawable.bg_tile_on : R.drawable.bg_tile_off);
		int pad = Ui.dp(getContext(), 6);
		setPadding(pad, pad, pad, pad);
	}
}
