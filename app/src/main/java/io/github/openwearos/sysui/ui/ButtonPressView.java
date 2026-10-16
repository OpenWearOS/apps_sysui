package io.github.openwearos.sysui.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Method;

import io.github.openwearos.sysui.R;

public class ButtonPressView extends FrameLayout {
    private static final int[] BUTTON_KEYCODES = {
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_STEM_PRIMARY,
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3,
    };
    private static final long PULSE_MS = 700;
    private static final float PULSE_TRAVEL_DP = 3f;
    private static final float PULSE_MIN_ALPHA = 0.35f;

    private final FrameLayout mStaticButtons;
    private final ImageView mPressAnimation;
    private final ImageView mScreenContent;
    private ValueAnimator mPulse;

    public ButtonPressView(Context context) {
        this(context, null);
    }

    public ButtonPressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonPressView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ButtonPressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.button_press_view, (ViewGroup) this, true);
        mStaticButtons = findViewById(R.id.button_tip_static_button_layers);
        mPressAnimation = findViewById(R.id.button_tip_press_animation);
        mScreenContent = findViewById(R.id.button_tip_screen_content);
    }

    public ImageView getScreenContent() {
        return mScreenContent;
    }

    public ImageView getPressAnimation() {
        return mPressAnimation;
    }

    public void showButton(int keyCode) {
        float[] target = HardwareButtons.position(getContext(), keyCode);
        if (target == null) return;

        stopPulse();
        mStaticButtons.removeAllViews();

        for (int other : BUTTON_KEYCODES) {
            if (other == keyCode) continue;
            if (HardwareButtons.position(getContext(), other) == null) continue;
            ImageView marker = new ImageView(getContext());
            marker.setImageDrawable(getResources().getDrawable(drawableFor(other), getContext().getTheme()));
            marker.setRotation(angleDegrees(other));
            mStaticButtons.addView(marker);
        }

        float angle = angleDegrees(keyCode);
        mPressAnimation.setImageDrawable(
                getResources().getDrawable(drawableFor(keyCode), getContext().getTheme()));
        mPressAnimation.setRotation(angle);
        startPulse(angle);
    }

    private static int drawableFor(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_POWER
                ? R.drawable.button_tip_static_button
                : R.drawable.button_tip_static_button_secondary;
    }

    private double angleDegrees(int keyCode) {
        float[] pos = HardwareButtons.position(getContext(), keyCode);
        if (pos == null) return 0.0;
        float cx = getResources().getDisplayMetrics().widthPixels / 2f;
        float cy = getResources().getDisplayMetrics().heightPixels / 2f;
        return Math.toDegrees(Math.atan2(pos[1] - cy, pos[0] - cx));
    }

    private void startPulse(float angle) {
        float travel = PULSE_TRAVEL_DP * getResources().getDisplayMetrics().density;
        double rad = Math.toRadians(angle);
        float dx = (float) -Math.cos(rad) * travel;
        float dy = (float) -Math.sin(rad) * travel;

        mPulse = ValueAnimator.ofFloat(0f, 1f);
        mPulse.setDuration(PULSE_MS);
        mPulse.setRepeatMode(ValueAnimator.REVERSE);
        mPulse.setRepeatCount(ValueAnimator.INFINITE);
        mPulse.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            mPressAnimation.setTranslationX(dx * t);
            mPressAnimation.setTranslationY(dy * t);
            mPressAnimation.setAlpha(1f - (1f - PULSE_MIN_ALPHA) * t);
        });
        mPulse.start();
    }

    private void stopPulse() {
        if (mPulse != null) {
            mPulse.cancel();
            mPulse = null;
        }
        mPressAnimation.setTranslationX(0f);
        mPressAnimation.setTranslationY(0f);
        mPressAnimation.setAlpha(1f);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    private static final class HardwareButtons {
        private static Method sGet;
        private static boolean sResolved;

        private static String prop(String key) {
            if (!sResolved) {
                sResolved = true;
                try {
                    sGet = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    sGet = null;
                }
            }
            if (sGet == null) return "";
            try {
                Object v = sGet.invoke(null, key);
                return v == null ? "" : v.toString();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return "";
            }
        }

        private static String name(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_POWER:
                    return "power";
                case KeyEvent.KEYCODE_STEM_PRIMARY:
                    return "stem_primary";
                case KeyEvent.KEYCODE_STEM_1:
                    return "stem_1";
                case KeyEvent.KEYCODE_STEM_2:
                    return "stem_2";
                case KeyEvent.KEYCODE_STEM_3:
                    return "stem_3";
                default:
                    return null;
            }
        }

        private static float[] pair(String xKey, String yKey) {
            try {
                String x = prop(xKey);
                String y = prop(yKey);
                if (x.isEmpty() || y.isEmpty()) return null;
                return new float[] {Float.parseFloat(x), Float.parseFloat(y)};
            } catch (NumberFormatException e) {
                return null;
            }
        }

        static float[] position(Context context, int keyCode) {
            String n = name(keyCode);
            if (n == null) return null;
            float[] pos = pair("ro." + n + ".location.x", "ro." + n + ".location.y");
            if (pos == null) return null;

            boolean flipped = Settings.System.getInt(
                    context.getContentResolver(), Settings.System.USER_ROTATION, 0) == 2;
            if (flipped) {
                float[] rotated = pair("ro." + n + ".rotated.x", "ro." + n + ".rotated.y");
                if (rotated != null) {
                    return rotated;
                }
                pos = new float[] {
                        context.getResources().getDisplayMetrics().widthPixels - pos[0],
                        context.getResources().getDisplayMetrics().heightPixels - pos[1]};
            }
            return pos;
        }
    }
}