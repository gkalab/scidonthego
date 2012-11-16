package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.os.Bundle;
import android.widget.Toast;

public class FavoritesSearchActivity extends SearchActivityBase {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);

		final DataBaseView dbv = ((ScidApplication) this.getApplicationContext())
				.getDataBaseView();
		(new SearchTask(this){
			@Override
			protected GameFilter doInBackground(Void... params) {
				return dbv.getFavorites(progress);
			}
			@Override
			void onNothingFound(){
				Toast.makeText(activity.getApplicationContext(),
		                R.string.filter_no_favorites, Toast.LENGTH_LONG).show();
				activity.finish();
			}
		}).execute();
	}
}
