package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

final class Ui {
    static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    static final int BG = 0xFF000000;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT2 = 0xFFB0B0B8;
    static final int ACCENT = 0xFF4DB6AC;

    private static final float SIDE_INSET_FRACTION = 0.1f;

    private Ui() {
    }

    static int dp(Context context, int dp) {
        return Math.round(context.getResources().getDisplayMetrics().density * dp);
    }

    static int sideInset(Context context) {
        return Math.round(context.getResources().getDisplayMetrics().widthPixels * SIDE_INSET_FRACTION);
    }

    static TextView text(Context context, CharSequence text, int sp, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        return view;
    }

    static TextClock clock(Context context, String format12, String format24, int sp, int color) {
        TextClock view = new TextClock(context);
        view.setFormat12Hour(format12);
        view.setFormat24Hour(format24);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        return view;
    }
}