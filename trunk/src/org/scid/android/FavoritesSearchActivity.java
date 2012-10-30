package org.scid.android;

import org.scid.database.DataBaseView;

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
			protected DataBaseView doInBackground(Void... params) {
				return DataBaseView.getFavorites(dbv);
			}
		}).execute();
	}
}
