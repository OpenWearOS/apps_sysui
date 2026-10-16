package io.github.openwearos.sysui.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class WearPlaceholderPulseDrawable extends Drawable implements WearPlaceholder {

	private static final int[] GRADIENT_COLORS = { 0x00FFFFFF, 0x22FFFFFF, 0x00FFFFFF };
	private static final float[] GRADIENT_POSITIONS = { 0.0f, 0.65f, 1.0f };
	private static final float SQRT_2 = 1.4142f;
	private static final float OVERSCAN = 1.4849099f;
	private static final int VERTICAL_OVERSCAN_PX = 1000;

	private boolean mHasBeenMutated;
	private State mState;

	public WearPlaceholderPulseDrawable() {
		this(new State());
	}

	private WearPlaceholderPulseDrawable(State state) {
		mState = state;
	}

	@Override
	public void draw(Canvas canvas) {
		State state = mState;
		Rect bounds = getBounds();
		if (state.rect.isEmpty()) {
			return;
		}
		float half = state.pulseSize(bounds) / SQRT_2;
		float start = -half;
		float travel = (half - start) * state.progress;
		int save = canvas.save();
		canvas.translate(0.0f, state.staggerOffset);
		canvas.rotate(45.0f, bounds.centerX(), bounds.centerY());
		canvas.translate(start + travel, 0.0f);
		canvas.drawRect(state.rect, state.paint);
		canvas.restoreToCount(save);
	}

	@Override
	public void initialize() {
		setProgress(0.0f);
		setAlpha(0);
	}

	@Override
	public int getAlpha() {
		return mState.paint.getAlpha();
	}

	@Override
	public void setAlpha(int alpha) {
		if (mState.paint.getAlpha() == alpha) {
			return;
		}
		mState.paint.setAlpha(alpha);
		invalidateSelf();
	}

	@Override
	public ColorFilter getColorFilter() {
		return mState.paint.getColorFilter();
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		if (mState.paint.getColorFilter() == colorFilter) {
			return;
		}
		mState.paint.setColorFilter(colorFilter);
		invalidateSelf();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public Drawable.ConstantState getConstantState() {
		return mState;
	}

	@Override
	public Drawable mutate() {
		if (!mHasBeenMutated && super.mutate() == this) {
			mState = new State(mState);
			mHasBeenMutated = true;
		}
		return this;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		State state = mState;
		if (bounds.isEmpty()) {
			state.rect.setEmpty();
			return;
		}
		int size = state.pulseSize(bounds);
		int extra = (((int) Math.ceil(size * OVERSCAN)) - size) / 2;
		state.rect.set(bounds.left - extra, bounds.top - VERTICAL_OVERSCAN_PX,
				bounds.right + extra, bounds.bottom + VERTICAL_OVERSCAN_PX);
		state.paint.setShader(new LinearGradient(state.rect.left, 0.0f, state.rect.right, 0.0f,
				GRADIENT_COLORS, GRADIENT_POSITIONS, Shader.TileMode.CLAMP));
	}

	public float getProgress() {
		return mState.progress;
	}

	public void setProgress(float progress) {
		if (mState.progress == progress) {
			return;
		}
		mState.progress = progress;
		invalidateSelf();
	}

	public void setRequestedPulseSize(int size) {
		mState.requestedSize = size;
	}

	public void setStaggerOffset(int offset) {
		if (mState.staggerOffset == offset) {
			return;
		}
		mState.staggerOffset = offset;
		invalidateSelf();
	}

	private static final class State extends Drawable.ConstantState {
		final Paint paint;
		final Rect rect;
		float progress;
		int staggerOffset;
		int requestedSize;

		State() {
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			rect = new Rect();
		}

		State(State other) {
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			rect = new Rect();
			paint.set(other.paint);
			rect.set(other.rect);
			progress = other.progress;
			staggerOffset = other.staggerOffset;
		}

		int pulseSize(Rect bounds) {
			return requestedSize > 0 ? requestedSize : bounds.width();
		}

		@Override
		public int getChangingConfigurations() {
			return 0;
		}

		@Override
		public Drawable newDrawable() {
			return new WearPlaceholderPulseDrawable(this);
		}
	}
}