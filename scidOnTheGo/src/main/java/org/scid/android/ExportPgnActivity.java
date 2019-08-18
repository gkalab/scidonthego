package org.scid.android;

import org.scid.database.DataBaseView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ExportPgnActivity extends AppCompatActivity {
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
		(new SimpleResultTask(this, R.string.export_pgn_title) {
			@Override
			protected Boolean doInBackground(Void... params) {
				if (dbv != null) {
					return dbv.exportPgn(pgnFileName, progress);
				} else {
					return false;
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
