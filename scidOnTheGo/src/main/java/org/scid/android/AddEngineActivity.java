package org.scid.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.scid.android.engine.Engine;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class AddEngineActivity extends AppCompatActivity {
	public static final String DATA_ENGINE_NAME = "org.scid.android.engine.name";
	public static final String DATA_ENGINE_EXECUTABLE = "org.scid.android.engine.executable";
	public static final String DATA_ENGINE_PACKAGE = "org.scid.android.engine.package";
	public static final String DATA_ENGINE_VERSION = "org.scid.android.engine.version";
	public static final String DATA_MAKE_CURRENT_ENGINE = "org.scid.android.make.current.engine";

	public static final int RESULT_EXECUTABLE_EXISTS = 2;

	private List<Engine> executablesList;
	private volatile String currentExecutable;
	private volatile String currentPackage;
	private volatile int currentVersion = 0;
	private List<ChessEngine> openEngines = new ArrayList<ChessEngine>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Set<String> ignoreExtensions = new HashSet<String>(
				Arrays.asList(new String[] { ".sg4", ".sn4", ".si4", ".pgn",
						".zip", ".xml" }));

		// Build set of engines. Add any additional engines from the external
		// directory.
		SortedSet<Engine> engines = Tools.findEnginesInDirectory(
				Tools.getScidDirectory(), ignoreExtensions);

		executablesList = new ArrayList<Engine>(engines);
		addOpenExchangeFormatEngines();

		setContentView(R.layout.add_engine);

		ListView executablesListView = (ListView) findViewById(R.id.engine_list);
		executablesListView
				.setOnItemClickListener(new ExecutableClickListener());
		executablesListView.setAdapter(new CheckableArrayAdapter<Engine>(this,
				android.R.layout.simple_list_item_single_choice,
				executablesList));
	}

	private void addOpenExchangeFormatEngines() {
		ChessEngineResolver resolver = new ChessEngineResolver(this);
		this.openEngines = resolver.resolveEngines();
		for (ChessEngine engine : openEngines) {
			if (!executablesList.contains(engine.getFileName())) {
				executablesList.add(new Engine(engine.getName(), engine
						.getFileName(), engine.getPackageName(), engine
						.getVersionCode()));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentExecutable != null) {
			outState.putString("engine.executable", currentExecutable);
		}
		if (currentPackage != null) {
			outState.putString("engine.package", currentPackage);
		}
		if (currentVersion > 0) {
			outState.putInt("engine.version", currentVersion);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		currentExecutable = savedInstanceState.getString("engine.executable");
		currentPackage = savedInstanceState.getString("engine.package");
		currentVersion = savedInstanceState.getInt("engine.version", 0);
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
			Engine item = (Engine) this.getItem(position);
			if (item != null) {
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);
				if (textView != null) {
					textView.setText(item.getName() != null ? item.getName()
							: item.getFileName());
					if (currentExecutable != null) {
						if (item.getFileName().equals(currentExecutable)) {
							((Checkable) view).setChecked(true);
						} else {
							((Checkable) view).setChecked(false);
						}
					}
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
				Engine engine = (Engine) parent.getItemAtPosition(position);
				currentExecutable = engine.getFileName();
				currentPackage = engine.getPackageName();
				currentVersion = engine.getVersionCode();
				EditText nameField = (EditText) findViewById(R.id.engine_name);
				Editable nameEditable = nameField.getText();
				if (nameEditable.length() == 0) {
					// If a name is not yet specified, set it to the selected
					// engine.
					String name = engine.getName() != null ? engine.getName()
							: currentExecutable;
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
		data.putExtra(DATA_ENGINE_PACKAGE, currentPackage);
		data.putExtra(DATA_ENGINE_VERSION, currentVersion);
		data.putExtra(DATA_MAKE_CURRENT_ENGINE, makeCurrentEngine);
		setResult(RESULT_EXECUTABLE_EXISTS, data);
		finish();
	}
}
