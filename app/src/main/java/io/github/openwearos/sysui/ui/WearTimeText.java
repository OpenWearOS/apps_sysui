package io.github.openwearos.sysui.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.wear.widget.CurvedTextView;

import java.util.Calendar;
import java.util.Locale;
import java.util.function.LongSupplier;

import io.github.openwearos.sysui.R;

public class WearTimeText extends FrameLayout {
    private static final String DIVIDER = "·";

    private static final class Label {
        final View view;
        private final TextView straight;
        private final CurvedTextView curved;

        Label(View view) {
            this.view = view;
            if (view instanceof TextView) {
                straight = (TextView) view;
                curved = null;
            } else if (view instanceof CurvedTextView) {
                straight = null;
                curved = (CurvedTextView) view;
            } else {
                throw new IllegalArgumentException("Expected TextView or CurvedTextView");
            }
        }

        void setText(CharSequence text) {
            if (straight != null) straight.setText(text);
            else curved.setText(text != null ? text.toString() : "");
        }

        void setColor(int color) {
            if (straight != null) straight.setTextColor(color);
            else curved.setTextColor(color);
        }
    }

    private final Label mClockLabel;
    private final Label mDividerLabel;
    private final Label mTitleLabel;
    private final float mMaxFadeScroll;

    private final IntentFilter mTimeFilter = new IntentFilter();
    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                mCalendar = Calendar.getInstance();
            }
            updateTime();
        }
    };
    private boolean mReceiverRegistered;

    private final ContentObserver mFormatObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    refreshHourFormat();
                    updateTime();
                }
            };
    private boolean mObserverRegistered;

    private Calendar mCalendar = Calendar.getInstance();
    private LongSupplier mClock = System::currentTimeMillis;
    private boolean m24Hour;

    public WearTimeText(Context context) {
        this(context, null);
    }

    public WearTimeText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearTimeText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boolean round = context.getResources().getConfiguration().isScreenRound();
        LayoutInflater.from(context).inflate(
                round ? R.layout.curved_time_text : R.layout.straight_time_text, this, true);
        mClockLabel = new Label(findViewById(R.id.wear_time_text_clock));
        mDividerLabel = new Label(findViewById(R.id.wear_time_text_divider));
        mTitleLabel = new Label(findViewById(R.id.wear_time_text_title));
        mDividerLabel.setText(DIVIDER);

        mMaxFadeScroll = getResources().getDimensionPixelSize(R.dimen.wear_time_max_fade_out_scroll);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearTimeText, defStyleAttr, 0);
        try {
            int color = a.getColor(R.styleable.WearTimeText_android_titleTextColor, -1);
            mTitleLabel.setColor(color);
            mDividerLabel.setColor(color);

            String title = a.getString(R.styleable.WearTimeText_titleText);
            mTitleLabel.setText(title);
            int visibility = TextUtils.isEmpty(title) ? GONE : VISIBLE;
            mTitleLabel.view.setVisibility(visibility);
            mDividerLabel.view.setVisibility(visibility);
        } finally {
            a.recycle();
        }

        mTimeFilter.addAction(Intent.ACTION_TIME_TICK);
        mTimeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mTimeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    }

    public void setScrollOffset(float scrollPx) {
        float t = Math.max(0f, Math.min(1f, scrollPx / mMaxFadeScroll));
        float scale = 1f - 0.5f * t;
        setAlpha(scale);
        setScaleX(scale);
        setScaleY(scale);
        setTranslationY(Math.min(0f, -scrollPx));
    }

    public void setClock(LongSupplier clock) {
        mClock = clock != null ? clock : System::currentTimeMillis;
        updateTime();
    }

    private void refreshHourFormat() {
        m24Hour = DateFormat.is24HourFormat(getContext());
    }

    private void updateTime() {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), m24Hour ? "Hm" : "hm");
        mCalendar.setTimeInMillis(mClock.getAsLong());
        mClockLabel.setText(DateFormat.format(pattern.replace("a", "").trim(), mCalendar));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshHourFormat();
        updateTime();

        if (!mReceiverRegistered) {
            getContext().registerReceiver(mTimeReceiver, mTimeFilter, Context.RECEIVER_NOT_EXPORTED);
            mReceiverRegistered = true;
        }
        if (!mObserverRegistered) {
            getContext().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.TIME_12_24), true, mFormatObserver);
            mObserverRegistered = true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mReceiverRegistered) {
            getContext().unregisterReceiver(mTimeReceiver);
            mReceiverRegistered = false;
        }
        if (mObserverRegistered) {
            getContext().getContentResolver().unregisterContentObserver(mFormatObserver);
            mObserverRegistered = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setPivotY(getPaddingTop() + getResources().getDimensionPixelSize(R.dimen.wear_time_text_size));
        setPivotX(getMeasuredWidth() / 2f);
    }
}