package org.scid.android;

import android.app.Activity;
import org.scid.database.DataBaseView;
import android.os.Bundle;
import android.view.View;

public class FavoritesSearchActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);
		searchFavorites(findViewById(R.id.favorites_view));
	}

	private void searchFavorites(View view) {
		final String fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		if (fileName.length() != 0) {
			(new SearchTask(this){
				@Override
				protected DataBaseView doInBackground(Void... params) {
					return DataBaseView.getFavorites(fileName);
				}
			}).execute();
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}
}
