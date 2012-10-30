package org.scid.android;

import org.scid.database.DataBaseView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

public abstract class SearchTask extends AsyncTask<Void,Integer,DataBaseView> {
	private Activity activity;
	private ProgressDialog progressDialog;
	protected Progress progress; // to be used by doInBackground

	public SearchTask(Activity activity){
		this.activity = activity;
		progressDialog = new ProgressDialog(activity);
	}

	@Override
	protected void onPreExecute() {
		progressDialog.setTitle("Search");
		progressDialog.setMessage("Searching...");
		progressDialog.setIndeterminate(false);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						SearchTask.this.cancel(false);
					}
				});
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgress(0);
		progressDialog.setMax(100);
		// progressDialog.setProgressNumberFormat(null); // requires API level 11 (Android 3.0.x)
		progressDialog.show();

		progress = new Progress(){
			@Override
			public boolean isCancelled() {
				return SearchTask.this.isCancelled();
			}
			@Override
			public void publishProgress(int value) {
				SearchTask.this.publishProgress(value);
			}
		};
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		progressDialog.setProgress(values[0]);
	}

	@Override
	protected void onCancelled() {
		progressDialog.dismiss();
		activity.setResult(Activity.RESULT_CANCELED);
		activity.finish();
	}

	@Override
	protected void onPostExecute(DataBaseView dbv) {
		int filterSize = 0;
		if (dbv != null) {
			((ScidApplication) activity.getApplicationContext())
			.setDataBaseView(dbv);
			filterSize = dbv.getCount();
			if (filterSize > 0) {
				dbv.moveToFirst();
			}
			if (filterSize == 0) {
				Toast.makeText(activity.getApplicationContext(),
						activity.getString(R.string.filter_no_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_FIRST_USER); // reset the filter after returning
			} else {
				Toast.makeText(activity.getApplicationContext(),
						""	+ filterSize + " " + activity.getString(R.string.filter_numberof_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_OK);
			}
		}
		progressDialog.dismiss();
		activity.finish();
	}
}
