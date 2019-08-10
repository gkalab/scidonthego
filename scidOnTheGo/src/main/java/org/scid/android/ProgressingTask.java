package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * ProgressingTask&lt;Result&gt; is an asynchronous task with ProgressDialog.
 * Canceling the progress, cancels the activity. Inside
 * <code>Result doInBackground(Void... params)</code> you should use
 * <code>progress</code> to update the ProgressDialog and check whether the task
 * is already canceled. Remember to call <code>dismissProgress</code> from
 * <code>onPostExecute(Result)</code>
 */
public abstract class ProgressingTask<Result> extends
		AsyncTask<Void, Integer, Result> {

	protected Activity activity;
	private int titleId, messageId; // for ProgressDialog
	private ProgressDialog progressDialog;
	protected Progress progress;

	public ProgressingTask(Activity activity, int titleId, int messageId) {
		this.activity = activity;
		this.titleId = titleId;
		this.messageId = messageId;
		progressDialog = new ProgressDialog(activity);
	}

	@Override
	protected void onPreExecute() {
		progressDialog.setTitle(titleId);
		progressDialog.setMessage(activity.getString(messageId));
		progressDialog.setIndeterminate(false);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				ProgressingTask.this.cancel(false);
			}
		});
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgress(0);
		progressDialog.setMax(100);
		// progressDialog.setProgressNumberFormat(null); // requires API level 11 (Android 3.0.x)
		progressDialog.show();

		progress = new Progress() {
			@Override
			public boolean isCancelled() {
				return ProgressingTask.this.isCancelled();
			}
			@Override
			public void publishProgress(int value) {
				ProgressingTask.this.publishProgress(value);
			}
		};
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		progressDialog.setProgress(values[0]);
	}

	/**
	 * Dismisses the ProgressDialog. Must be called from <code>onPostExecute(Result)</code>.
	 */
	protected void dismissProgress() {
		progressDialog.dismiss();
	}

	@Override
	protected void onCancelled() {
		Toast.makeText(activity, activity.getText(R.string.canceled), Toast.LENGTH_SHORT).show();
		progressDialog.dismiss();
		activity.setResult(Activity.RESULT_CANCELED);
		activity.finish();
	}
}
