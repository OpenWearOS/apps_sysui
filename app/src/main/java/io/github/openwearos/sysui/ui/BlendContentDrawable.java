package io.github.openwearos.sysui.ui;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;

import io.github.openwearos.sysui.R;

import java.io.IOException;
import java.util.function.Consumer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class BlendContentDrawable extends DrawableWrapper {
	public enum Mode {
		NONE(PorterDuff.Mode.SRC_ATOP),
		COLOR(PorterDuff.Mode.SRC_ATOP),
		ALPHA(PorterDuff.Mode.DST_OUT);

		final PorterDuff.Mode porterDuff;

		Mode(PorterDuff.Mode porterDuff) {
			this.porterDuff = porterDuff;
		}
	}

	private final Paint mContentPaint = new Paint();
	private final Paint mDrawablePaint = new Paint();
	private final RectF mTmpRect = new RectF();
	private Mode mBlendMode = Mode.NONE;
	private Consumer<Canvas> mContentProvider;

	public BlendContentDrawable() {
		super(null);
		mContentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
		mDrawablePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
	}

	public static BlendContentDrawable create(Drawable drawable) {
		return create(drawable, Mode.NONE);
	}

	public static BlendContentDrawable create(Drawable drawable, Mode mode) {
		BlendContentDrawable result = new BlendContentDrawable();
		result.setDrawable(drawable);
		result.setBlendMode(mode);
		return result;
	}

	public void setBlendMode(Mode mode) {
		mBlendMode = mode;
		mDrawablePaint.setXfermode(new PorterDuffXfermode(mode.porterDuff));
	}

	public void setContentProvider(Consumer<Canvas> provider) {
		if (mContentProvider == null) {
			mContentProvider = provider;
		}
	}

	private boolean useAlphaChannelBlending() {
		return mBlendMode != Mode.NONE;
	}

	@Override
	public void draw(Canvas canvas) {
		if (mContentProvider == null) {
			super.draw(canvas);
			return;
		}
		if (useAlphaChannelBlending()) {
			mTmpRect.set(getBounds());
			int contentLayer = canvas.saveLayer(mTmpRect, mContentPaint);
			mContentProvider.accept(canvas);
			int drawableLayer = canvas.saveLayer(mTmpRect, mDrawablePaint);
			super.draw(canvas);
			canvas.restoreToCount(drawableLayer);
			canvas.restoreToCount(contentLayer);
		} else {
			mContentProvider.accept(canvas);
			super.draw(canvas);
		}
	}

	@Override
	public int getOpacity() {
		return useAlphaChannelBlending() ? PixelFormat.TRANSLUCENT : super.getOpacity();
	}

	@Override
	public void inflate(Resources resources, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme)
			throws XmlPullParserException, IOException {
		super.inflate(resources, parser, attrs, theme);
		TypedArray a = theme != null
				? theme.obtainStyledAttributes(attrs, R.styleable.BlendContentDrawable, 0, 0)
				: resources.obtainAttributes(attrs, R.styleable.BlendContentDrawable);
		setDrawable(a.getDrawable(R.styleable.BlendContentDrawable_android_drawable));
		switch (a.getInt(R.styleable.BlendContentDrawable_blendMode, 0)) {
			case 1:
				setBlendMode(Mode.COLOR);
				break;
			case 2:
				setBlendMode(Mode.ALPHA);
				break;
			default:
				setBlendMode(Mode.NONE);
				break;
		}
		a.recycle();
	}
}
