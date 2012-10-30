package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import org.scid.database.DataBaseView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public abstract class SearchTask extends AsyncTask<Void,Integer,DataBaseView> {
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
	protected void onPostExecute(DataBaseView dbv) {
		int filterSize = 0;
		if (dbv != null) {
			((ScidApplication) activity.getApplicationContext())
					.setGamesDataBaseView(dbv);
			((ScidApplication) activity.getApplicationContext())
					.setNoGames(dbv);
			Bundle extras = dbv.getExtras();
			if (extras != Bundle.EMPTY) {
				filterSize = extras.getInt("filterSize");
				if (filterSize > 0) {
					dbv.moveToFirst();
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
