package org.scid.android;

import org.scid.database.DataBaseView;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class SearchHeaderActivity extends SearchActivityBase {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_header);
	    Tools.setKeepScreenOn(this, true);
		addSpinner();
	}
	
    private String ets(int id){ // id of EditText => String
        return ((EditText) findViewById(id)).getText().toString().trim();
    }
    private boolean cbb(int id){ // id of CheckBox => boolean
        return ((CheckBox) findViewById(id)).isChecked();
    }
	public void onOkClick(View view) {
		final DataBaseView dbv = ((ScidApplication) this.getApplicationContext())
				.getDataBaseView();
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
	}
}
