package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class SearchTask extends AsyncTask {
	private Activity activity;
	private ProgressDialog progressDlg;

	@Override
	protected Object doInBackground(Object... params) {
		Integer filterSize = 0;
		this.activity = (Activity) params[0];
		String fileName = (String) params[1];
		String[] search = (String[]) params[2];
		this.progressDlg = (ProgressDialog) params[3];
		Cursor cursor = activity.getContentResolver().query(
				Uri.parse("content://org.scid.database.scidprovider/games"),
				null, fileName, search, null);
		if (cursor != null) {
			((ScidApplication) activity.getApplicationContext())
					.setGamesCursor(cursor);
			((ScidApplication) activity.getApplicationContext())
					.setNoGames(cursor.getCount());
			Bundle extras = cursor.getExtras();
			if (extras != Bundle.EMPTY) {
				filterSize = extras.getInt("filterSize");
				if (filterSize > 0) {
					activity.startManagingCursor(cursor);
					cursor.moveToFirst();
				}
			}
		}
		return filterSize;
	}

	@Override
	protected void onPostExecute(Object result) {
		progressDlg.dismiss();
		Integer filterSize = (Integer) result;
		if (filterSize == 0) {
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.filter_no_games),
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(
					activity.getApplicationContext(),
					""
							+ filterSize
							+ " "
							+ activity
									.getString(R.string.filter_numberof_games),
					Toast.LENGTH_LONG).show();
		}
		activity.setResult(activity.RESULT_OK);
		activity.finish();
	}

}
