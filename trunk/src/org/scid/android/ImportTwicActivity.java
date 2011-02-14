package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

public class ImportTwicActivity extends Activity {
	private ProgressDialog progressDlg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		setContentView(tv);
		this.progressDlg = ProgressDialog.show(tv.getContext(),
				"Download current TWIC", "Downloading...", true, false);
		AsyncTask task = new ImportTwicTask().execute(this, progressDlg);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to distroy progress dialog in case user turns device
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}
}
