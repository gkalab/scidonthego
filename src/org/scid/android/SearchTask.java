package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.app.Activity;
import android.widget.Toast;

public abstract class SearchTask extends ProgressingTask<GameFilter> {
	public SearchTask(Activity activity){
		super(activity, R.string.search, R.string.please_wait);
	}

	@Override
	protected void onPostExecute(GameFilter filter) {
		if (filter != null) {
			DataBaseView dbv = ((ScidApplication) activity.getApplicationContext())
					.getDataBaseView();
			int filterSize = filter.getSize();
			if (filterSize > 0) {
				dbv.setFilter(filter);
				dbv.moveToFirst(); // TODO: move to previous game
				Toast.makeText(activity.getApplicationContext(),
						""	+ filterSize + " " + activity.getString(R.string.filter_numberof_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_OK); // TODO: show new filter after return
			} else {
				Toast.makeText(activity.getApplicationContext(),
						activity.getString(R.string.filter_no_games),
						Toast.LENGTH_LONG).show();
				activity.setResult(Activity.RESULT_FIRST_USER); // TODO: do nothing after return
			}
		}
		dismissProgress();
		activity.finish();
	}
}
