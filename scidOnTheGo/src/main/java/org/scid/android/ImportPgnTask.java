package org.scid.android;

import org.scid.database.DataBase;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class ImportPgnTask extends ProgressingTask<String> {
	private static final String LOG_TAG = "ImportPgnTask";
	private String pgnFileName;

	public ImportPgnTask(Activity activity, String pgnFileName) {
		super(activity, R.string.import_pgn_title, R.string.please_wait);
		this.pgnFileName = pgnFileName;
	}

	@Override
	protected String doInBackground(Void... params) {
		Log.d(LOG_TAG, "Starting import from " + pgnFileName);
		final String importErrors = DataBase.importPgn(pgnFileName, progress);
		Log.d(LOG_TAG, "Import from " + pgnFileName + " done.");
		Log.d(LOG_TAG, "Result: " + importErrors);
		activity.runOnUiThread(new Runnable() {
			public void run() {
				TextView resultView = (TextView) activity.findViewById(R.id.importResult);
				resultView.setText(importErrors);
			}
		});
		return importErrors;
	}

	@Override
	protected void onPostExecute(String importErrors) {
		dismissProgress();
		if (importErrors.length() == 0) {
			Toast.makeText(activity.getApplicationContext(),
					String.format(activity.getString(R.string.pgn_import_success),pgnFileName),
					Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_OK, (new Intent()).setAction(pgnFileName));
			activity.finish();
		} else {
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.pgn_import_failure),
					Toast.LENGTH_LONG).show();
		}
	}
}
