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
		dismissProgress();
		int filterSize = (filter == null) ? 0 : filter.getSize();
		if (filterSize == 0) { // nothing was found
			Toast.makeText(activity.getApplicationContext(),
                           R.string.filter_no_games, Toast.LENGTH_LONG).show();
		} else { // something was found
			DataBaseView dbv = ((ScidApplication) activity.getApplicationContext())
					.getDataBaseView();
			String toast;
			if (filterSize > 1) { // several games were found
				toast = ""	+ filterSize + " "
						+ activity.getString(R.string.filter_number_of_games);
				boolean wasPreserved = dbv.setFilter(filter, true);
				activity.setResult(wasPreserved
						? SearchActivityBase.RESULT_SHOW_LIST_AND_KEEP_OLD_GAME
						: SearchActivityBase.RESULT_SHOW_LIST_AND_GOTO_NEW);
			} else { // only one game was found
				int toastId;
				int foundId = filter.getGameId(0);
				if (foundId == dbv.getId()) { // it is the one already shown
					toastId = R.string.filter_one_game_preserved_filter;
					// do not change the game view since it already shows the sole found game
					activity.setResult(SearchActivityBase.RESULT_CANCELED);
				} else { // the found game is not already shown
					int positionInOld = dbv.getPosition(foundId);
					if (positionInOld >= 0) { // but it *can* be shown in the previous filter
						dbv.moveToPosition(positionInOld);
						toastId = R.string.filter_one_game_preserved_filter;
						activity.setResult(SearchActivityBase.RESULT_SHOW_SINGLE_NEW);
					} else { // the found game is not in the previous filter
						dbv.setFilter(null, false); // thus we need to reset filter
						dbv.moveToPosition(foundId); // with null filter position is equal to id
						toastId = R.string.filter_one_game_reset_filter;
						activity.setResult(SearchActivityBase.RESULT_SHOW_SINGLE_NEW);
					}
				}
				toast = activity.getString(toastId);
			}
			Toast.makeText(activity.getApplicationContext(), toast, Toast.LENGTH_LONG).show();
            activity.finish();
		}
	}
}
