package org.scid.android;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

public class RemoveEngineActivity extends ListActivity {
	public static final String DATA_ENGINE_NAME = "org.scid.android.engine.name";
	public static final String DATA_ENGINE_NAMES = "org.scid.android.engine.names";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String [] engineNames = getIntent().getStringArrayExtra(DATA_ENGINE_NAMES);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, engineNames));
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				String engineName = (String)parent.getItemAtPosition(position);
				Intent data = getIntent();
				data.putExtra(DATA_ENGINE_NAME, engineName);
				setResult(RESULT_OK, data);
				finish();
			}
		});
	}

	
}
