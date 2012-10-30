package org.scid.android;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
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
				protected Cursor doInBackground(Void... params) {
					String[] search = { };
					return FavoritesSearchActivity.this.getContentResolver().query(
							Uri.parse("content://org.scid.database.scidprovider/games"),
							null, fileName, search, null);
				}
			}).execute();
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}
}
