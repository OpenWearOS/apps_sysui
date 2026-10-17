package io.github.openwearos.sysui.ui;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

public final class OutlineProviders {
    public static final ViewOutlineProvider CIRCLE = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int side = Math.min(view.getWidth(), view.getHeight());
            outline.setRoundRect(0, 0, side, side, side / 2f);
        }
    };

    public static final ViewOutlineProvider RECT = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    private OutlineProviders() {
    }
}
