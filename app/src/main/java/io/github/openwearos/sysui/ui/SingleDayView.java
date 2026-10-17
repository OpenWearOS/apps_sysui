package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.ui.AmbientableTextView;

import java.util.ArrayList;
import java.util.List;

public class SingleDayView extends LinearLayout {
    private final List<SingleEventView> mEventViews = new ArrayList<>();
    private AmbientableTextView mDayHeader;
    private AmbientableTextView mFirstDayHeader;
    private LinearLayout mEventsContainer;

    public SingleDayView(Context context) {
        super(context);
    }

    public SingleDayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SingleDayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDayHeader = requireViewById(R.id.day_header);
        mFirstDayHeader = requireViewById(R.id.first_day_header);
        mEventsContainer = requireViewById(R.id.day_events);
    }

    public void setHeader(CharSequence text, CharSequence contentDescription, boolean first) {
        AmbientableTextView shown = first ? mFirstDayHeader : mDayHeader;
        AmbientableTextView hidden = first ? mDayHeader : mFirstDayHeader;
        shown.setText(text);
        shown.setContentDescription(contentDescription);
        shown.setVisibility(VISIBLE);
        hidden.setVisibility(GONE);
    }

    public void addEventView(SingleEventView view) {
        mEventsContainer.addView(view);
        mEventViews.add(view);
    }

    public void clearEventViews() {
        mEventsContainer.removeAllViews();
        mEventViews.clear();
    }

    public List<SingleEventView> getEventViews() {
        return mEventViews;
    }

    public LinearLayout getEventsContainer() {
        return mEventsContainer;
    }

    public AmbientableTextView getDayHeader() {
        return mDayHeader;
    }

    public AmbientableTextView getFirstDayHeader() {
        return mFirstDayHeader;
    }
}
