package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import io.github.openwearos.sysui.R;

public class WearInlineSliderPreference extends Preference {
    private float mValue;
    private final float mMin;
    private final float mMax;
    private final float mIncrement;
    private final boolean mShowSeparators;
    private final boolean mShowMinimumIncrement;
    private CharSequence mDecrementDescription;
    private CharSequence mIncrementDescription;
    private CharSequence mProgressDescription;

    public WearInlineSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.inlineSliderPreferenceStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearInlineSliderPreference,
                R.attr.inlineSliderPreferenceStyle, 0);
        mMin = a.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderMinValue, 0.0f);
        mMax = a.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderMaxValue, 1.0f);
        mShowMinimumIncrement = a.getBoolean(
                R.styleable.WearInlineSliderPreference_preferenceSliderShowMinimumIncrement, false);
        mIncrement = a.getFloat(R.styleable.WearInlineSliderPreference_preferenceSliderIncrement, 1.0f);
        mShowSeparators = a.getBoolean(
                R.styleable.WearInlineSliderPreference_preferenceSliderIncrementSeparators, false);
        if (a.hasValue(R.styleable.WearInlineSliderPreference_decrementContentDescription)) {
            mDecrementDescription = a.getString(R.styleable.WearInlineSliderPreference_decrementContentDescription);
        }
        if (a.hasValue(R.styleable.WearInlineSliderPreference_incrementContentDescription)) {
            mIncrementDescription = a.getString(R.styleable.WearInlineSliderPreference_incrementContentDescription);
        }
        if (a.hasValue(R.styleable.WearInlineSliderPreference_progressContentDescription)) {
            mProgressDescription = a.getString(R.styleable.WearInlineSliderPreference_progressContentDescription);
        }
        a.recycle();
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        setValue(value, true);
    }

    void setValue(float value, boolean notifyChanged) {
        if (mValue == value) {
            return;
        }
        mValue = value;
        if (shouldPersist() && value != getPersistedFloat(Float.NaN)) {
            persistFloat(value);
        }
        if (notifyChanged) {
            notifyChanged();
        }
    }

    void notifyUserChanged() {
        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null) {
            listener.onPreferenceClick(this);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        WearInlineSlider slider = (WearInlineSlider) holder.itemView;
        slider.setPreference(this);
        slider.setMinValue(mMin);
        slider.setMaxValue(mMax);
        slider.setValue(mValue);
        slider.setStep(mIncrement);
        slider.setShowIncrementSeparators(mShowSeparators);
        slider.setShowMinimumIncrement(mShowMinimumIncrement);
        if (mIncrementDescription != null) {
            slider.setIncrementDescription(mIncrementDescription);
        }
        if (mDecrementDescription != null) {
            slider.setDecrementDescription(mDecrementDescription);
        }
        if (mProgressDescription != null) {
            slider.setProgressDescription(mProgressDescription);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, 0.0f);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = mMin;
        }
        setValue(getPersistedFloat((Float) defaultValue), true);
    }
}