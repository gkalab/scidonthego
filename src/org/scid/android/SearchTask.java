package org.scid.android;

import org.scid.database.ScidCursor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public abstract class SearchTask extends AsyncTask<Void,Integer,Cursor> {
	private Activity activity;
	private ProgressDialog progressDialog;

	public SearchTask(Activity activity){
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(activity, "Search", "Searching...", true, false);
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
	}

	@Override
	protected void onPostExecute(Cursor cursor) {
		int filterSize = 0;
		if (cursor != null) {
			((ScidApplication) activity.getApplicationContext())
					.setGamesCursor(cursor);
			((ScidApplication) activity.getApplicationContext())
					.setNoGames(cursor);
			Bundle extras = cursor.getExtras();
			if (extras != Bundle.EMPTY) {
				filterSize = extras.getInt("filterSize");
				if (filterSize > 0) {
					activity.startManagingCursor(cursor);
					cursor.moveToFirst();
				}
			}
		}
		progressDialog.dismiss();
		if (filterSize == 0) {
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.filter_no_games),
					Toast.LENGTH_LONG).show();
			activity.setResult(Activity.RESULT_FIRST_USER); // reset the filter after returning
		} else {
			Toast.makeText(
					activity.getApplicationContext(),
					""
							+ filterSize
							+ " "
							+ activity
									.getString(R.string.filter_numberof_games),
					Toast.LENGTH_LONG).show();
			activity.setResult(Activity.RESULT_OK);
		}
		activity.finish();
	}
}
