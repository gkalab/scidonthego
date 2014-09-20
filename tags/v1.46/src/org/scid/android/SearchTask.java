package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public abstract class SearchTask extends ProgressingTask<GameFilter> {
	public SearchTask(Activity activity){
		super(activity, R.string.search, R.string.please_wait);
	}
	/** called from onPostExecute if the result is empty */
	void onNothingFound(){
		Toast.makeText(activity.getApplicationContext(),
                R.string.filter_no_games, Toast.LENGTH_LONG).show();
	}
	@Override
	protected void onPostExecute(GameFilter filter) {
		dismissProgress();
		int filterSize = (filter == null) ? 0 : filter.getSize();
		if (filterSize == 0) { // nothing was found
			onNothingFound();
		} else { // something was found
			DataBaseView dbv = ((ScidApplication) activity.getApplicationContext())
					.getDataBaseView();
			String toast;
			if (filterSize > 1) { // several games were found
				toast = String.format(activity.getString(R.string.filter_number_of_games), filterSize);
				boolean wasPreserved = dbv.setFilter(filter, true);
				activity.setResult(wasPreserved
						? SearchActivityBase.RESULT_SHOW_LIST_AND_KEEP_OLD_GAME
						: SearchActivityBase.RESULT_SHOW_LIST_AND_GOTO_NEW);
			} else { // only one game was found
				int toastId;
				int foundId = filter.getGameId(0);
				int positionInOld = dbv.getPosition(foundId);
				if (positionInOld >= 0) { // the found game can be shown in the previous filter
					dbv.moveToPosition(positionInOld);
					toastId = R.string.filter_one_game_preserved_filter;
				} else { // the found game is not in the previous filter
					dbv.setFilter(null, false); // thus we need to reset filter
					dbv.moveToPosition(foundId); // with null filter position is equal to id
					toastId = R.string.filter_one_game_reset_filter;
				}
				activity.setResult(SearchActivityBase.RESULT_SHOW_SINGLE_NEW,
						(new Intent()).putExtra("foundPly", filter.getGamePly(0)));
				toast = activity.getString(toastId);
			}
			Toast.makeText(activity.getApplicationContext(), toast, Toast.LENGTH_LONG).show();
            activity.finish();
		}
	}
}
