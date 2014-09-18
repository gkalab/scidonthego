package org.scid.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

public class AddEngineActivity extends Activity {
	public static final String DATA_ENGINE_MANAGER = "org.scid.android.engine.manager";
	public static final String DATA_ENGINE_NAME = "org.scid.android.engine.name";
	public static final String DATA_ENGINE_EXECUTABLE = "org.scid.android.engine.executable";
	public static final String DATA_MAKE_CURRENT_ENGINE = "org.scid.android.make.current.engine";

	public static final int RESULT_EXECUTABLE_EXISTS = 2;
	public static final int RESULT_EXECUTABLE_COPYING = 3;
	public static final int RESULT_EXECUTABLE_COPYFAILED = 3;

	private List<String> executablesList;
	private volatile String currentExecutable;
	private List<ChessEngine> openEngines = new ArrayList<ChessEngine>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Set<String> ignoreExtensions = new HashSet<String>(
				Arrays.asList(new String[] { ".sg4", ".sn4", ".si4", ".pgn",
						".zip", ".xml" }));

		// Build set of engines starting with engines already added.
		// Once per engine configuration preferences are possible,
		// multiple engine configurations using the same engine may be
		// useful.
		SortedSet<String> engines = Tools.findEnginesInDirectory(
				"/data/data/org.scid.android/", ignoreExtensions);
		// Add any additional engines from the external directory.
		engines.addAll(Tools.findEnginesInDirectory(Tools.getScidDirectory(),
				ignoreExtensions));

		executablesList = new ArrayList<String>(engines);
		addOpenExchangeFormatEngines();

		setContentView(R.layout.add_engine);

		ListView executablesListView = (ListView) findViewById(R.id.engine_list);
		executablesListView
				.setOnItemClickListener(new ExecutableClickListener());
		executablesListView.setAdapter(new CheckableArrayAdapter<String>(this,
				android.R.layout.simple_list_item_single_choice,
				executablesList));
	}

	private void addOpenExchangeFormatEngines() {
		ChessEngineResolver resolver = new ChessEngineResolver(this);
		this.openEngines = resolver.resolveEngines();
		for (ChessEngine engine : openEngines) {
			if (!executablesList.contains(engine.getFileName())) {
				executablesList.add(engine.getFileName());
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentExecutable != null) {
			outState.putString("engine.executable", currentExecutable);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		currentExecutable = savedInstanceState.getString("engine.executable");
	}

	/**
	 * An extension to ArrayAdapter to preserve checked state during orientation
	 * changes.
	 */
	private class CheckableArrayAdapter<T> extends ArrayAdapter<T> {

		public CheckableArrayAdapter(Context context, int textViewResourceId,
				List<T> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			if (currentExecutable != null) {
				String text = ((TextView) view).getText().toString();
				if (text.equals(currentExecutable)) {
					((Checkable) view).setChecked(true);
				} else {
					((Checkable) view).setChecked(false);
				}
			}
			return view;
		}
	}

	private class ExecutableClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position != ListView.INVALID_POSITION) {
				currentExecutable = (String) parent.getItemAtPosition(position);
				EditText nameField = (EditText) findViewById(R.id.engine_name);
				Editable nameEditable = nameField.getText();
				if (nameEditable.length() == 0) {
					// If a name is not yet specified, set it to the selected
					// engine.
					String name = currentExecutable;
					for (ChessEngine openEngine : openEngines) {
						if (openEngine.getFileName().equals(currentExecutable)) {
							name = openEngine.getName();
							break;
						}
					}
					nameField.setText(name);
				} else {
					String currentName = nameEditable.toString();
					// If the current name matches a different engine, update
					// the name.
					if (!currentExecutable.equals(currentName)
							&& executablesList.contains(currentName)) {
						nameField.setText(currentExecutable);
					}
				}
				// Ensure only the newly selected executable is checked.
				int selectedIndex = position - parent.getFirstVisiblePosition();
				for (int i = 0; i < parent.getChildCount(); i++) {
					View child = parent.getChildAt(i);
					if (i == selectedIndex) {
						((Checkable) child).setChecked(true);
					} else {
						((Checkable) child).setChecked(false);
					}
				}
			} else {
				currentExecutable = null;
				for (int i = 0; i < parent.getChildCount(); i++) {
					View child = parent.getChildAt(i);
					((Checkable) child).setChecked(false);
				}
			}
		}
	}

	public void onCancelClick(View view) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void onOkClick(View view) {
		Intent data = getIntent();

		EditText nameField = (EditText) findViewById(R.id.engine_name);
		String name = nameField.getText().toString();

		boolean makeCurrentEngine = ((Checkable) findViewById(R.id.make_current))
				.isChecked();

		data.putExtra(DATA_ENGINE_NAME, name);
		data.putExtra(DATA_ENGINE_EXECUTABLE, currentExecutable);
		data.putExtra(DATA_MAKE_CURRENT_ENGINE, makeCurrentEngine);
		setResult(RESULT_EXECUTABLE_EXISTS, data);
		finish();
	}
}
