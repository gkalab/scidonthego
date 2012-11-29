package org.scid.android;

import java.util.TreeMap;

import org.scid.database.DataBase;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SaveGameActivity extends Activity {
	private int resultSelected = 0;
	private ProgressDialog progressDlg;
	private EditText event;
	private EditText site;
	private EditText date;
	private EditText round;
	private EditText white;
	private EditText black;

	private class OnResultSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			resultSelected = pos;
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// do nothing
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save_game);
		final TreeMap<String, String> headers = new TreeMap<String, String>();
		getScidAppContext().getController().getHeaders(headers);
		// disable save game if the database is empty or if it's a new game (gameNo=-1)
		Button save_game_button = (Button) findViewById(R.id.save_game_save);
		if (getScidAppContext().getNoGames() == 0
				|| getScidAppContext().getGameId() < 0) {
			save_game_button.setEnabled(false);
		}
		event = (EditText) findViewById(R.id.ed_header_event);
		site = (EditText) findViewById(R.id.ed_header_site);
		date = (EditText) findViewById(R.id.ed_header_date);
		round = (EditText) findViewById(R.id.ed_header_round);
		white = (EditText) findViewById(R.id.ed_header_white);
		black = (EditText) findViewById(R.id.ed_header_black);

		event.setText(headers.get("Event"));
		site.setText(headers.get("Site"));
		date.setText(headers.get("Date"));
		round.setText(headers.get("Round"));
		white.setText(headers.get("White"));
		black.setText(headers.get("Black"));
		addSpinner(headers.get("Result"));
	}

	private void addSpinner(String resultString) {
		Spinner spinner = (Spinner) findViewById(R.id.ed_result_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.result_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getPosition(resultString));
		spinner.setOnItemSelectedListener(new OnResultSelectedListener());
	}

	private ScidApplication getScidAppContext() {
		return ((ScidApplication) this.getApplicationContext());
	}

	public void onCancelClick(View view) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void onSaveClick(View view) {
		final String fileName = getScidAppContext().getCurrentFileName();
		if (fileName.length() != 0) {
			saveGame(view, getScidAppContext().getGameId());
			finish();
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	public void onSaveAsNewClick(View view) {
		final String fileName = getScidAppContext().getCurrentFileName();
		if (fileName.length() != 0) {
			saveGame(view, -1);
			finish();
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	private void saveGame(View view, Integer gameNo) {
		this.progressDlg = ProgressDialog.show(view.getContext(),
				getString(R.string.saving), getString(R.string.please_wait),
				true, false);

		final TreeMap<String, String> headers = new TreeMap<String, String>();
		headers.put("Event", event.getText().toString().trim());
		headers.put("Site", site.getText().toString().trim());
		headers.put("Date", date.getText().toString().trim());
		headers.put("Round", round.getText().toString().trim());
		headers.put("White", white.getText().toString().trim());
		headers.put("Black", black.getText().toString().trim());
		CharSequence[] resultArray = getResources().getTextArray(
				R.array.result_array);
		headers.put("Result", ((String) resultArray[resultSelected]).trim());

		getScidAppContext().getController().setHeaders(headers);

		String pgn = getScidAppContext().getController().getPGN();
		final String result = DataBase.saveGame(gameNo, pgn);
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();
		}
		if (result.length() > 0) {
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
					.show();
		} else {
			setResult(RESULT_OK, (new Intent()).setAction(gameNo.toString()));
			Toast.makeText(getApplicationContext(), R.string.game_saved,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		// TODO redisplay progress dialog on resume?!
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();
		}
	}
}
