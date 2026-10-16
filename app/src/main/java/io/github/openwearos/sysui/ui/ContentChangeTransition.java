package io.github.openwearos.sysui.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spanned;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import io.github.openwearos.sysui.R;

import java.util.Map;

public final class ContentChangeTransition extends TransitionSet {
	private static final String NAME = ContentChangeTransition.class.getName();
	private static final String KEY_CONTENT_VERSION = NAME + ":content-version";
	private static final String KEY_VISIBILITY = NAME + ":visibility";

	public static final String[] TRANSITION_PROPERTIES = { KEY_VISIBILITY, KEY_CONTENT_VERSION };

	private static final Interpolator FADE_IN_INTERPOLATOR = new PathInterpolator(0f, 0f, 0.2f, 1f);
	private static final Interpolator FADE_OUT_INTERPOLATOR = new PathInterpolator(0.4f, 0f, 1f, 1f);

	private static final long FADE_OUT_DURATION_MS = 150;
	private static final long FADE_IN_DURATION_MS = 300;
	private static final int SNAPSHOT_END_ALPHA = 25;

	public ContentChangeTransition(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrdering(ORDERING_SEQUENTIAL);

		SnapshotFadeOut fadeOut = new SnapshotFadeOut();
		fadeOut.setDuration(FADE_OUT_DURATION_MS);
		fadeOut.setInterpolator(FADE_OUT_INTERPOLATOR);
		addTransition(fadeOut);

		ContentFadeIn fadeIn = new ContentFadeIn();
		fadeIn.setDuration(FADE_IN_DURATION_MS);
		fadeIn.setInterpolator(FADE_IN_INTERPOLATOR);
		addTransition(fadeIn);
	}

	public static void setText(TextView textView, CharSequence text) {
		CharSequence current = textView.getText();
		if (current == text) {
			return;
		}
		if (current == null || text == null || current instanceof Spanned || text instanceof Spanned
				|| !current.toString().contentEquals(text)) {
			textView.setText(text);
			textView.setTag(R.id.tag_content_version, View.generateViewId());
		}
	}

	private static int contentVersion(Map<String, Object> values) {
		Integer version = (Integer) values.get(KEY_CONTENT_VERSION);
		return version == null ? View.generateViewId() : version;
	}

	private static boolean isVisible(Map<String, Object> values) {
		Boolean visible = (Boolean) values.get(KEY_VISIBILITY);
		return visible != null && visible;
	}

	private static void captureValues(TransitionValues transitionValues) {
		View view = transitionValues.view;
		Integer version = (Integer) view.getTag(R.id.tag_content_version);
		transitionValues.values.put(KEY_CONTENT_VERSION, version == null ? View.generateViewId() : version);
		transitionValues.values.put(KEY_VISIBILITY, view.getVisibility() == View.VISIBLE);
	}

	private static boolean isContentChanged(TransitionValues start, TransitionValues end) {
		if (start.view != end.view) {
			return false;
		}
		Map<String, Object> startValues = start.values;
		Map<String, Object> endValues = end.values;
		return isVisible(startValues) && isVisible(endValues)
				&& contentVersion(startValues) != contentVersion(endValues);
	}

	private static final class SnapshotFadeOut extends Transition {
		private static final String KEY_SNAPSHOT = SnapshotFadeOut.class.getName() + ":snapshot";
		private static final android.util.Property<WearSnapshot, Integer> ALPHA = new IntProperty<WearSnapshot>(
				"alpha") {
			@Override
			public Integer get(WearSnapshot snapshot) {
				return snapshot.getAlpha();
			}

			@Override
			public void setValue(WearSnapshot snapshot, int value) {
				snapshot.setAlpha(value);
			}
		};

		@Override
		public void captureEndValues(TransitionValues transitionValues) {
			captureValues(transitionValues);
		}

		@Override
		public void captureStartValues(TransitionValues transitionValues) {
			captureValues(transitionValues);
			View view = transitionValues.view;
			Rect rect = new Rect();
			view.getHitRect(rect);
			if (rect.isEmpty() || view.getVisibility() != View.VISIBLE) {
				return;
			}
			Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
			view.draw(new Canvas(bitmap));
			BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
			drawable.setBounds(rect);
			transitionValues.values.put(KEY_SNAPSHOT, new WearSnapshot(drawable, view));
		}

		@Override
		public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
				TransitionValues endValues) {
			WearSnapshot snapshot = (WearSnapshot) startValues.values.remove(KEY_SNAPSHOT);
			if (snapshot == null) {
				return null;
			}
			ObjectAnimator animator = ObjectAnimator.ofInt(snapshot, ALPHA, SNAPSHOT_END_ALPHA);
			animator.addListener(new SnapshotOverlayListener(snapshot));
			animator.addListener(new HardwareLayerListener(startValues.view));
			addListener(new TransientStateListener(startValues.view));
			return animator;
		}

		@Override
		public String[] getTransitionProperties() {
			return TRANSITION_PROPERTIES;
		}

		@Override
		public boolean isTransitionRequired(TransitionValues start, TransitionValues end) {
			return start != null && end != null && isContentChanged(start, end);
		}
	}

	private static final class ContentFadeIn extends Fade {
		ContentFadeIn() {
			super(Fade.IN);
		}

		@Override
		public void captureEndValues(TransitionValues transitionValues) {
			super.captureEndValues(transitionValues);
			captureValues(transitionValues);
		}

		@Override
		public void captureStartValues(TransitionValues transitionValues) {
			super.captureStartValues(transitionValues);
			captureValues(transitionValues);
		}

		@Override
		public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
				TransitionValues endValues) {
			View view = endValues.view;
			Animator animator = onAppear(sceneRoot, view, startValues, endValues);
			if (animator == null) {
				return null;
			}
			if (animator instanceof ObjectAnimator) {
				((ObjectAnimator) animator).setFloatValues(0.1f, 1.0f);
			}
			addListener(new TransientStateListener(view));
			return animator;
		}

		@Override
		public String[] getTransitionProperties() {
			return TRANSITION_PROPERTIES;
		}

		@Override
		public boolean isTransitionRequired(TransitionValues start, TransitionValues end) {
			return start != null && end != null && isContentChanged(start, end);
		}
	}

	private static final class SnapshotOverlayListener extends AnimatorListenerAdapter {
		private final WearSnapshot mSnapshot;

		SnapshotOverlayListener(WearSnapshot snapshot) {
			mSnapshot = snapshot;
		}

		@Override
		public void onAnimationStart(Animator animation) {
			mSnapshot.getDrawable().getBitmap().prepareToDraw();
			mSnapshot.getOverlay().add(mSnapshot.getDrawable());
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			mSnapshot.remove();
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			mSnapshot.remove();
		}
	}

	private static final class HardwareLayerListener extends AnimatorListenerAdapter {
		private final View mView;
		private boolean mLayerApplied = false;

		HardwareLayerListener(View view) {
			mView = view;
		}

		@Override
		public void onAnimationStart(Animator animation) {
			if (mView.hasOverlappingRendering() && mView.getLayerType() == View.LAYER_TYPE_NONE) {
				mLayerApplied = true;
				mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			}
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (mLayerApplied) {
				mLayerApplied = false;
				mView.setLayerType(View.LAYER_TYPE_NONE, null);
			}
		}
	}

	private static final class TransientStateListener implements Transition.TransitionListener {
		private final View mView;

		TransientStateListener(View view) {
			mView = view;
		}

		@Override
		public void onTransitionStart(Transition transition) {
			mView.setHasTransientState(true);
		}

		@Override
		public void onTransitionEnd(Transition transition) {
			transition.removeListener(this);
			mView.setHasTransientState(false);
		}

		@Override
		public void onTransitionCancel(Transition transition) {
		}

		@Override
		public void onTransitionPause(Transition transition) {
		}

		@Override
		public void onTransitionResume(Transition transition) {
		}
	}
}
