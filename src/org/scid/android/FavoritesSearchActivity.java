package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;

public class FavoritesSearchActivity extends Activity {
	private ProgressDialog progressDlg;

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
			String[] search = { };
			this.progressDlg = ProgressDialog.show(view.getContext(), "Search",
					"Searching...", true, false);
			new SearchTask().execute(this, fileName, search, progressDlg);
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}
}
