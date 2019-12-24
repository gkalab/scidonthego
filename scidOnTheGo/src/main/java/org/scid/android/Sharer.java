package org.scid.android;

import android.app.Activity;
import android.content.Intent;

public class Sharer {

	private Activity context;
	private Intent intent = null;

	Sharer(Activity context) {
		this.context = context;
	}

	void createShareIntent(String type, String data) {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType(type);
		shareIntent.putExtra(Intent.EXTRA_TEXT, data);
		this.intent = Intent.createChooser(shareIntent, context
				.getResources().getText(R.string.menu_share_game));
	}

	public Intent getIntent() {
		return intent;
	}
}
