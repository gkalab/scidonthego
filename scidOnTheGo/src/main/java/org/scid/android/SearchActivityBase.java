package org.scid.android;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

class SearchActivityBase extends AppCompatActivity {
	public static final int RESULT_SHOW_LIST_AND_KEEP_OLD_GAME = RESULT_FIRST_USER;
	public static final int RESULT_SHOW_LIST_AND_GOTO_NEW = RESULT_FIRST_USER + 1;
	public static final int RESULT_SHOW_SINGLE_NEW = RESULT_FIRST_USER + 2;

	protected int filterOperation = 0;
	protected void addSpinner() {
		Spinner spinner = findViewById(R.id.search_filter_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.search_filter_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int pos,	long id) {
				filterOperation = pos;
			}
			public void onNothingSelected(AdapterView<?> parent) {
				// do nothing
			}
		});
	}

	public void onCancelClick(View view) {
		setResult(RESULT_CANCELED);
		finish();
	}
}