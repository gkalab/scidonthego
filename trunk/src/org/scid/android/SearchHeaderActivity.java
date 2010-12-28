package org.scid.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class SearchHeaderActivity extends Activity {
	private int filterOperation = 0;
	private ProgressDialog progressDlg;

	private class OnFilterOperationSelectedListener implements
			OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			filterOperation = pos;
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// do nothing
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_header);
		addSpinner();
	}

	private void addSpinner() {
		Spinner spinner = (Spinner) findViewById(R.id.search_filter_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.search_filter_array,
				android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner
				.setOnItemSelectedListener(new OnFilterOperationSelectedListener());
	}

	public void onCancelClick(View view) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void onOkClick(View view) {
		final String fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		if (fileName.length() != 0) {
			EditText white = (EditText) findViewById(R.id.search_white);
			EditText black = (EditText) findViewById(R.id.search_black);
			CheckBox ignoreColors = (CheckBox) findViewById(R.id.ignore_colors);
			CheckBox resultWhiteWins = (CheckBox) findViewById(R.id.result_white_wins);
			CheckBox resultDraw = (CheckBox) findViewById(R.id.result_draw);
			CheckBox resultBlackWins = (CheckBox) findViewById(R.id.result_black_wins);
			CheckBox resultUnspecified = (CheckBox) findViewById(R.id.result_unspecified);
			EditText event = (EditText) findViewById(R.id.search_event);
			EditText site = (EditText) findViewById(R.id.search_site);
			EditText ecoFrom = (EditText) findViewById(R.id.search_eco_from);
			EditText ecoTo = (EditText) findViewById(R.id.search_eco_to);
			EditText yearFrom = (EditText) findViewById(R.id.search_year_from);
			EditText yearTo = (EditText) findViewById(R.id.search_year_to);
			CheckBox ecoNone = (CheckBox) findViewById(R.id.eco_none);
			String[] search = { "" + filterOperation,
					white.getText().toString().trim(),
					black.getText().toString().trim(),
					ignoreColors.isChecked() ? "true" : "false",
					resultWhiteWins.isChecked() ? "true" : "false",
					resultDraw.isChecked() ? "true" : "false",
					resultBlackWins.isChecked() ? "true" : "false",
					resultUnspecified.isChecked() ? "true" : "false",
					event.getText().toString().trim(),
					site.getText().toString().trim(),
					ecoFrom.getText().toString().trim(),
					ecoTo.getText().toString().trim(),
					ecoNone.isChecked() ? "true" : "false",
					yearFrom.getText().toString().trim(),
					yearTo.getText().toString().trim() };

			this.progressDlg = ProgressDialog.show(view.getContext(), "Search",
					"Searching...", true, false);
			AsyncTask task = new SearchTask().execute(this, fileName, search,
					progressDlg);
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to distroy progress dialog in case user turns device
		// TODO redisplay progress dialog on resume?!
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}
}
