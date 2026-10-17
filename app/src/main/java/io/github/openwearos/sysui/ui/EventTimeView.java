package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.ui.AmbientableTextView;

public class EventTimeView extends LinearLayout {
    private final AmbientableTextView mAbsolute;
    private final AmbientableTextView mRelative;

    public EventTimeView(Context context) {
        this(context, null);
    }

    public EventTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.event_time_view, this);
        mAbsolute = findViewById(R.id.event_time_absolute);
        mRelative = findViewById(R.id.event_time_relative);
    }

    public void hide() {
        mAbsolute.setVisibility(GONE);
        mRelative.setVisibility(GONE);
    }

    public void showRelative(CharSequence text) {
        mRelative.setText(text);
        mRelative.setVisibility(VISIBLE);
        mAbsolute.setVisibility(GONE);
    }

    public void showAbsolute(CharSequence text) {
        mAbsolute.setText(text);
        mAbsolute.setTypeface(Typeface.create(mRelative.getTypeface(), Typeface.NORMAL));
        mAbsolute.setVisibility(VISIBLE);
        mRelative.setVisibility(GONE);
    }

    public AmbientableTextView getAbsoluteView() {
        return mAbsolute;
    }

    public AmbientableTextView getRelativeView() {
        return mRelative;
    }
}