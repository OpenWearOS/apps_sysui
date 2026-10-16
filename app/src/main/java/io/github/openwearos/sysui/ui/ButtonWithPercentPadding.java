package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class ButtonWithPercentPadding extends Button {
    private static final float ROUND_FRACTION = 0.15f;
    private static final float SQUARE_FRACTION = 0.05f;

    public ButtonWithPercentPadding(Context context) {
        super(context);
    }

    public ButtonWithPercentPadding(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonWithPercentPadding(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ButtonWithPercentPadding(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean round = getResources().getConfiguration().isScreenRound();
        int side = (int) (View.MeasureSpec.getSize(widthMeasureSpec)
                * (round ? ROUND_FRACTION : SQUARE_FRACTION));
        setPadding(side, getPaddingTop(), side, getPaddingBottom());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}