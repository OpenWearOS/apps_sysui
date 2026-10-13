package io.github.openwearos.sysui.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.github.openwearos.sysui.R;

import java.util.ArrayList;

public final class RemoteInputActivity extends Activity {
	public static final String ACTION = "android.support.wearable.input.action.REMOTE_INPUT";
	public static final String EXTRA_REMOTE_INPUTS = "android.support.wearable.input.extra.REMOTE_INPUTS";
	public static final String EXTRA_ACTION_TYPE = "android.support.wearable.input.extra.INPUT_ACTION_TYPE";
	public static final String EXTRA_PENDING_INTENT = "pending_intent";

	private static final String TAG = "OpenWearInput";
	private static final String EXTRA_LEGACY_INPUTS = "remote_inputs_bundle";
	private static final int REQ_VOICE = 1;

	private RemoteInput[] mInputs;
	private CharSequence[] mAnswers;
	private PendingIntent mTarget;
	private int mImeAction;
	private int mIndex;
	private boolean mAllFromChoices = true;

	private TextView mPrompt;
	private LinearLayout mOptions;
	private EditText mText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		mInputs = parseInputs(extras);
		if (mInputs == null || mInputs.length == 0) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		mTarget = extras.getParcelable(EXTRA_PENDING_INTENT, PendingIntent.class);
		mImeAction = imeAction(extras.getInt(EXTRA_ACTION_TYPE, 0));
		mAnswers = new CharSequence[mInputs.length];
		buildViews();
		showOptions();
	}

	@Override
	public void onBackPressed() {
		if (mText != null && mText.getVisibility() == View.VISIBLE) {
			showOptions();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQ_VOICE || resultCode != RESULT_OK || data == null)
			return;
		ArrayList<String> heard = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		if (heard != null && !heard.isEmpty())
			answer(heard.get(0), false);
	}

	@SuppressWarnings("deprecation")
	private static RemoteInput[] parseInputs(Bundle extras) {
		if (extras == null)
			return null;
		Parcelable[] raw = extras.getParcelableArray(EXTRA_REMOTE_INPUTS);
		if (raw != null) {
			ArrayList<RemoteInput> out = new ArrayList<>();
			for (Parcelable p : raw) {
				if (p instanceof RemoteInput)
					out.add((RemoteInput) p);
			}
			return out.toArray(new RemoteInput[0]);
		}
		Parcelable[] legacy = extras.getParcelableArray(EXTRA_LEGACY_INPUTS);
		if (legacy == null)
			return null;
		ArrayList<RemoteInput> out = new ArrayList<>();
		for (Parcelable p : legacy) {
			if (!(p instanceof Bundle))
				continue;
			Bundle b = (Bundle) p;
			String key = b.getString("resultKey");
			if (key == null)
				continue;
			RemoteInput.Builder builder = new RemoteInput.Builder(key)
					.setLabel(b.getCharSequence("label"))
					.setChoices(b.getCharSequenceArray("choices"))
					.setAllowFreeFormInput(b.getBoolean("allowFreeFormInput"));
			Bundle more = b.getBundle("extras");
			if (more != null)
				builder.addExtras(more);
			out.add(builder.build());
		}
		return out.toArray(new RemoteInput[0]);
	}

	private static int imeAction(int type) {
		switch (type) {
			case 1:
				return EditorInfo.IME_ACTION_SEARCH;
			case 2:
				return EditorInfo.IME_ACTION_DONE;
			case 3:
				return EditorInfo.IME_ACTION_GO;
			default:
				return EditorInfo.IME_ACTION_SEND;
		}
	}

	private void buildViews() {
		FrameLayout root = new FrameLayout(this);
		root.setBackgroundColor(Ui.BG);

		LinearLayout column = new LinearLayout(this);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setGravity(Gravity.CENTER_HORIZONTAL);
		int side = Ui.sideInset(this);
		column.setPadding(side, Ui.dp(this, 40), side, Ui.dp(this, 40));

		mPrompt = Ui.text(this, "", 15, Ui.TEXT2);
		mPrompt.setPadding(0, 0, 0, Ui.dp(this, 8));
		column.addView(mPrompt, new LinearLayout.LayoutParams(Ui.MP, Ui.WC));

		mOptions = new LinearLayout(this);
		mOptions.setOrientation(LinearLayout.VERTICAL);
		column.addView(mOptions, new LinearLayout.LayoutParams(Ui.MP, Ui.WC));

		mText = new EditText(this);
		mText.setTextColor(Ui.TEXT);
		mText.setTextSize(16);
		mText.setHorizontallyScrolling(false);
		mText.setMaxLines(4);
		mText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		mText.setImeOptions(mImeAction);
		mText.setVisibility(View.GONE);
		mText.setOnEditorActionListener((v, actionId, event) -> {
			String typed = v.getText().toString().trim();
			if (!typed.isEmpty())
				answer(typed, false);
			return true;
		});
		column.addView(mText, new LinearLayout.LayoutParams(Ui.MP, Ui.WC));

		ScrollView scroll = new ScrollView(this);
		scroll.addView(column, new FrameLayout.LayoutParams(Ui.MP, Ui.WC));
		root.addView(scroll, new FrameLayout.LayoutParams(Ui.MP, Ui.MP));
		setContentView(root);
	}

	private void showOptions() {
		hideKeyboard();
		mText.setVisibility(View.GONE);
		mOptions.setVisibility(View.VISIBLE);
		mOptions.removeAllViews();

		RemoteInput input = mInputs[mIndex];
		CharSequence label = input.getLabel();
		mPrompt.setText(TextUtils.isEmpty(label) ? getString(R.string.remote_input_reply) : label);

		CharSequence[] choices = input.getChoices();
		if (choices != null) {
			for (CharSequence choice : choices)
				addOption(choice, () -> answer(choice, true));
		}
		if (input.getAllowFreeFormInput()) {
			addOption(getString(R.string.remote_input_keyboard), this::showKeyboard);
			if (voiceAvailable())
				addOption(getString(R.string.remote_input_voice), this::startVoice);
		}
		if (mOptions.getChildCount() == 0) {
			Log.w(TAG, "input '" + input.getResultKey() + "' offers no way to answer");
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	private void addOption(CharSequence label, Runnable onTap) {
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MP, Ui.WC);
		lp.topMargin = Ui.dp(this, 6);
		mOptions.addView(new WearChipButton(this, label, onTap), lp);
	}

	private void showKeyboard() {
		mOptions.setVisibility(View.GONE);
		mText.setText("");
		mText.setVisibility(View.VISIBLE);
		mText.requestFocus();
		mText.post(() -> {
			InputMethodManager imm = getSystemService(InputMethodManager.class);
			if (imm != null)
				imm.showSoftInput(mText, InputMethodManager.SHOW_IMPLICIT);
		});
	}

	private void hideKeyboard() {
		InputMethodManager imm = getSystemService(InputMethodManager.class);
		if (imm != null && mText != null)
			imm.hideSoftInputFromWindow(mText.getWindowToken(), 0);
	}

	private Intent voiceIntent() {
		return new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
				.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
	}

	private boolean voiceAvailable() {
		return !getPackageManager().queryIntentActivities(voiceIntent(), 0).isEmpty();
	}

	private void startVoice() {
		try {
			startActivityForResult(voiceIntent(), REQ_VOICE);
		} catch (RuntimeException e) {
			Log.w(TAG, "voice input failed", e);
		}
	}

	private void answer(CharSequence text, boolean fromChoice) {
		mAnswers[mIndex] = text;
		mAllFromChoices &= fromChoice;
		mIndex++;
		if (mIndex < mInputs.length)
			showOptions();
		else
			deliver();
	}

	private void deliver() {
		Bundle results = new Bundle();
		for (int i = 0; i < mInputs.length; i++) {
			results.putCharSequence(mInputs[i].getResultKey(), mAnswers[i]);
		}
		Intent data = new Intent();
		RemoteInput.addResultsToIntent(mInputs, data, results);
		RemoteInput.setResultsSource(data,
				mAllFromChoices ? RemoteInput.SOURCE_CHOICE : RemoteInput.SOURCE_FREE_FORM_INPUT);
		try {
			if (mTarget != null) {
				mTarget.send(this, 0, data);
				setResult(RESULT_OK);
			} else {
				setResult(RESULT_OK, data);
			}
		} catch (PendingIntent.CanceledException e) {
			Log.w(TAG, "target intent was cancelled", e);
			setResult(RESULT_CANCELED);
		}
		finish();
	}
}
