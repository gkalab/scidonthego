package org.scid.android;

import org.scid.database.DataBaseView;

import android.content.Intent;
import android.os.Bundle;

public class ExportPgnActivity extends SearchActivityBase {
	private String pgnFileName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);
		Tools.setKeepScreenOn(this, true);
		Intent i = getIntent();
		pgnFileName = i.getAction();
		final DataBaseView dbv = ((ScidApplication) this
				.getApplicationContext()).getDataBaseView();
		(new SimpleResultTask(this) {
			@Override
			protected Boolean doInBackground(Void... params) {
				return dbv.exportPgn(pgnFileName, progress);
			}
		}).execute();
	}
}
