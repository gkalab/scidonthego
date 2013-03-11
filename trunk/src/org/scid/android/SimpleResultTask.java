package org.scid.android;

import android.app.Activity;

public abstract class SimpleResultTask extends ProgressingTask<Boolean> {
	public SimpleResultTask(Activity activity) {
		super(activity, R.string.export_pgn_title, R.string.please_wait);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		dismissProgress();
		if (result) {
			activity.setResult(Activity.RESULT_OK);
		} else {
			activity.setResult(Activity.RESULT_CANCELED);
		}
		activity.finish();
	}
}
