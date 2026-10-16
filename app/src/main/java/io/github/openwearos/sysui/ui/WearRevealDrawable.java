package io.github.openwearos.sysui.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public final class WearRevealDrawable extends Drawable {
	private static final int[] GRADIENT_COLORS = { 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF };
	private static final float[] GRADIENT_POSITIONS = { 0.0f, 0.7f, 1.0f };
	private static final float OVERSCAN = 1.7677499f;
	private static final int VERTICAL_OVERSCAN_PX = 1000;

	private boolean mHasBeenMutated;
	private State mState;

	public WearRevealDrawable() {
		this(new State());
	}

	private WearRevealDrawable(State state) {
		mState = state;
	}

	@Override
	public void draw(Canvas canvas) {
		State state = mState;
		Rect bounds = getBounds();
		if (state.rect.isEmpty()) {
			return;
		}
		float width = bounds.width();
		float start = -width * 1.5f;
		float travel = ((width * 0.225f) - start) * state.progress;
		int save = canvas.save();
		canvas.translate(0.0f, state.staggerOffset);
		canvas.rotate(45.0f, state.rect.centerX(), state.rect.centerY());
		canvas.translate(start + travel, 0.0f);
		canvas.drawRect(state.rect, state.paint);
		canvas.restoreToCount(save);
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
		super.onBoundsChange(bounds);
		State state = mState;
		int width = bounds.width();
		int height = bounds.height();
		if (width == 0 || height == 0) {
			state.rect.setEmpty();
		} else {
			int extra = (((int) Math.ceil(Math.max(width, height) * OVERSCAN)) - width) / 2;
			state.rect.set(bounds.left - extra, bounds.top - VERTICAL_OVERSCAN_PX,
					bounds.right + extra, bounds.bottom + VERTICAL_OVERSCAN_PX);
		}
		if (state.rect.isEmpty()) {
			return;
		}
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

	public void setStaggerOffset(int offset) {
		if (mState.staggerOffset == offset) {
			return;
		}
		mState.staggerOffset = offset;
		invalidateSelf();
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
	}

	private static final class State extends Drawable.ConstantState {
		final Paint paint;
		final Rect rect;
		float progress;
		int staggerOffset;

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

		@Override
		public int getChangingConfigurations() {
			return 0;
		}

		@Override
		public Drawable newDrawable() {
			return new WearRevealDrawable(this);
		}
	}
}
