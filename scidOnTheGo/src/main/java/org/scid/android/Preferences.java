package org.scid.android;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	public static final String DATA_ENGINE_NAME = "org.scid.android.engine.name";
	public static final String DATA_ENGINE_NAMES = "org.scid.android.engine.names";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		String [] engineNames = getIntent().getStringArrayExtra(DATA_ENGINE_NAMES);
		String engineName = getIntent().getStringExtra(DATA_ENGINE_NAME);
		ListPreference enginePreference = (ListPreference)findPreference("analysisEngine");
		enginePreference.setEntryValues(engineNames);
		enginePreference.setEntries(engineNames);
		enginePreference.setDefaultValue(engineName);
	}
}
