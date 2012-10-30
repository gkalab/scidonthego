package org.scid.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class SearchCurrentBoardActivity extends Activity {
	private String fen;
	private int filterOperation = 0;

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
		setContentView(R.layout.search_board);
		addSpinner();
		Intent i = getIntent();
		this.fen = i.getAction();
        Tools.setKeepScreenOn(this, true);
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
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.search_board);
		int checked = radioGroup.getCheckedRadioButtonId();
		switch (checked) {
		case R.id.search_current_board:
			currentBoardSearch(view, 0);
			break;
		case R.id.search_pawns:
			currentBoardSearch(view, 1);
			break;
		case R.id.search_files:
			currentBoardSearch(view, 2);
			break;
		case R.id.search_any:
			currentBoardSearch(view, 3);
			break;
		}
	}

	public void currentBoardSearch(View view, int searchType) {
		final String fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		if (fileName.length() != 0) {
			final String[] search = { "" + filterOperation, this.fen, "" + searchType };
			(new SearchTask(this){
				@Override
				protected Cursor doInBackground(Void... params) {
					return SearchCurrentBoardActivity.this.getContentResolver().query(
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
