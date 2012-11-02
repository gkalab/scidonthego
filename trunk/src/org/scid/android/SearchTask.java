package org.scid.android;

import org.scid.database.DataBaseView;

import android.app.Activity;
import android.widget.Toast;

public abstract class SearchTask extends ProgressingTask<DataBaseView> {
	public SearchTask(Activity activity){
		super(activity, R.string.search, R.string.please_wait);
	}

	@Override
	protected void onPostExecute(DataBaseView dbv) {
		if (dbv != null) {
			((ScidApplication) activity.getApplicationContext())
			.setDataBaseView(dbv);
			int filterSize = dbv.getCount();
			if (filterSize > 0) {
				dbv.moveToFirst();
				Toast.makeText(activity.getApplicationContext(),
						""	+ filterSize + " " + activity.getString(R.string.filter_numberof_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_OK);
			} else {
				Toast.makeText(activity.getApplicationContext(),
						activity.getString(R.string.filter_no_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_FIRST_USER); // reset the filter after return
			}
		}
		dismissProgress();
		activity.finish();
	}
}
