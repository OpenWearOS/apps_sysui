package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import io.github.openwearos.sysui.R;

public class AmbientableTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int[] STATE_AMBIENT = {R.attr.ambient};
    private static final int[] STATE_LOW_BIT = {R.attr.lowBitAmbient};

    private boolean mAmbient;
    private boolean mLowBit;
    private Boolean mWasAntiAliased;
    private CharSequence mStyledText;
    private ColorStateList mNormalTextColors;
    private ColorStateList mNormalDrawableTint;

    public AmbientableTextView(Context context) {
        super(context);
    }

    public AmbientableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AmbientableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isAmbient() {
        return mAmbient;
    }

    public void enterAmbient(boolean lowBitAmbient) {
        if (mAmbient) return;

        if (lowBitAmbient) {
            mWasAntiAliased = (getPaintFlags() & Paint.ANTI_ALIAS_FLAG) != 0;
            setPaintFlags(getPaintFlags() & ~Paint.ANTI_ALIAS_FLAG);
        }

        CharSequence text = getText();
        if (text instanceof Spanned && hasColorSpans((Spanned) text)) {
            mStyledText = TextUtils.stringOrSpannedString(text);
            super.setText(stripColors((Spanned) text));
        }

        mNormalTextColors = getTextColors();
        super.setTextColor(Color.WHITE);
        mNormalDrawableTint = getCompoundDrawableTintList();
        super.setCompoundDrawableTintList(null);

        mAmbient = true;
        mLowBit = lowBitAmbient;
        refreshDrawableState();
    }

    public void exitAmbient() {
        if (!mAmbient) return;
        mAmbient = false;

        if (mWasAntiAliased != null) {
            if (mWasAntiAliased) setPaintFlags(getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
            mWasAntiAliased = null;
        }
        if (mStyledText != null) {
            super.setText(mStyledText);
            mStyledText = null;
        }
        if (mNormalTextColors != null) {
            super.setTextColor(mNormalTextColors);
            mNormalTextColors = null;
        }
        if (mNormalDrawableTint != null) {
            super.setCompoundDrawableTintList(mNormalDrawableTint);
            mNormalDrawableTint = null;
        }
        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (isDuplicateParentStateEnabled()) {
            return super.onCreateDrawableState(extraSpace);
        }
        int[] state = super.onCreateDrawableState(
                extraSpace + (mAmbient ? 1 : 0) + (mLowBit ? 1 : 0));
        if (mAmbient) mergeDrawableStates(state, STATE_AMBIENT);
        if (mLowBit) mergeDrawableStates(state, STATE_LOW_BIT);
        return state;
    }

    @Override
    public void setCompoundDrawableTintList(ColorStateList tint) {
        if (mAmbient) mNormalDrawableTint = tint;
        else super.setCompoundDrawableTintList(tint);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!mAmbient) {
            super.setText(text, type);
        } else if (text instanceof Spanned && hasColorSpans((Spanned) text)) {
            mStyledText = text;
            super.setText(stripColors((Spanned) text), type);
        } else {
            mStyledText = null;
            super.setText(text, type);
        }
    }

    @Override
    public void setTextColor(int color) {
        setTextColor(ColorStateList.valueOf(color));
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        if (mAmbient) mNormalTextColors = colors;
        else super.setTextColor(colors);
    }

    private static boolean hasColorSpans(Spanned text) {
        int end = text.length();
        return text.getSpans(0, end, ForegroundColorSpan.class).length
                + text.getSpans(0, end, BackgroundColorSpan.class).length
                + text.getSpans(0, end, TextAppearanceSpan.class).length > 0;
    }

    private static SpannableString stripColors(Spanned source) {
        SpannableString result = new SpannableString(source);
        removeSpans(result, ForegroundColorSpan.class);
        removeSpans(result, BackgroundColorSpan.class);
        for (TextAppearanceSpan span : result.getSpans(0, result.length(), TextAppearanceSpan.class)) {
            TextAppearanceSpan plain = new TextAppearanceSpan(
                    span.getFamily(), span.getTextStyle(), span.getTextSize(), null, null);
            int start = result.getSpanStart(span);
            int end = result.getSpanEnd(span);
            int flags = result.getSpanFlags(span);
            result.removeSpan(span);
            result.setSpan(plain, start, end, flags);
        }
        return result;
    }

    private static void removeSpans(Spannable text, Class<? extends CharacterStyle> type) {
        for (CharacterStyle span : text.getSpans(0, text.length(), type)) {
            text.removeSpan(span);
        }
    }
}