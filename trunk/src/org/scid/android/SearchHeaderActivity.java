package org.scid.android;

import android.app.Activity;
import org.scid.database.DataBaseView;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class SearchHeaderActivity extends Activity {
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
		setContentView(R.layout.search_header);
		addSpinner();
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

    private String ets(int id){ // id of EditText => String
        return ((EditText) findViewById(id)).getText().toString().trim();
    }
    private boolean cbb(int id){ // id of CheckBox => boolean
        return ((CheckBox) findViewById(id)).isChecked();
    }
	public void onOkClick(View view) {
		final DataBaseView dbv = ((ScidApplication) this.getApplicationContext())
				.getGamesDataBaseView();
		if (dbv != null) {
            final String white = ets(R.id.search_white), black = ets(R.id.search_black),
            		event = ets(R.id.search_event), site = ets(R.id.search_site),
            		ecoFrom = ets(R.id.search_eco_from), ecoTo = ets(R.id.search_eco_to),
            		yearFrom = ets(R.id.search_year_from), yearTo = ets(R.id.search_year_to);
            final boolean ignoreColors = cbb(R.id.ignore_colors),
            		resultWhiteWins = cbb(R.id.result_white_wins),
            		resultDraw = cbb(R.id.result_draw),
            		resultBlackWins = cbb(R.id.result_black_wins),
            		resultUnspecified = cbb(R.id.result_unspecified),
            		ecoNone = cbb(R.id.eco_none);
			(new SearchTask(this){
				@Override
				protected DataBaseView doInBackground(Void... params) {
					return DataBaseView.getMatchingHeaders(dbv, filterOperation,
							white, black, ignoreColors,
                    		resultWhiteWins, resultDraw,
                    		resultBlackWins, resultUnspecified,
                    		event, site,
                    		ecoFrom, ecoTo, ecoNone,
                    		yearFrom, yearTo);
				}
			}).execute();
		} else {
            // TODO: this (dbv == null) should be impossible
			setResult(RESULT_OK);
			finish();
		}
	}
}
