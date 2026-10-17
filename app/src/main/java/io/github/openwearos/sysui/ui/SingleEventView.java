package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import io.github.openwearos.sysui.R;
import io.github.openwearos.sysui.calendar.EventInstance;
import io.github.openwearos.sysui.calendar.EventTimeFormatter;
import io.github.openwearos.sysui.ui.AmbientableTextView;

import java.util.function.Consumer;

public class SingleEventView extends LinearLayout {
    private AmbientableTextView mTitle;
    private AmbientableTextView mLocation;
    private AmbientableTextView mNoEventsTitle;
    private EventTimeView mTime;

    private EventInstance mEvent;
    private EventTimeFormatter mFormatter;
    private long mDayMillis;
    private Consumer<EventInstance> mClickListener;

    public SingleEventView(Context context) {
        super(context);
    }

    public SingleEventView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SingleEventView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitle = requireViewById(R.id.event_title);
        mLocation = requireViewById(R.id.event_location);
        mTime = requireViewById(R.id.event_time);
        mNoEventsTitle = requireViewById(R.id.no_events_title);
        setClipToOutline(true);
        setOnClickListener(v -> {
            Consumer<EventInstance> listener = mClickListener;
            if (listener != null && mEvent != null) listener.accept(mEvent);
        });
    }

    public void setOnEventClickListener(Consumer<EventInstance> listener) {
        mClickListener = listener;
    }

    public void bind(EventInstance event, EventTimeFormatter formatter, long dayMillis)
            throws EventTimeFormatter.EventOutsideDayException {
        mEvent = event;
        mFormatter = formatter;
        mDayMillis = dayMillis;
        setClickable(true);

        String title = event.title == null ? null : event.title.trim();
        mTitle.setText(TextUtils.isEmpty(title) ? getResources().getString(R.string.cal_no_title) : title);
        refresh();
    }

    public void refresh() throws EventTimeFormatter.EventOutsideDayException {
        if (mEvent == null || mFormatter == null) {
            mTime.hide();
        } else {
            EventTimeFormatter.TimeString time = mFormatter.format(mEvent, mDayMillis);
            if (time.highlighted) mTime.showRelative(time.text);
            else mTime.showAbsolute(time.text);
        }

        if (mEvent == null || TextUtils.isEmpty(mEvent.location)) {
            mLocation.setVisibility(GONE);
        } else {
            mLocation.setText(mEvent.location);
            mLocation.setVisibility(VISIBLE);
        }
    }

    public void showNoEvents() {
        mEvent = null;
        mFormatter = null;
        Resources res = getResources();
        int padding = (int) res.getDimension(R.dimen.agenda_list_event_padding);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                (int) res.getDimension(R.dimen.agenda_list_no_event_card_height));
        params.gravity = Gravity.CENTER_VERTICAL;
        setLayoutParams(params);
        setPadding(padding, padding, padding, padding);
        setGravity(Gravity.CENTER_VERTICAL);
        mLocation.setVisibility(GONE);
        mTime.setVisibility(GONE);
        mTitle.setVisibility(GONE);
        mNoEventsTitle.setVisibility(VISIBLE);
        setClickable(false);
    }

    public EventInstance getEvent() {
        return mEvent;
    }

    public AmbientableTextView getTitleView() {
        return mTitle;
    }

    public AmbientableTextView getLocationView() {
        return mLocation;
    }

    public EventTimeView getTimeView() {
        return mTime;
    }

    public AmbientableTextView getNoEventsTitleView() {
        return mNoEventsTitle;
    }
}