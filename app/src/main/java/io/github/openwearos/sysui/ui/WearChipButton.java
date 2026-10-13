package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.TextView;

import io.github.openwearos.sysui.R;

public final class WearChipButton extends TextView {
	public WearChipButton(Context c, CharSequence label, Runnable onTap) {
		super(c);
		setText(label);
		setTextSize(14);
		setTextColor(Ui.TEXT);
		setGravity(Gravity.CENTER);
		setMaxLines(1);
		setEllipsize(TextUtils.TruncateAt.END);
		setMinHeight(Ui.dp(c, 40));
		int h = Ui.dp(c, 12);
		int v = Ui.dp(c, 8);
		setPadding(h, v, h, v);
		setBackgroundResource(R.drawable.bg_chip);
		setOnClickListener(x -> onTap.run());
	}
}
