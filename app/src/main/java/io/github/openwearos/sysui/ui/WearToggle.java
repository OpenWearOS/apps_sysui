package io.github.openwearos.sysui.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import io.github.openwearos.sysui.R;

public final class WearToggle extends androidx.appcompat.widget.AppCompatToggleButton {
	public WearToggle(Context context) {
		this(context, null);
	}

	public WearToggle(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.wearToggleStyle);
	}

	public WearToggle(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public Drawable getButtonDrawable() {
		Drawable drawable = super.getButtonDrawable();
		return drawable instanceof RtlMirrorDrawable ? ((RtlMirrorDrawable) drawable).getDrawable() : drawable;
	}

	@Override
	public void setButtonDrawable(Drawable drawable) {
		super.setButtonDrawable(new RtlMirrorDrawable(drawable));
	}

	@Override
	public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
		if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
			CharSequence stateDescription = getStateDescription();
			if (stateDescription != null) {
				announceForAccessibility(stateDescription.toString());
			}
		}
		super.onPopulateAccessibilityEvent(event);
	}

	private static final class RtlMirrorDrawable extends DrawableWrapper {
		RtlMirrorDrawable(Drawable drawable) {
			super(drawable);
		}

		@Override
		public void draw(Canvas canvas) {
			if (getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
				super.draw(canvas);
				return;
			}
			int save = canvas.save();
			canvas.scale(-1.0f, 1.0f, getBounds().centerX(), 0.0f);
			super.draw(canvas);
			canvas.restoreToCount(save);
		}
	}
}
