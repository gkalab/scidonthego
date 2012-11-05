package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.os.Bundle;

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
		}).execute();
	}
}
