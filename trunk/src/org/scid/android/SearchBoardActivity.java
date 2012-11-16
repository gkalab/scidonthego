package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public class SearchBoardActivity extends SearchActivityBase {
	private String fen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_board);
	    Tools.setKeepScreenOn(this, true);
		addSpinner();
		Intent i = getIntent();
		fen = i.getAction();
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

	public void currentBoardSearch(View view, final int searchType) {
		final DataBaseView dbv = ((ScidApplication) this.getApplicationContext())
				.getDataBaseView();
		(new SearchTask(this){
			@Override
			protected GameFilter doInBackground(Void... params) {
				return dbv.getMatchingBoards(filterOperation, fen, searchType, progress);
			}
		}).execute();
	}
}
