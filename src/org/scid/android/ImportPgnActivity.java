package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ImportPgnActivity extends Activity {
	private ProgressDialog progressDlg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pgnimport);
		Intent i = getIntent();
		String pgnFileName = i.getAction();
		if (pgnFileName.length() != 0) {
			TextView view = (TextView) findViewById(R.id.importResult);
			this.progressDlg = ProgressDialog.show(view.getContext(), "Import",
					"Importing...", true, false);
			new ImportPgnTask().execute(this, pgnFileName,
					progressDlg);
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}

	public void onOkClick(View view) {
		setResult(RESULT_OK);
		finish();
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
